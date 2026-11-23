package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.registry.ADDataSerializers;
import mod.azure.atomicdawn.story.StoryService;
import mod.azure.atomicdawn.weapon.instance.Weapons;

@SuppressWarnings("unused")
public class NpcEntity extends PathfinderMob {

    public enum CompanionMode {
        FOLLOW,
        WAIT
    }

    private static final EntityDataAccessor<CompoundTag> DATA_APPEARANCE = SynchedEntityData.defineId(
        NpcEntity.class,
        ADDataSerializers.COMPOUND_TAG
    );

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(
        NpcEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_POSE = SynchedEntityData.defineId(
        NpcEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<String> DATA_TITLE = SynchedEntityData.defineId(
        NpcEntity.class,
        EntityDataSerializers.STRING
    );

    private static final EntityDataAccessor<String> DATA_EMOTE = SynchedEntityData.defineId(
        NpcEntity.class,
        EntityDataSerializers.STRING
    );

    private static final EntityDataAccessor<Integer> DATA_EMOTE_ID = SynchedEntityData.defineId(
        NpcEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Boolean> DATA_DOWNED = SynchedEntityData.defineId(
        NpcEntity.class,
        EntityDataSerializers.BOOLEAN
    );

    private static final EquipmentSlot[] EQUIPMENT = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET };

    private static final long TALK_TIMEOUT_TICKS = 20L * 120L;

    private static final double TALK_RANGE_SQR = 10.0 * 10.0;

    private static final double FAR_FROM_HOME_SQR = 64.0 * 64.0;

    private NpcConfig config = NpcConfig.DEFAULT;

    private NpcAppearance appearance = NpcAppearance.DEFAULT;

    @Nullable
    private BlockPos home;

    private float homeYaw;

    @Nullable
    private UUID talkingTo;

    private long talkStarted;

    @Nullable
    private UUID owner;

    private long hireExpires = -1L;

    private CompanionMode companionMode = CompanionMode.FOLLOW;

    private int respawnTicks;

    private int[] shopUses = new int[0];

    private long lastRestock;

    @Nullable
    private NpcMerchant merchant;

    private int clientState = -1;

    private int clientEmote;

    @Nullable
    private NpcAppearance clientAppearance;

    private int clientRedispatchAt = -1;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (!level.isClientSide()) {
            rebuildGoals();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_APPEARANCE, new CompoundTag());
        builder.define(DATA_STATE, NpcAnimationState.IDLE.ordinal());
        builder.define(DATA_POSE, NpcBehavior.NpcPose.STANDING.ordinal());
        builder.define(DATA_TITLE, "");
        builder.define(DATA_EMOTE, "");
        builder.define(DATA_EMOTE_ID, 0);
        builder.define(DATA_DOWNED, false);
    }

    private void rebuildGoals() {
        goalSelector.removeAllGoals(_ -> true);
        targetSelector.removeAllGoals(_ -> true);
        var behavior = config().behavior();
        var hostility = config().combat().hostility();
        var wander = behavior.movement() == NpcBehavior.Movement.WANDER;
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new NpcGoals.TalkGoal(this));
        if (hostility.fights()) {
            goalSelector.addGoal(2, new NpcCombatGoals.RangedGoal(this));
            goalSelector.addGoal(2, new NpcCombatGoals.MeleeGoal(this));
        } else if (!config().stats().invulnerable()) {
            goalSelector.addGoal(2, new PanicGoal(this, 1.3));
        }
        goalSelector.addGoal(3, new NpcCombatGoals.FollowOwnerGoal(this));
        goalSelector.addGoal(4, new NpcGoals.ReturnHomeGoal(this));
        if (wander) {
            goalSelector.addGoal(5, new NpcGoals.WanderGoal(this));
        }
        if (behavior.lookAtPlayers()) {
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
        }
        if (wander) {
            goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        }

        if (!hostility.fights()) {
            return;
        }
        targetSelector.addGoal(1, new NpcCombatGoals.DefendOwnerGoal(this));
        targetSelector.addGoal(2, new NpcCombatGoals.AssistOwnerGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        if (hostility != NpcCombat.Hostility.HOSTILE) {
            targetSelector.addGoal(
                4,
                new NearestAttackableTargetGoal<>(
                    this,
                    Player.class,
                    10,
                    true,
                    false,
                    (target, _) -> target instanceof Player player && StoryService.hostile(this, player)
                )
            );
        }
        if (hostility == NpcCombat.Hostility.GUARD) {
            targetSelector.addGoal(
                4,
                new NearestAttackableTargetGoal<>(
                    this,
                    Mob.class,
                    10,
                    true,
                    false,
                    (target, _) -> isEnemyOfGuards(target)
                )
            );
        } else if (hostility == NpcCombat.Hostility.HOSTILE) {
            targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
            targetSelector.addGoal(
                5,
                new NearestAttackableTargetGoal<>(
                    this,
                    NpcEntity.class,
                    10,
                    true,
                    false,
                    (target, _) -> target instanceof NpcEntity npc && !npc.isHostile() && npc.canFight()
                )
            );
        }
    }

    private boolean isEnemyOfGuards(LivingEntity target) {
        if (target instanceof NpcEntity npc) {
            return npc.isHostile() && !npc.isDowned();
        }
        return target instanceof Enemy;
    }

    public NpcConfig config() {
        return config == null ? NpcConfig.DEFAULT : config;
    }

    public NpcAppearance appearance() {
        return appearance == null ? NpcAppearance.DEFAULT : appearance;
    }

    public String title() {
        return entityData.get(DATA_TITLE);
    }

    public NpcAnimationState animationState() {
        return NpcAnimationState.byIndex(entityData.get(DATA_STATE));
    }

    public NpcBehavior.NpcPose npcPose() {
        return NpcBehavior.NpcPose.byIndex(entityData.get(DATA_POSE));
    }

    public String emote() {
        return entityData.get(DATA_EMOTE);
    }

    public int emoteId() {
        return entityData.get(DATA_EMOTE_ID);
    }

    public boolean isDowned() {
        return entityData.get(DATA_DOWNED);
    }

    public int respawnTicks() {
        return respawnTicks;
    }

    public void respawnNow() {
        if (respawnTicks > 0) {
            respawnTicks = 0;
            finishRespawn();
        }
    }

    public boolean isHostile() {
        return config().combat().hostility() == NpcCombat.Hostility.HOSTILE;
    }

    public boolean canFight() {
        return config().combat().hostility().fights() && !isDowned() && isAlive();
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public float homeYaw() {
        return homeYaw;
    }

    public void setHome(BlockPos pos, float yaw) {
        home = pos.immutable();
        homeYaw = yaw;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public boolean isOwnedBy(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    @Nullable
    public Player ownerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    public CompanionMode companionMode() {
        return companionMode;
    }

    public void setCompanionMode(CompanionMode mode) {
        companionMode = mode;
        if (mode == CompanionMode.WAIT) {
            getNavigation().stop();
        }
    }

    public void setOwner(@Nullable UUID player, long expiresAt) {
        owner = player;
        hireExpires = player == null ? -1L : expiresAt;
        companionMode = CompanionMode.FOLLOW;
        setTarget(null);
        if (player == null && home != null && home.distToCenterSqr(position()) > FAR_FROM_HOME_SQR) {
            getNavigation().stop();
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        }
    }

    public long hireExpires() {
        return hireExpires;
    }

    public boolean isAlly(Entity entity) {
        if (entity == this) {
            return true;
        }
        if (owner != null) {
            if (owner.equals(entity.getUUID())) {
                return true;
            }
            if (entity instanceof NpcEntity npc && owner.equals(npc.owner)) {
                return true;
            }
        }
        if (entity instanceof NpcEntity npc) {
            return npc.isHostile() == isHostile();
        }
        return false;
    }

    public boolean canShoot(Entity entity, LivingEntity target) {
        if (entity == target) {
            return true;
        }
        if (isAlly(entity)) {
            return false;
        }
        return !(entity instanceof Player player) || isHostile() || StoryService.hostile(this, player);
    }

    public void applyConfig(NpcConfig value) {
        config = value.sanitized();
        var current = config();
        setCustomName(current.name().isEmpty() ? null : Component.literal(current.name()));
        setCustomNameVisible(current.showName() && !current.name().isEmpty());
        entityData.set(DATA_TITLE, current.title());
        entityData.set(DATA_POSE, current.behavior().pose().ordinal());
        entityData.set(DATA_APPEARANCE, current.appearance().toTag());
        setBase(Attributes.MAX_HEALTH, current.stats().maxHealth());
        setBase(Attributes.MOVEMENT_SPEED, current.behavior().speed());
        setBase(Attributes.ATTACK_DAMAGE, current.combat().meleeDamage());
        setBase(Attributes.ARMOR, current.combat().armor());
        setBase(Attributes.FOLLOW_RANGE, Math.max(current.combat().aggroRange(), current.combat().rangedRange()) + 8.0);
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
        var trades = current.shop().trades().size();
        if (shopUses.length != trades) {
            shopUses = Arrays.copyOf(shopUses, trades);
        }
        merchant = null;
        if (!current.combat().hostility().fights()) {
            setTarget(null);
        }
        if (!level().isClientSide()) {
            rebuildGoals();
        }
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        var instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_APPEARANCE.equals(accessor)) {
            appearance = NpcAppearance.fromTag(entityData.get(DATA_APPEARANCE));
            refreshDimensions();
        }
    }

    @Override
    public @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        var level = level();
        return NpcModels.dimensions(level.registryAccess(), appearance());
    }

    @Nullable
    public Player talkingPlayer() {
        if (talkingTo == null) {
            return null;
        }
        return level().getPlayerByUUID(talkingTo);
    }

    public void startTalking(ServerPlayer player) {
        talkingTo = player.getUUID();
        talkStarted = level().getGameTime();
    }

    public void stopTalking(UUID player) {
        if (player.equals(talkingTo)) {
            talkingTo = null;
        }
    }

    public void emote(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (appearance().kind() == NpcAppearance.Kind.ENTITY && ("swing".equals(name) || "attack".equals(name))) {
            swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        }
        animate(name);
    }

    public void animate(String name) {
        entityData.set(DATA_EMOTE, name);
        entityData.set(DATA_EMOTE_ID, entityData.get(DATA_EMOTE_ID) + 1);
    }

    public void copyEquipmentFrom(Player player) {
        for (var slot : EQUIPMENT) {
            setItemSlot(slot, player.getItemBySlot(slot).copy());
        }
    }

    public void clearEquipment() {
        for (var slot : EQUIPMENT) {
            setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    public NpcMerchant merchant() {
        if (merchant == null) {
            merchant = new NpcMerchant(this);
        }
        return merchant;
    }

    public int shopUses(int index) {
        restockIfDue();
        return index >= 0 && index < shopUses.length ? shopUses[index] : 0;
    }

    public void recordShopUse(int index, int uses) {
        if (index >= 0 && index < shopUses.length) {
            shopUses[index] = uses;
        }
    }

    public void resetShopUses() {
        shopUses = new int[config().shop().trades().size()];
        lastRestock = level().getGameTime();
        merchant = null;
    }

    private void restockIfDue() {
        var minutes = config().shop().restockMinutes();
        if (minutes > 0 && level().getGameTime() - lastRestock >= minutes * 1200L) {
            Arrays.fill(shopUses, 0);
            lastRestock = level().getGameTime();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientAnimations();
            return;
        }
        if (home == null) {
            setHome(blockPosition(), getYRot());
        }
        if (respawnTicks > 0) {
            if (--respawnTicks == 0) {
                finishRespawn();
            }
            return;
        }
        tickCompanion();
        tickTalking();
        tickState();
        tickFacing();
    }

    private void tickCompanion() {
        if (owner == null || hireExpires < 0L || level().getGameTime() < hireExpires) {
            return;
        }
        if (ownerPlayer() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("npc.atomicdawn.companion.expired", getDisplayName()));
        }
        setOwner(null, -1L);
    }

    private void tickTalking() {
        if (talkingTo == null) {
            return;
        }
        var player = talkingPlayer();
        if (
            player == null || !player.isAlive() || player.distanceToSqr(this) > TALK_RANGE_SQR
                || level().getGameTime() - talkStarted > TALK_TIMEOUT_TICKS || getTarget() != null
        ) {
            if (player instanceof ServerPlayer serverPlayer) {
                NpcDialogueService.close(serverPlayer);
            }
            talkingTo = null;
        }
    }

    private void tickState() {
        var dx = getX() - xo;
        var dz = getZ() - zo;
        var speed = Math.sqrt(dx * dx + dz * dz);
        NpcAnimationState state;
        if (isDeadOrDying()) {
            state = NpcAnimationState.DEATH;
        } else if (speed > 0.18) {
            state = NpcAnimationState.RUN;
        } else if (speed > 0.01) {
            state = NpcAnimationState.WALK;
        } else if (getTarget() != null) {
            state = NpcAnimationState.COMBAT;
        } else if (talkingTo != null) {
            state = NpcAnimationState.TALK;
        } else if (npcPose() == NpcBehavior.NpcPose.SITTING) {
            state = NpcAnimationState.SIT;
        } else if (npcPose() == NpcBehavior.NpcPose.CROUCHING) {
            state = NpcAnimationState.CROUCH;
        } else {
            state = NpcAnimationState.IDLE;
        }
        if (state.ordinal() != entityData.get(DATA_STATE)) {
            entityData.set(DATA_STATE, state.ordinal());
        }
    }

    private void tickFacing() {
        if (
            owner != null || config().behavior().movement() != NpcBehavior.Movement.STATIONARY || talkingTo != null
                || getTarget() != null || !getNavigation().isDone() || home == null
                || home.distToCenterSqr(position()) > 2.25
        ) {
            return;
        }
        setYRot(homeYaw);
        yBodyRot = homeYaw;
        if (!config().behavior().lookAtPlayers()) {
            setYHeadRot(homeYaw);
        }
    }

    private void clientAnimations() {
        var current = appearance();
        if (current.kind() != NpcAppearance.Kind.AZURE) {
            clientAppearance = current;
            return;
        }
        var definition = NpcModels.get(level().registryAccess(), current.model()).orElse(null);
        if (definition == null) {
            return;
        }
        int state = entityData.get(DATA_STATE);
        if (state != clientState || !current.equals(clientAppearance)) {
            clientState = state;
            clientAppearance = current;
            clientRedispatchAt = tickCount + 10;
            NpcAnimations.loop(this, definition.animation(NpcAnimationState.byIndex(state)));
        } else if (tickCount == clientRedispatchAt) {
            NpcAnimations.loop(this, definition.animation(NpcAnimationState.byIndex(state)));
        }
        var emoteId = emoteId();
        if (emoteId != clientEmote) {
            clientEmote = emoteId;
            NpcAnimations.play(this, definition.emote(emote()));
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull ServerLevel level, @NotNull Entity target) {
        var definition = Weapons.definition(level, getMainHandItem());
        boolean hit;
        if (definition != null && definition.isMelee()) {
            var amount = definition.damage().base() * config().combat().damageMultiplier();
            hit = target.hurtServer(level, ADDamageTypes.source(level, definition.damage().type(), this), amount);
        } else {
            hit = super.doHurtTarget(level, target);
        }
        if (hit) {
            animate("attack");
        }
        return hit;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && (isAlly(target) || !config().combat().hostility().fights())) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof NpcWandItem) {
            return InteractionResult.PASS;
        }
        if (hand != InteractionHand.MAIN_HAND || isDeadOrDying() || isDowned() || isHostile()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NpcDialogueService.open(serverPlayer, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        var bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypass) {
            if (isDowned()) {
                return false;
            }
            var attacker = source.getEntity();
            if (attacker != null && isAlly(attacker)) {
                return false;
            }
            if (config().stats().invulnerable()) {
                return false;
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (
            !level().isClientSide() && config().combat().respawn() && !source.is(
                DamageTypeTags.BYPASSES_INVULNERABILITY
            )
        ) {
            startRespawn();
            return;
        }
        super.die(source);
    }

    private void startRespawn() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(
                ParticleTypes.POOF,
                getX(),
                getY() + getBbHeight() / 2,
                getZ(),
                20,
                0.3,
                0.5,
                0.3,
                0.02
            );
        }
        if (talkingPlayer() instanceof ServerPlayer player) {
            NpcDialogueService.close(player);
        }
        setHealth(getMaxHealth());
        clearFire();
        setTarget(null);
        setLastHurtByMob(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        respawnTicks = config().combat().respawnSeconds() * 20;
        entityData.set(DATA_DOWNED, true);
        setInvisible(true);
        setNoAi(true);
        if (home != null) {
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        }
    }

    private void finishRespawn() {
        entityData.set(DATA_DOWNED, false);
        setInvisible(false);
        setNoAi(false);
        setHealth(getMaxHealth());
        playSound(SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.2F);
    }

    @Override
    public boolean isPickable() {
        return !isDowned() && super.isPickable();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !isDowned() && super.canBeSeenAsEnemy();
    }

    @Override
    public boolean shouldShowName() {
        return !isDowned() && super.shouldShowName();
    }

    @Override
    public boolean isPushable() {
        return !isDowned() && !config().behavior().immovable() && super.isPushable();
    }

    @Override
    public boolean isPushedByFluid(@NonNull FluidType type) {
        return !config().behavior().immovable() && super.isPushedByFluid(type);
    }

    @Override
    public void knockback(double power, double xd, double zd, @NonNull DamageSource source, float damage) {
        if (!config().behavior().immovable()) {
            super.knockback(power, xd, zd, source, damage);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void dropEquipment(@NotNull ServerLevel level) {}

    @Override
    protected void dropCustomDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource source, boolean recentlyHit) {}

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("NpcConfig", NpcConfig.CODEC, config());
        if (home != null) {
            output.store("NpcHome", BlockPos.CODEC, home);
        }
        output.putFloat("NpcHomeYaw", homeYaw);
        if (owner != null) {
            output.store("NpcOwner", UUIDUtil.CODEC, owner);
            output.putLong("NpcHireExpires", hireExpires);
            output.putString("NpcCompanionMode", companionMode.name());
        }
        output.store("NpcShopUses", Codec.INT.listOf(), Arrays.stream(shopUses).boxed().toList());
        output.putLong("NpcLastRestock", lastRestock);
        output.putInt("NpcRespawnTicks", respawnTicks);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        applyConfig(input.read("NpcConfig", NpcConfig.CODEC).orElse(NpcConfig.DEFAULT));
        home = input.read("NpcHome", BlockPos.CODEC).orElse(null);
        homeYaw = input.getFloatOr("NpcHomeYaw", getYRot());
        owner = input.read("NpcOwner", UUIDUtil.CODEC).orElse(null);
        hireExpires = input.getLongOr("NpcHireExpires", -1L);
        try {
            companionMode = CompanionMode.valueOf(input.getStringOr("NpcCompanionMode", CompanionMode.FOLLOW.name()));
        } catch (IllegalArgumentException exception) {
            companionMode = CompanionMode.FOLLOW;
        }
        var uses = input.read("NpcShopUses", Codec.INT.listOf()).orElse(List.of());
        shopUses = Arrays.copyOf(uses.stream().mapToInt(Integer::intValue).toArray(), config().shop().trades().size());
        lastRestock = input.getLongOr("NpcLastRestock", 0L);
        respawnTicks = input.getIntOr("NpcRespawnTicks", 0);
        if (respawnTicks > 0) {
            entityData.set(DATA_DOWNED, true);
            setInvisible(true);
            setNoAi(true);
        }
    }
}

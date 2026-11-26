package mod.azure.atomicdawn.powerarmor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.upgrade.ArmorEvents;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.PowerArmorLandingPayload;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorBonuses;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorEffectType;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgradeEvents;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.weapon.action.VatsActions;

@SuppressWarnings("unused")
public class PowerArmorEntity extends LivingEntity {

    public static final int CORE_SLOT = PowerArmorSlot.COUNT;

    public static final int SLOT_COUNT = PowerArmorSlot.COUNT + 1;

    private static final float RIDER_FOOT_OFFSET = 0.2F;

    public static final float WALK_SPEED = 0.11F;

    public static final double JUMP_VELOCITY = 0.42;

    public static final int JET_PACK_AP_DELAY = 6;

    public static final int ENTER_TICKS = 20;

    private static final List<EntityDataAccessor<ItemStack>> ITEMS = new ArrayList<>();

    static {
        for (var i = 0; i < SLOT_COUNT; i++) {
            ITEMS.add(SynchedEntityData.defineId(PowerArmorEntity.class, EntityDataSerializers.ITEM_STACK));
        }
    }

    private static final EntityDataAccessor<Float> DOCK_YAW = SynchedEntityData.defineId(
        PowerArmorEntity.class,
        EntityDataSerializers.FLOAT
    );

    private static final EntityDataAccessor<Boolean> DOCKED = SynchedEntityData.defineId(
        PowerArmorEntity.class,
        EntityDataSerializers.BOOLEAN
    );

    private static final EntityDataAccessor<Float> DRAIN_RATE = SynchedEntityData.defineId(
        PowerArmorEntity.class,
        EntityDataSerializers.FLOAT
    );

    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(
        PowerArmorEntity.class,
        EntityDataSerializers.BOOLEAN
    );

    public static final float FLY_SPEED_MULTIPLIER = 2.5F;

    public static final float SNEAK_SPEED_MULTIPLIER = 0.35F;

    public static final double FLY_VERTICAL_SPEED = 0.3;

    @Nullable
    private BlockPos stationPos;

    private float drainBuffer;

    private float drainWindow;

    private float rechargeBuffer;

    private int landingTicks;

    private int transitionTicks;

    private double lastX;

    private double lastZ;

    private boolean warnedEmpty;

    private int jetPackTicks;

    private int animationState = -1;

    private final Container container = new ArmorContainer();

    private final WalkAnimationState riderWalk = new WalkAnimationState();

    private static boolean forwarding;

    public static boolean isForwarding() {
        return forwarding;
    }

    private boolean preview;

    public PowerArmorEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.11)
            .add(Attributes.STEP_HEIGHT, 1.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        for (var accessor : ITEMS) {
            builder.define(accessor, ItemStack.EMPTY);
        }
        builder.define(DOCKED, false);
        builder.define(DOCK_YAW, 0F);
        builder.define(DRAIN_RATE, 0F);
        builder.define(FLYING, false);
    }

    public ItemStack getSlotItem(int slot) {
        return entityData.get(ITEMS.get(slot));
    }

    public ItemStack getPiece(PowerArmorSlot slot) {
        return getSlotItem(slot.ordinal());
    }

    public ItemStack getCore() {
        return getSlotItem(CORE_SLOT);
    }

    public void setSlotItem(int slot, ItemStack stack) {
        entityData.set(ITEMS.get(slot), stack.copy());
    }

    public Container container() {
        return container;
    }

    public boolean isDocked() {
        return entityData.get(DOCKED);
    }

    @Nullable
    public BlockPos stationPos() {
        return stationPos;
    }

    public float drainPerSecond() {
        return entityData.get(DRAIN_RATE);
    }

    public boolean isFunctioning() {
        return PowerCoreItem.charge(getCore()) > 0;
    }

    public boolean canMove() {
        return isFunctioning() && landingTicks <= 0 && transitionTicks <= 0 && !isDocked();
    }

    private boolean exitRequested;

    public boolean isExitRequested() {
        return exitRequested;
    }

    public void exit(Player pilot) {
        if (!hasPassenger(pilot) || transitionTicks > 0) {
            return;
        }
        exitRequested = true;
        try {
            pilot.stopRiding();
        } finally {
            exitRequested = false;
        }
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    public boolean canFly() {
        return getControllingPassenger() instanceof Player player && player.mayFly() && !isDocked();
    }

    public void setFlying(boolean flying) {
        var value = flying && canFly();
        if (value != isFlying()) {
            entityData.set(FLYING, value);
        }
        if (value) {
            resetFallDistance();
        }
    }

    public boolean canSteer() {
        return canMove() || (isFlying() && transitionTicks <= 0 && !isDocked());
    }

    @Override
    protected double getEffectiveGravity() {
        return isFlying() ? 0.0 : super.getEffectiveGravity();
    }

    public boolean hasAllPieces() {
        for (var slot : PowerArmorSlot.all()) {
            if (getPiece(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public WalkAnimationState riderWalk() {
        return riderWalk;
    }

    public int landingTicks() {
        return landingTicks;
    }

    public static boolean accepts(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (slot == CORE_SLOT) {
            return stack.getItem() instanceof PowerCoreItem;
        }
        return stack.getItem() instanceof PowerArmorPieceItem piece && piece.slot().ordinal() == slot;
    }

    public void dock(BlockPos station, Vec3 position, Direction facing) {
        stationPos = station.immutable();
        entityData.set(DOCKED, true);
        ejectPassengers();
        setPos(position);
        entityData.set(DOCK_YAW, facing.toYRot());
        holdDockYaw();
        setDeltaMovement(Vec3.ZERO);
    }

    private void holdDockYaw() {
        var yaw = entityData.get(DOCK_YAW);
        setYRot(yaw);
        yRotO = yaw;
        setXRot(0F);
        xRotO = 0F;
        yBodyRot = yaw;
        yBodyRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
    }

    public void undock() {
        stationPos = null;
        entityData.set(DOCKED, false);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (canSteer()) {
            setRot(player.getYRot(), 0F);
            yRotO = getYRot();
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 travelVector) {
        if (!canSteer()) {
            return Vec3.ZERO;
        }
        var strafe = player.xxa * 0.6F;
        var forward = player.zza;
        if (forward < 0F) {
            forward *= 0.5F;
        }
        return new Vec3(strafe, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player player) {
        var speed = (float) (WALK_SPEED * AtomicDawn.CONFIG.common.equipment.powerArmor.movement.POWER_ARMOR_SPEED)
            * PowerArmorUpgradeEvents.speedMultiplier(player, this);
        if (isFlying()) {
            return speed * FLY_SPEED_MULTIPLIER;
        }
        return player.isShiftKeyDown() ? speed * SNEAK_SPEED_MULTIPLIER : speed;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    @Override
    public boolean isPickable() {
        return !preview && getPassengers().isEmpty() && super.isPickable();
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof LivingEntity rider) {
            rider.setYBodyRot(getYRot());
        }
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(
        @NotNull Entity entity,
        @NotNull EntityDimensions dimensions,
        float scale
    ) {
        return new Vec3(0, RIDER_FOOT_OFFSET + entity.getVehicleAttachmentPoint(this).y, 0);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        var behind = Vec3.directionFromRotation(0F, getYRot()).scale(-1.3);
        var target = position().add(behind.x, 0, behind.z);
        var box = passenger.getBoundingBox().move(target.subtract(passenger.position()));
        return level().noCollision(passenger, box) ? target : super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide()) {
            entityData.set(FLYING, false);

            setInvisible(false);
        }
        if (passenger instanceof Player player && !level().isClientSide()) {
            player.setInvisible(player.hasEffect(MobEffects.INVISIBILITY));
            playSound(ADSounds.POWER_ARMOR_EXIT.get(), 1F, 1F);
            transitionTicks = ENTER_TICKS;
            PowerArmorAnimations.exit(this);
        }
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public @NotNull InteractionResult interact(
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull Vec3 location
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        var held = player.getItemInHand(hand);

        if (held.getItem() instanceof PowerCoreItem && getCore().isEmpty()) {
            setSlotItem(CORE_SLOT, held.copyWithCount(1));
            held.consume(1, player);
            playSound(ADSounds.POWER_ARMOR_CORE.get(), 1F, 1F);
            return InteractionResult.SUCCESS_SERVER;
        }

        if (player.isSecondaryUseActive()) {
            if (isDocked() && stationPos != null && player instanceof ServerPlayer serverPlayer) {
                PowerArmorStationBlock.openMenu(serverPlayer, this);
                return InteractionResult.SUCCESS_SERVER;
            }

            if (getPassengers().isEmpty() && !isDocked()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerItems.give(serverPlayer, PowerArmorChassisItem.fromEntity(this));
                }

                discard();
                return InteractionResult.SUCCESS_SERVER;
            }

            return InteractionResult.PASS;
        }

        if (held.isEmpty() && getPassengers().isEmpty()) {
            if (hasBoundArmor(player)) {
                player.sendSystemMessage(Component.translatable("power_armor.atomicdawn.bound_armor"));
                return InteractionResult.FAIL;
            }

            if (isDocked()) {
                undock();
            }

            if (player.startRiding(this)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    unequipArmor(serverPlayer);
                    serverPlayer.sendSystemMessage(
                        Component.translatable(
                            "power_armor.atomicdawn.exit_hint",
                            Component.keybind("key.atomicdawn.exit_power_armor")
                        ),
                        true
                    );
                }
                playSound(ADSounds.POWER_ARMOR_ENTER.get(), 1F, 1F);
                transitionTicks = ENTER_TICKS;
                PowerArmorAnimations.enter(this);
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return InteractionResult.PASS;
    }

    public static final EquipmentSlot[] PILOT_ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET };

    private static boolean hasBoundArmor(Player player) {
        if (player.isCreative()) {
            return false;
        }
        for (var slot : PILOT_ARMOR_SLOTS) {
            if (EnchantmentHelper.has(player.getItemBySlot(slot), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                return true;
            }
        }
        return false;
    }

    public static void unequipArmor(ServerPlayer player) {
        ArmorEvents.onPowerArmorStow(player, player.getItemBySlot(EquipmentSlot.CHEST));
        for (var slot : PILOT_ARMOR_SLOTS) {
            var worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) {
                continue;
            }
            player.setItemSlot(slot, ItemStack.EMPTY);
            PlayerItems.give(player, worn);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isDocked() && getPassengers().isEmpty()) {
            holdDockYaw();
        }
        if (level().isClientSide()) {
            tickAnimation();
            return;
        }
        if (landingTicks > 0) {
            landingTicks--;
        }
        if (transitionTicks > 0) {
            transitionTicks--;
        }
        if (isDocked()) {
            setDeltaMovement(Vec3.ZERO);
            if (
                tickCount % 20 == 0 && (stationPos == null || !PowerArmorStationBlock.isStationMaster(
                    level(),
                    stationPos
                ))
            ) {
                undock();
            }
        }
        var rider = getControllingPassenger();
        if (isFlying()) {
            if (canFly()) {
                resetFallDistance();
            } else {
                entityData.set(FLYING, false);
            }
        }
        if (rider instanceof ServerPlayer player) {
            var hide = hasAllPieces() || player.hasEffect(MobEffects.INVISIBILITY);
            if (player.isInvisible() != hide) {
                player.setInvisible(hide);
            }
            var dx = getX() - lastX;
            var dz = getZ() - lastZ;
            if (dx * dx + dz * dz > 0.0001 && isFunctioning()) {
                drain((float) AtomicDawn.CONFIG.common.equipment.powerArmor.fusionCores.FUSION_CORE_MOVE_COST);
                var kinetic = PowerArmorBonuses.of(this).get(PowerArmorEffectType.KINETIC_CHARGE);
                if (kinetic > 0F) {
                    recharge(kinetic * (float) Math.sqrt(dx * dx + dz * dz));
                }
            }
            if (
                !onGround() && !isFlying() && player.getLastClientInput().jump()
                    && PowerArmorBonuses.of(this).has(PowerArmorEffectType.JET_PACK)
            ) {
                jetPackTicks++;
                if (jetPackTicks >= JET_PACK_AP_DELAY && canJetPack()) {
                    var apCost =
                        (float) AtomicDawn.CONFIG.common.equipment.powerArmor.movement.POWER_ARMOR_JETPACK_AP_COST;
                    if (player.hasInfiniteMaterials() || apCost <= 0F || VatsActions.spend(player, apCost)) {
                        drain((float) AtomicDawn.CONFIG.common.equipment.powerArmor.movement.POWER_ARMOR_JETPACK_COST);
                    }
                }
            } else {
                jetPackTicks = 0;
            }
            if (!isFunctioning() && !getCore().isEmpty() && !warnedEmpty) {
                warnedEmpty = true;
                player.sendSystemMessage(Component.translatable("power_armor.atomicdawn.core_depleted"), true);
            }
        }
        lastX = getX();
        lastZ = getZ();
        if (tickCount % 20 == 0) {
            entityData.set(DRAIN_RATE, drainWindow);
            drainWindow = 0F;
        }
    }

    private void tickAnimation() {
        var dx = getX() - xo;
        var dz = getZ() - zo;
        riderWalk.update((float) Math.min(1.0, Math.sqrt(dx * dx + dz * dz) * 4.0), 0.4F, 1F);
        var state = getPassengers().isEmpty() ? 0 : dx * dx + dz * dz > 0.0004 ? 2 : 1;
        if (state == animationState) {
            return;
        }
        animationState = state;
        switch (state) {
            case 0 -> PowerArmorAnimations.empty(this);
            case 2 -> PowerArmorAnimations.walk(this);
            default -> PowerArmorAnimations.idle(this);
        }
    }

    public void drain(float amount) {
        if (amount <= 0F || level().isClientSide()) {
            return;
        }
        amount *= Math.max(0F, 1F - PowerArmorBonuses.of(this).get(PowerArmorEffectType.CORE_DRAIN));
        if (getControllingPassenger() instanceof Player pilot) {
            amount *= PerkHooks.powerArmorDrainMultiplier(pilot);
        }
        var core = getCore();
        if (core.isEmpty()) {
            return;
        }
        drainWindow += amount;
        drainBuffer += amount;
        if (drainBuffer < 1F) {
            return;
        }
        var whole = (int) drainBuffer;
        drainBuffer -= whole;
        var copy = core.copy();
        PowerCoreItem.setCharge(copy, PowerCoreItem.charge(copy) - whole);
        setSlotItem(CORE_SLOT, copy);
        if (PowerCoreItem.charge(copy) > 0) {
            warnedEmpty = false;
        }
    }

    public void recharge(float amount) {
        var core = getCore();
        if (amount <= 0F || core.isEmpty() || level().isClientSide()) {
            return;
        }
        rechargeBuffer += amount;
        if (rechargeBuffer < 1F) {
            return;
        }
        var whole = (int) rechargeBuffer;
        rechargeBuffer -= whole;
        var copy = core.copy();
        PowerCoreItem.setCharge(copy, Math.min(PowerCoreItem.capacity(), PowerCoreItem.charge(copy) + whole));
        setSlotItem(CORE_SLOT, copy);
    }

    public boolean canJetPack() {
        return PowerArmorBonuses.of(this).has(PowerArmorEffectType.JET_PACK) && isFunctioning() && !isDocked()
            && !isFlying();
    }

    public void damagePiece(PowerArmorSlot slot, float amount) {
        var stack = getPiece(slot);
        if (stack.isEmpty() || !stack.isDamageableItem() || amount <= 0F) {
            return;
        }
        amount *= Math.max(0F, 1F - PowerArmorBonuses.of(this).get(PowerArmorEffectType.PIECE_DURABILITY));
        if (amount < 1F && random.nextFloat() >= amount) {
            return;
        }
        var copy = stack.copy();
        copy.setDamageValue(Math.min(copy.getMaxDamage() - 1, copy.getDamageValue() + Math.max(1, Math.round(amount))));
        setSlotItem(slot.ordinal(), copy);
    }

    public float absorb(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return amount;
        }
        var coverage = 0F;
        var intact = new ArrayList<PowerArmorSlot>();
        for (var slot : PowerArmorSlot.all()) {
            var piece = getPiece(slot);
            if (!piece.isEmpty() && !PowerArmorPieceItem.isBroken(piece)) {
                coverage += slot.coverage();
                intact.add(slot);
            }
        }
        if (intact.isEmpty()) {
            return amount;
        }
        var absorbed = amount * coverage
            * AtomicDawn.CONFIG.common.equipment.powerArmor.protection.POWER_ARMOR_MAX_REDUCTION;
        var pick = random.nextFloat() * coverage;
        for (var slot : intact) {
            pick -= slot.coverage();
            if (pick <= 0F) {
                damagePiece(
                    slot,
                    (float) (absorbed
                        * AtomicDawn.CONFIG.common.equipment.powerArmor.protection.POWER_ARMOR_CONDITION_PER_DAMAGE)
                );
                break;
            }
        }
        return (float) Math.max(0F, amount - absorbed);
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (
            isInvulnerableTo(level, source) || source.is(DamageTypeTags.IS_FALL) || source.is(ADDamageTypes.RADIATION)
        ) {

            return false;
        }
        var rider = getControllingPassenger();
        if (rider != null && source.getEntity() == rider) {
            return false;
        }
        var remaining = absorb(source, amount);
        if (rider != null && remaining > 0F) {
            forwarding = true;
            try {
                rider.hurtServer(level, source, remaining);
            } finally {
                forwarding = false;
            }
        }
        playSound(SoundEvents.IRON_GOLEM_HURT, 0.6F, 1.2F);
        return true;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, @NotNull DamageSource source) {
        if (fallDistance <= 3.0 || isFlying()) {
            return false;
        }
        if (isFunctioning()) {
            var absorb = PowerArmorBonuses.of(this).get(PowerArmorEffectType.LANDING_ABSORB);
            if (absorb >= 1F) {
                return false;
            }
            var threshold = AtomicDawn.CONFIG.common.equipment.powerArmor.movement.POWER_ARMOR_LANDING_HEIGHT * (1F
                + absorb * 3F);
            if (fallDistance >= threshold) {
                land(fallDistance);
            }
            return false;
        }
        var damage = (float) (fallDistance - 3.0) * 2F * multiplier;
        damagePiece(PowerArmorSlot.LEFT_LEG, damage);
        damagePiece(PowerArmorSlot.RIGHT_LEG, damage);
        return false;
    }

    private void land(double fallDistance) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        landingTicks = AtomicDawn.CONFIG.common.equipment.powerArmor.movement.POWER_ARMOR_LANDING_TICKS;
        var intensity = (float) Mth.clamp(
            (fallDistance - AtomicDawn.CONFIG.common.equipment.powerArmor.movement.POWER_ARMOR_LANDING_HEIGHT) / 20.0
                + 0.5,
            0.5,
            1.0
        );
        serverLevel.playSound(null, blockPosition(), ADSounds.POWER_ARMOR_LAND.get(), SoundSource.PLAYERS, 1.5F, 0.9F);
        BlockState below = level().getBlockState(blockPosition().below());
        if (!below.isAir()) {
            serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, below),
                getX(),
                getY() + 0.1,
                getZ(),
                Math.round(30 * intensity) + 10,
                0.9,
                0.1,
                0.9,
                0.25
            );
        }
        PowerArmorAnimations.landing(this);
        if (getControllingPassenger() instanceof ServerPlayer player) {
            ADNetwork.sendTo(player, new PowerArmorLandingPayload(intensity, landingTicks));
        }
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(ADSounds.POWER_ARMOR_STEP.get(), 0.5F, 1F);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel serverLevel) {
            spawnAtLocation(serverLevel, PowerArmorChassisItem.fromEntity(this));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        var items = new ArrayList<ItemStack>();
        for (var i = 0; i < SLOT_COUNT; i++) {
            items.add(getSlotItem(i));
        }
        output.store("PowerArmorItems", ItemStack.OPTIONAL_CODEC.listOf(), items);
        output.putBoolean("Docked", isDocked());
        output.putFloat("DockYaw", entityData.get(DOCK_YAW));
        if (stationPos != null) {
            output.store("Station", BlockPos.CODEC, stationPos);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("PowerArmorItems", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(items -> {
            for (var i = 0; i < Math.min(items.size(), SLOT_COUNT); i++) {
                setSlotItem(i, items.get(i));
            }
        });
        entityData.set(DOCKED, input.getBooleanOr("Docked", false));
        entityData.set(DOCK_YAW, input.getFloatOr("DockYaw", getYRot()));
        stationPos = input.read("Station", BlockPos.CODEC).orElse(null);
    }

    private final class ArmorContainer implements Container {

        @Override
        public int getContainerSize() {
            return SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (var i = 0; i < SLOT_COUNT; i++) {
                if (!getSlotItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return getSlotItem(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            var stack = getSlotItem(slot);
            if (stack.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            var copy = stack.copy();
            var result = copy.split(amount);
            setSlotItem(slot, copy);
            return result;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            var stack = getSlotItem(slot);
            setSlotItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            setSlotItem(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(@NotNull Player player) {
            return isAlive() && player.distanceToSqr(PowerArmorEntity.this) < 64.0;
        }

        @Override
        public void clearContent() {
            for (var i = 0; i < SLOT_COUNT; i++) {
                setSlotItem(i, ItemStack.EMPTY);
            }
        }
    }
}

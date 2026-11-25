package mod.azure.atomicdawn.entity.scorched;

import mod.azure.azurelib.hitbox.AzHitboxHolder;
import mod.azure.azurelib.hitbox.AzHitboxSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorched.ai.ScorchedBrain;
import mod.azure.atomicdawn.registry.ADSounds;

@SuppressWarnings({ "unused", "deprecation" })
public class ScorchedEntity extends Monster implements AzHitboxHolder {

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
        ScorchedEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(
        ScorchedEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_POSE = SynchedEntityData.defineId(
        ScorchedEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_STANCE = SynchedEntityData.defineId(
        ScorchedEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(
        ScorchedEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION_ID = SynchedEntityData.defineId(
        ScorchedEntity.class,
        EntityDataSerializers.INT
    );

    public static final int WAKE_DURATION = 30;

    private static final double RUN_THRESHOLD_SQR = 0.17 * 0.17;

    private static final double WALK_THRESHOLD_SQR = 0.015 * 0.015;

    private static final int RADIO_FLASH_TICKS = 24;

    @Nullable
    private final ScorchedBrain brain;

    private boolean dormant;

    private int wakeTicks;

    @Nullable
    private BlockPos home;

    private int patrolRadius = 32;

    private int ambienceTicks = 100;

    private int clientState = -1;

    private int clientPose = -1;

    private int clientStance = -1;

    private int clientActionId;

    private int clientRedispatchAt = -1;

    private int clientRadioFlashUntil;

    public ScorchedEntity(EntityType<? extends ScorchedEntity> type, Level level) {
        super(type, level);
        this.brain = level.isClientSide() ? null : new ScorchedBrain(this);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = AtomicDawn.CONFIG.scorched;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, config.ATTACK_DAMAGE)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public AzHitboxSet getHitboxSet() {
        return ScorchedHitboxes.SET;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, ScorchedVariant.STANDARD.ordinal());
        builder.define(DATA_STATE, ScorchedState.IDLE.ordinal());
        builder.define(DATA_POSE, 0);
        builder.define(DATA_STANCE, ScorchedStance.NONE.ordinal());
        builder.define(DATA_ACTION, ScorchedAction.NONE.ordinal());
        builder.define(DATA_ACTION_ID, 0);
    }

    @Nullable
    public ScorchedBrain brain() {
        return brain;
    }

    public ScorchedVariant variant() {
        return ScorchedVariant.byIndex(entityData.get(DATA_VARIANT));
    }

    public void setVariant(ScorchedVariant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public ScorchedState animationState() {
        return ScorchedState.byIndex(entityData.get(DATA_STATE));
    }

    public int dormantPose() {
        return entityData.get(DATA_POSE);
    }

    public ScorchedStance stance() {
        return ScorchedStance.byIndex(entityData.get(DATA_STANCE));
    }

    public void setStance(ScorchedStance stance) {
        if (dormant || !isAlive()) {
            stance = ScorchedStance.NONE;
        }
        if (stance.ordinal() != entityData.get(DATA_STANCE)) {
            entityData.set(DATA_STANCE, stance.ordinal());
        }
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public int patrolRadius() {
        return patrolRadius;
    }

    public boolean isDormant() {
        return dormant;
    }

    public boolean isWaking() {
        return dormant && wakeTicks > 0;
    }

    public void setDormant(boolean value) {
        dormant = value;
        wakeTicks = 0;
        if (value) {
            entityData.set(DATA_POSE, random.nextInt(ScorchedState.DORMANT_POSES));
            getNavigation().stop();
            setStance(ScorchedStance.NONE);
        }
        updateState();
    }

    public void requestWake() {
        if (!dormant || wakeTicks > 0) {
            return;
        }
        wakeTicks = WAKE_DURATION;
        playAction(ScorchedAction.WAKE);
    }

    public boolean inMeleeRange(@Nullable LivingEntity target) {
        return target != null && isWithinMeleeAttackRange(target);
    }

    public void playAction(ScorchedAction action) {
        entityData.set(DATA_ACTION, action.ordinal());
        entityData.set(DATA_ACTION_ID, entityData.get(DATA_ACTION_ID) + 1);
    }

    public void alertCry() {
        playSound(ADSounds.SCORCHED_ALERT.get(), 1.0F, getVoicePitch());
    }

    public void attackCry() {
        if (random.nextInt(3) == 0) {
            playSound(ADSounds.SCORCHED_ATTACK.get(), 0.9F, getVoicePitch());
        }
    }

    public void receiveHiveAlert(LivingEntity target, BlockPos origin, long now) {
        if (brain != null) {
            brain.acceptShared(target, origin, now);
        }
    }

    public void meleeHit(ServerLevel level, LivingEntity target) {
        var damage = ScorchedWeapons.meleeDamage(this);
        if (damage > 0F) {
            var hit = target.hurtServer(level, damageSources().mobAttack(this), damage);
            if (hit) {
                target.knockback(
                    0.4,
                    getX() - target.getX(),
                    getZ() - target.getZ(),
                    damageSources().mobAttack(this),
                    0.0F
                );
            }
            return;
        }
        doHurtTarget(level, target);
    }

    public boolean radioLightOn() {
        if (variant() != ScorchedVariant.OFFICER || !isAlive()) {
            return false;
        }
        if (tickCount < clientRadioFlashUntil) {
            return (tickCount / 3) % 2 == 0;
        }
        return tickCount % 40 < 4;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientAnimations();
            return;
        }
        if (dormant) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (wakeTicks > 0 && --wakeTicks == 0) {
                dormant = false;
                if (brain != null) {
                    brain.onWake(level().getGameTime());
                }
            }
        }
        if (brain != null && isAlive() && !isNoAi()) {
            brain.tick();
            var target = brain.target();
            if (getTarget() != target) {
                setTarget(target);
            }
        }
        if (isAlive()) {
            ambience();
        }
        updateState();
    }

    private void ambience() {
        if (--ambienceTicks > 0) {
            return;
        }
        switch (variant()) {
            case OFFICER -> {
                playSound(ADSounds.SCORCHED_RADIO.get(), 0.6F, 0.95F + random.nextFloat() * 0.1F);
                playAction(ScorchedAction.RADIO);
                ambienceTicks = 100 + random.nextInt(120);
            }
            case HOLIDAY -> {
                var moving = animationState() == ScorchedState.WALK || animationState() == ScorchedState.RUN;
                if (moving || random.nextInt(4) == 0) {
                    playSound(ADSounds.SCORCHED_JINGLE.get(), 0.5F, 0.9F + random.nextFloat() * 0.2F);
                }
                ambienceTicks = moving ? 20 + random.nextInt(20) : 80 + random.nextInt(80);
            }
            case SPOOKY -> {
                if (!dormant) {
                    playSound(ADSounds.SCORCHED_EERIE.get(), 0.7F, 0.8F + random.nextFloat() * 0.2F);
                }
                ambienceTicks = 160 + random.nextInt(160);
            }
            case STANDARD -> ambienceTicks = 200;
        }
    }

    private void updateState() {
        if (level().isClientSide()) {
            return;
        }
        ScorchedState state;
        if (!isAlive()) {
            state = ScorchedState.DEAD;
        } else if (dormant) {
            state = ScorchedState.DORMANT;
        } else {
            var dx = getX() - xo;
            var dz = getZ() - zo;
            var speed = dx * dx + dz * dz;
            state = speed > RUN_THRESHOLD_SQR
                ? ScorchedState.RUN
                : speed > WALK_THRESHOLD_SQR ? ScorchedState.WALK : ScorchedState.IDLE;
        }
        if (state.ordinal() != entityData.get(DATA_STATE)) {
            entityData.set(DATA_STATE, state.ordinal());
        }
    }

    private void clientAnimations() {
        int state = entityData.get(DATA_STATE);
        int pose = entityData.get(DATA_POSE);
        if (state != clientState || pose != clientPose) {
            clientState = state;
            clientPose = pose;
            clientRedispatchAt = tickCount + 10;
            ScorchedAnimations.state(this, ScorchedState.byIndex(state), pose);
        } else if (tickCount == clientRedispatchAt) {
            ScorchedAnimations.state(this, ScorchedState.byIndex(state), pose);
        }
        int stance = entityData.get(DATA_STANCE);
        if (stance != clientStance) {
            clientStance = stance;
            ScorchedAnimations.stance(this, ScorchedStance.byIndex(stance));
        }
        var actionId = entityData.get(DATA_ACTION_ID);
        if (actionId != clientActionId) {
            clientActionId = actionId;
            var action = ScorchedAction.byIndex(entityData.get(DATA_ACTION));
            if (action == ScorchedAction.RADIO) {
                clientRadioFlashUntil = tickCount + RADIO_FLASH_TICKS;
            } else {
                ScorchedAnimations.action(this, action);
            }
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        var hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive() && brain != null && source.getEntity() instanceof LivingEntity attacker) {
            brain.onHurt(attacker, level.getGameTime());
        }
        return hurt;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        dormant = false;
        entityData.set(DATA_STANCE, ScorchedStance.NONE.ordinal());
        playAction(ScorchedAction.STOP);
        updateState();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull EntitySpawnReason reason,
        @Nullable SpawnGroupData data
    ) {
        var config = AtomicDawn.CONFIG.scorched;
        var variant = ScorchedSpawning.rollVariant(random);
        setVariant(variant);
        setItemSlot(EquipmentSlot.MAINHAND, ScorchedWeapons.roll(level.getLevel(), variant, random));
        home = ScorchedSpawning.fissureCenter(level.getLevel(), blockPosition()).orElse(blockPosition());
        var min = Math.min(config.PATROL_RADIUS_MIN, config.PATROL_RADIUS_MAX);
        var max = Math.max(config.PATROL_RADIUS_MIN, config.PATROL_RADIUS_MAX);
        patrolRadius = Mth.randomBetweenInclusive(random, min, max);
        var natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION
            || reason == EntitySpawnReason.STRUCTURE;
        if (natural && random.nextDouble() < config.DORMANT_SPAWN_CHANCE) {
            setDormant(true);
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variant().serializedName());
        output.putBoolean("Dormant", dormant);
        output.putInt("DormantPose", dormantPose());
        output.putInt("PatrolRadius", patrolRadius);
        if (home != null) {
            output.putBoolean("HasHome", true);
            output.putInt("HomeX", home.getX());
            output.putInt("HomeY", home.getY());
            output.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(ScorchedVariant.byName(input.getStringOr("Variant", "")));
        patrolRadius = input.getIntOr("PatrolRadius", 32);
        home = input.getBooleanOr("HasHome", false)
            ? new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0))
            : null;
        setDormant(input.getBooleanOr("Dormant", false));
        if (dormant) {
            entityData.set(DATA_POSE, Math.floorMod(input.getIntOr("DormantPose", 0), ScorchedState.DORMANT_POSES));
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return dormant ? null : ADSounds.SCORCHED_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return ADSounds.SCORCHED_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return ADSounds.SCORCHED_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !dormant && super.removeWhenFarAway(distance);
    }

    @Override
    protected boolean considersEntityAsAlly(@NotNull Entity other) {
        return other instanceof ScorchedEntity
            || super.considersEntityAsAlly(other);
    }
}

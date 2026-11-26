package mod.azure.atomicdawn.entity.ghoul;

import mod.azure.azurelib.hitbox.AzHitboxHolder;
import mod.azure.azurelib.hitbox.AzHitboxSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ghoul.ai.FeralGhoulBrain;
import mod.azure.atomicdawn.helper.ADSerialization;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADSounds;

@SuppressWarnings({ "unused", "deprecation" })
public class FeralGhoulEntity extends Monster implements AzHitboxHolder {

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
        FeralGhoulEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(
        FeralGhoulEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(
        FeralGhoulEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION_ID = SynchedEntityData.defineId(
        FeralGhoulEntity.class,
        EntityDataSerializers.INT
    );

    private static final int WAKE_DURATION = 30;

    private static final double RUN_THRESHOLD_SQR = 0.17 * 0.17;

    private static final double WALK_THRESHOLD_SQR = 0.015 * 0.015;

    @Nullable
    private final FeralGhoulBrain brain;

    private boolean dormant;

    private int wakeTicks;

    private int clientState = -1;

    private int clientActionId;

    private int clientRedispatchAt = -1;

    public FeralGhoulEntity(EntityType<? extends FeralGhoulEntity> type, Level level) {
        super(type, level);
        this.brain = level.isClientSide() ? null : new FeralGhoulBrain(this);
        this.xpReward = 8;
    }

    @Override
    public AzHitboxSet getHitboxSet() {
        return FeralGhoulHitboxes.SET;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = AtomicDawn.CONFIG.server.entities.mutants.feralGhouls.attributes;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, config.ATTACK_DAMAGE)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, FeralGhoulVariant.DUSTY_BEIGE.ordinal());
        builder.define(DATA_STATE, FeralGhoulState.IDLE.ordinal());
        builder.define(DATA_ACTION, FeralGhoulAction.NONE.ordinal());
        builder.define(DATA_ACTION_ID, 0);
    }

    public FeralGhoulVariant variant() {
        return FeralGhoulVariant.byIndex(entityData.get(DATA_VARIANT));
    }

    public void setVariant(FeralGhoulVariant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public FeralGhoulState animationState() {
        return FeralGhoulState.byIndex(entityData.get(DATA_STATE));
    }

    public boolean isDormant() {
        return dormant;
    }

    public void setDormant(boolean value) {
        dormant = value;
        wakeTicks = 0;
        if (value) {
            getNavigation().stop();
        }
        updateState();
    }

    public void requestWake() {
        if (!dormant || wakeTicks > 0) {
            return;
        }
        wakeTicks = WAKE_DURATION;
        playAction(FeralGhoulAction.WAKE);
    }

    public boolean inClawRange(@Nullable LivingEntity target) {
        return target != null && isWithinMeleeAttackRange(target);
    }

    public void playAction(FeralGhoulAction action) {
        entityData.set(DATA_ACTION, action.ordinal());
        entityData.set(DATA_ACTION_ID, entityData.get(DATA_ACTION_ID) + 1);
    }

    public void attackCry() {
        playSound(ADSounds.FERAL_GHOUL_ATTACK.get(), 1.0F, getVoicePitch());
    }

    public void screech() {
        playSound(ADSounds.FERAL_GHOUL_YELL.get(), 0.8F, 1F);
    }

    public void alert(LivingEntity target, boolean fromPack) {
        if (brain == null || !FeralGhoulBrain.validTarget(target)) {
            return;
        }
        brain.setTarget(target);
        if (fromPack) {
            brain.suppressScream(200 + random.nextInt(200));
        }
        if (dormant) {
            requestWake();
        }
    }

    public void alertPack(LivingEntity target) {
        var range = AtomicDawn.CONFIG.server.entities.mutants.feralGhouls.behavior.ALERT_RANGE;
        for (
            var other : level().getEntitiesOfClass(
                FeralGhoulEntity.class,
                getBoundingBox().inflate(range),
                ghoul -> ghoul != this && ghoul.isAlive()
            )
        ) {
            other.alert(target, true);
        }
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
            }
        }
        if (brain != null && isAlive() && !isNoAi()) {
            brain.tick();
            var target = brain.target();
            if (getTarget() != target) {
                setTarget(target);
            }
        }
        updateState();
    }

    private void updateState() {
        if (level().isClientSide()) {
            return;
        }
        FeralGhoulState state;
        if (!isAlive()) {
            state = FeralGhoulState.DEAD;
        } else if (dormant) {
            state = FeralGhoulState.DORMANT;
        } else {
            var dx = getX() - xo;
            var dz = getZ() - zo;
            var speed = dx * dx + dz * dz;
            if (speed > RUN_THRESHOLD_SQR) {
                var target = getTarget();
                state = target != null && target.isAlive() ? FeralGhoulState.CHARGE : FeralGhoulState.RUN;
            } else {
                state = speed > WALK_THRESHOLD_SQR ? FeralGhoulState.WALK : FeralGhoulState.IDLE;
            }
        }
        if (state.ordinal() != entityData.get(DATA_STATE)) {
            entityData.set(DATA_STATE, state.ordinal());
        }
    }

    private void clientAnimations() {
        int state = entityData.get(DATA_STATE);
        if (state != clientState) {
            clientState = state;
            clientRedispatchAt = tickCount + 10;
            FeralGhoulAnimations.state(this, FeralGhoulState.byIndex(state));
        } else if (tickCount == clientRedispatchAt) {
            FeralGhoulAnimations.state(this, FeralGhoulState.byIndex(state));
        }
        var actionId = entityData.get(DATA_ACTION_ID);
        if (actionId != clientActionId) {
            clientActionId = actionId;
            FeralGhoulAnimations.action(this, FeralGhoulAction.byIndex(entityData.get(DATA_ACTION)));
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        var hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive() && source.getEntity() instanceof LivingEntity attacker) {
            alert(attacker, false);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(@NotNull ServerLevel level, @NotNull Entity target) {
        var hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            RadiationService.expose(
                living,
                (float) AtomicDawn.CONFIG.server.entities.mutants.feralGhouls.attributes.RADS_PER_HIT
            );
        }
        return hit;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        dormant = false;
        updateState();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull EntitySpawnReason reason,
        @Nullable SpawnGroupData data
    ) {
        setVariant(FeralGhoulVariant.byIndex(random.nextInt(FeralGhoulVariant.count())));
        var natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION
            || reason == EntitySpawnReason.STRUCTURE;
        if (
            natural && random
                .nextDouble() < AtomicDawn.CONFIG.server.entities.mutants.feralGhouls.spawning.DORMANT_CHANCE
        ) {
            setDormant(true);
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variant().serializedName());
        output.putBoolean("Dormant", dormant);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(FeralGhoulVariant.byName(ADSerialization.getString(input, "Variant", "")));
        setDormant(ADSerialization.getBoolean(input, "Dormant", false));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return dormant ? null : ADSounds.FERAL_GHOUL_AMBIENT.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return ADSounds.FERAL_GHOUL_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return ADSounds.FERAL_GHOUL_DEATH.get();
    }

    @Override
    protected void playStepSound(@NonNull BlockPos pos, @NonNull BlockState blockState) {
        this.playSound(ADSounds.FERAL_GHOUL_WALK.get(), 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !dormant && super.removeWhenFarAway(distance);
    }
}

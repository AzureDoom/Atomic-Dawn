package mod.azure.atomicdawn.entity.supermutant;

import mod.azure.azurelib.hitbox.AzHitboxHolder;
import mod.azure.azurelib.hitbox.AzHitboxSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.supermutant.ai.SuperMutantBrain;
import mod.azure.atomicdawn.registry.ADSounds;

@SuppressWarnings({ "unused", "deprecation" })
public class SuperMutantEntity extends Monster implements AzHitboxHolder {

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
        SuperMutantEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(
        SuperMutantEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_STANCE = SynchedEntityData.defineId(
        SuperMutantEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(
        SuperMutantEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION_ID = SynchedEntityData.defineId(
        SuperMutantEntity.class,
        EntityDataSerializers.INT
    );

    private static final double RUN_THRESHOLD_SQR = 0.15 * 0.15;

    private static final double WALK_THRESHOLD_SQR = 0.012 * 0.012;

    private static final int DEATH_TICKS = 46;

    private static final double BASE_REACH = 1.5;

    @Nullable
    private final SuperMutantBrain brain;

    @Nullable
    private BlockPos home;

    private int patrolRadius = 20;

    private int busyUntil;

    private int clientState = -1;

    private int clientStance = -1;

    private int clientActionId;

    private int clientRedispatchAt = -1;

    public SuperMutantEntity(EntityType<? extends SuperMutantEntity> type, Level level) {
        super(type, level);
        this.brain = level.isClientSide() ? null : new SuperMutantBrain(this);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = AtomicDawn.CONFIG.superMutants;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.23)
            .add(Attributes.ATTACK_DAMAGE, config.ATTACK_DAMAGE)
            .add(Attributes.ATTACK_KNOCKBACK, 0.8)
            .add(Attributes.ARMOR, config.ARMOR)
            .add(Attributes.ARMOR_TOUGHNESS, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, config.KNOCKBACK_RESISTANCE)
            .add(Attributes.STEP_HEIGHT, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public AzHitboxSet getHitboxSet() {
        return SuperMutantHitboxes.SET;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, SuperMutantVariant.APPALACHIAN.ordinal());
        builder.define(DATA_STATE, SuperMutantState.IDLE.ordinal());
        builder.define(DATA_STANCE, SuperMutantStance.NONE.ordinal());
        builder.define(DATA_ACTION, SuperMutantAction.NONE.ordinal());
        builder.define(DATA_ACTION_ID, 0);
    }

    @Nullable
    public SuperMutantBrain brain() {
        return brain;
    }

    public SuperMutantVariant variant() {
        return SuperMutantVariant.byIndex(entityData.get(DATA_VARIANT));
    }

    public void setVariant(SuperMutantVariant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public SuperMutantStance stance() {
        return SuperMutantStance.byIndex(entityData.get(DATA_STANCE));
    }

    public void setStance(SuperMutantStance stance) {
        if (!isAlive()) {
            stance = SuperMutantStance.NONE;
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

    public boolean busy() {
        return tickCount < busyUntil;
    }

    public void playAction(SuperMutantAction action) {
        entityData.set(DATA_ACTION, action.ordinal());
        entityData.set(DATA_ACTION_ID, entityData.get(DATA_ACTION_ID) + 1);
        if (action == SuperMutantAction.STOP) {
            busyUntil = tickCount;
        } else if (action.duration() > 0) {
            busyUntil = tickCount + action.duration();
        }
    }

    public boolean inReach(@Nullable LivingEntity target, double extra) {
        if (target == null) {
            return false;
        }
        var reach = BASE_REACH + extra + (getBbWidth() + target.getBbWidth()) * 0.5;
        var dx = target.getX() - getX();
        var dz = target.getZ() - getZ();
        var dy = target.getY() - getY();
        return dx * dx + dz * dz <= reach * reach && dy > -2.0 && dy < getBbHeight();
    }

    public void roar() {
        playSound(ADSounds.SUPER_MUTANT_ROAR.get(), 2.0F, getVoicePitch());
    }

    public void taunt() {
        playSound(ADSounds.SUPER_MUTANT_TAUNT.get(), 1.6F, getVoicePitch());
    }

    public void attackGrunt() {
        if (random.nextInt(3) == 0) {
            playSound(ADSounds.SUPER_MUTANT_ATTACK.get(), 1.2F, getVoicePitch());
        }
    }

    public void receivePackCall(LivingEntity target, BlockPos origin, long now) {
        if (brain != null) {
            brain.acceptShared(target, origin, now);
        }
    }

    public void hearDisturbance(Vec3 pos, @Nullable Entity source) {
        if (brain != null && isAlive() && !isNoAi()) {
            brain.onDisturbance(pos, source, level().getGameTime());
        }
    }

    public void meleeHit(ServerLevel level, LivingEntity target) {
        var damage = SuperMutantWeapons.meleeDamage(this);
        var hit = damage > 0F
            ? target.hurtServer(level, damageSources().mobAttack(this), damage)
            : doHurtTarget(level, target);
        if (hit) {
            knock(target, 0.7, 0.15);
        }
    }

    public void slam(ServerLevel level) {
        var config = AtomicDawn.CONFIG.superMutants;
        var forward = Vec3.directionFromRotation(0F, getYRot());
        var center = position().add(forward.scale(1.8));
        var radius = config.SLAM_RADIUS;
        var weapon = SuperMutantWeapons.meleeDamage(this);
        var base = weapon > 0F ? weapon : (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        var damage = base * (float) config.SLAM_DAMAGE_MULTIPLIER;
        var box = new AABB(center, center).inflate(radius, 2.0, radius);
        for (
            var victim : level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                candidate -> candidate != this && candidate.isAlive() && !isAlliedTo(candidate)
                    && (candidate == getTarget() || SuperMutantBrain.validTarget(candidate))
            )
        ) {
            var dx = victim.getX() - center.x;
            var dz = victim.getZ() - center.z;
            var distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > radius) {
                continue;
            }
            var falloff = (float) Mth.clamp(1.0 - distance / (radius * 1.5), 0.4, 1.0);
            if (victim.hurtServer(level, damageSources().mobAttack(this), damage * falloff)) {
                knock(victim, 1.1 * falloff, 0.45 * falloff);
            }
        }
        BlockState ground = level.getBlockState(BlockPos.containing(center.x, getY() - 0.5, center.z));
        if (!ground.isAir()) {
            level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, ground),
                center.x,
                getY() + 0.1,
                center.z,
                48,
                radius * 0.4,
                0.1,
                radius * 0.4,
                0.15
            );
        }
        level.sendParticles(
            ParticleTypes.POOF,
            center.x,
            getY() + 0.2,
            center.z,
            12,
            radius * 0.3,
            0.1,
            radius * 0.3,
            0.05
        );
        playSound(ADSounds.SUPER_MUTANT_SLAM.get(), 1.6F, 0.8F + random.nextFloat() * 0.15F);
    }

    private void knock(LivingEntity victim, double strength, double lift) {
        victim.knockback(
            strength,
            getX() - victim.getX(),
            getZ() - victim.getZ(),
            damageSources().mobAttack(this),
            0.0F
        );
        if (lift > 0 && victim.onGround()) {
            victim.setDeltaMovement(victim.getDeltaMovement().add(0, lift, 0));
            victim.syncVelocity = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientAnimations();
            return;
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
        SuperMutantState state;
        if (!isAlive()) {
            state = SuperMutantState.DEAD;
        } else {
            var dx = getX() - xo;
            var dz = getZ() - zo;
            var speed = dx * dx + dz * dz;
            state = speed > RUN_THRESHOLD_SQR
                ? SuperMutantState.RUN
                : speed > WALK_THRESHOLD_SQR ? SuperMutantState.WALK : SuperMutantState.IDLE;
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
            SuperMutantAnimations.state(this, SuperMutantState.byIndex(state));
        } else if (tickCount == clientRedispatchAt) {
            SuperMutantAnimations.state(this, SuperMutantState.byIndex(state));
        }
        int stance = entityData.get(DATA_STANCE);
        if (stance != clientStance) {
            clientStance = stance;
            SuperMutantAnimations.stance(this, SuperMutantStance.byIndex(stance));
        }
        var actionId = entityData.get(DATA_ACTION_ID);
        if (actionId != clientActionId) {
            clientActionId = actionId;
            SuperMutantAnimations.action(this, SuperMutantAction.byIndex(entityData.get(DATA_ACTION)));
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        if (source.getEntity() instanceof SuperMutantEntity) {
            return false;
        }
        var hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive()) {
            if (!busy() && random.nextInt(3) == 0) {
                playAction(SuperMutantAction.HURT);
            }
            if (brain != null && source.getEntity() instanceof LivingEntity attacker) {
                brain.onHurt(attacker, level.getGameTime());
            }
        }
        return hurt;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        entityData.set(DATA_STANCE, SuperMutantStance.NONE.ordinal());
        playAction(SuperMutantAction.STOP);
        updateState();
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, EntityEvent.POOF);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull EntitySpawnReason reason,
        @Nullable SpawnGroupData data
    ) {
        var config = AtomicDawn.CONFIG.superMutants;
        var egg = reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.DISPENSER;
        setVariant(egg ? SuperMutantVariant.random(random) : SuperMutantSpawning.variantFor(level, blockPosition()));
        if (getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, SuperMutantWeapons.roll(level.getLevel(), random));
        }
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            home = SuperMutantSpawning.campCenter(level.getLevel(), blockPosition()).orElse(blockPosition());
        } else {
            home = blockPosition();
        }
        patrolRadius = Math.max(4, config.PATROL_RADIUS);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Variant", variant().serializedName());
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
        setVariant(SuperMutantVariant.byName(input.getStringOr("Variant", "")));
        patrolRadius = input.getIntOr("PatrolRadius", AtomicDawn.CONFIG.superMutants.PATROL_RADIUS);
        home = input.getBooleanOr("HasHome", false)
            ? new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0))
            : null;
    }

    @Override
    public float getVoicePitch() {
        return 0.75F + (random.nextFloat() - random.nextFloat()) * 0.08F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ADSounds.SUPER_MUTANT_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return ADSounds.SUPER_MUTANT_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return ADSounds.SUPER_MUTANT_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(ADSounds.SUPER_MUTANT_STEP.get(), 0.6F, 0.8F + random.nextFloat() * 0.1F);
    }

    @Override
    protected boolean considersEntityAsAlly(@NotNull Entity other) {
        return other instanceof SuperMutantEntity || SuperMutantBrain.ally(other) || super.considersEntityAsAlly(other);
    }
}

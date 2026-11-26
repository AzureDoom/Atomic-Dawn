package mod.azure.atomicdawn.entity.misterhandy;

import mod.azure.azurelib.hitbox.AzHitboxHolder;
import mod.azure.azurelib.hitbox.AzHitboxSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.misterhandy.ai.MisterHandyBrain;
import mod.azure.atomicdawn.limb.LimbService;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.vats.BodyPart;

@SuppressWarnings({ "unused", "deprecation" })
public class MisterHandyEntity extends Monster implements AzHitboxHolder {

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(
        MisterHandyEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(
        MisterHandyEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION_ID = SynchedEntityData.defineId(
        MisterHandyEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Boolean> DATA_SCANNING = SynchedEntityData.defineId(
        MisterHandyEntity.class,
        EntityDataSerializers.BOOLEAN
    );

    private static final double MOVE_THRESHOLD_SQR = 0.015 * 0.015;

    private static final double MAX_SINK = -0.12;

    private static final int DEATH_TICKS = 40;

    private static final int HOVER_SOUND_INTERVAL = 50;

    private static final double BASE_REACH = 1.2;

    @Nullable
    private final MisterHandyBrain brain;

    @Nullable
    private BlockPos home;

    private int busyUntil;

    private int clientState = -1;

    private int clientActionId;

    public MisterHandyEntity(EntityType<? extends MisterHandyEntity> type, Level level) {
        super(type, level);
        this.brain = level.isClientSide() ? null : new MisterHandyBrain(this);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = AtomicDawn.CONFIG.server.entities.robots.misterHandy;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.attributes.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, config.combat.CLAW_DAMAGE)
            .add(Attributes.ARMOR, config.attributes.ARMOR)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
            .add(Attributes.STEP_HEIGHT, 1.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    public static boolean ally(Entity entity) {
        return entity instanceof MisterHandyEntity || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType())
            .is(MisterHandyBrain.ALLIES);
    }

    @Override
    public AzHitboxSet getHitboxSet() {
        return MisterHandyHitboxes.SET;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, MisterHandyState.IDLE.ordinal());
        builder.define(DATA_ACTION, MisterHandyAction.NONE.ordinal());
        builder.define(DATA_ACTION_ID, 0);
        builder.define(DATA_SCANNING, false);
    }

    @Nullable
    public MisterHandyBrain brain() {
        return brain;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public boolean busy() {
        return tickCount < busyUntil;
    }

    public void setScanning(boolean scanning) {
        if (entityData.get(DATA_SCANNING) != scanning) {
            entityData.set(DATA_SCANNING, scanning);
        }
    }

    public boolean sawDisabled() {
        return LimbService.crippled(this, BodyPart.RIGHT_ARM);
    }

    public boolean flamerDisabled() {
        return LimbService.crippled(this, BodyPart.LEFT_ARM);
    }

    public boolean opticsDamaged() {
        return LimbService.crippled(this, BodyPart.HEAD);
    }

    public void playAction(MisterHandyAction action) {
        entityData.set(DATA_ACTION, action.ordinal());
        entityData.set(DATA_ACTION_ID, entityData.get(DATA_ACTION_ID) + 1);
        busyUntil = action == MisterHandyAction.STOP ? tickCount : tickCount + action.duration();
    }

    public boolean inReach(@Nullable LivingEntity target, double extra) {
        if (target == null) {
            return false;
        }
        var reach = BASE_REACH + extra + (getBbWidth() + target.getBbWidth()) * 0.5;
        var dx = target.getX() - getX();
        var dz = target.getZ() - getZ();
        var dy = target.getY() - getY();
        return dx * dx + dz * dz <= reach * reach && dy > -1.5 && dy < getBbHeight();
    }

    public Vec3 nozzle() {
        var forward = Vec3.directionFromRotation(0F, getYRot());
        var side = Vec3.directionFromRotation(0F, getYRot() - 90F);
        return position().add(0, getBbHeight() * 0.35, 0).add(forward.scale(0.7)).add(side.scale(0.35));
    }

    public void sawHit(ServerLevel level, LivingEntity target) {
        var damage = (float) AtomicDawn.CONFIG.server.entities.robots.misterHandy.combat.SAW_DAMAGE;
        if (target.hurtServer(level, damageSources().mobAttack(this), damage)) {
            target.setInvulnerableTime(0);
            level.sendParticles(
                ParticleTypes.CRIT,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.6,
                target.getZ(),
                6,
                0.2,
                0.2,
                0.2,
                0.2
            );
        }
        playSound(ADSounds.MISTER_HANDY_SAW.get(), 1.0F, 1.2F + random.nextFloat() * 0.2F);
    }

    public void clawHit(ServerLevel level, LivingEntity target) {
        var config = AtomicDawn.CONFIG.server.entities.robots.misterHandy.combat;
        if (target.hurtServer(level, damageSources().mobAttack(this), (float) config.CLAW_DAMAGE)) {
            target.knockback(
                0.6,
                getX() - target.getX(),
                getZ() - target.getZ(),
                damageSources().mobAttack(this),
                0F
            );
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        }
        playSound(ADSounds.MISTER_HANDY_CLAW.get(), 1.0F, 1.0F);
    }

    public void flame(ServerLevel level, @Nullable LivingEntity target, boolean damageTick) {
        var config = AtomicDawn.CONFIG.server.entities.robots.misterHandy.combat;
        var origin = nozzle();
        var aim = target != null
            ? target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin).normalize()
            : Vec3.directionFromRotation(getXRot(), getYRot());
        var range = config.FLAMER_RANGE;
        for (var i = 0; i < 6; i++) {
            var spread = aim.add(
                (random.nextDouble() - 0.5) * 0.25,
                (random.nextDouble() - 0.5) * 0.15,
                (random.nextDouble() - 0.5) * 0.25
            ).normalize().scale(0.35 + random.nextDouble() * 0.25);
            level.sendParticles(
                ParticleTypes.FLAME,
                origin.x,
                origin.y,
                origin.z,
                0,
                spread.x,
                spread.y,
                spread.z,
                1.0
            );
        }
        if (random.nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 2, 0.1, 0.1, 0.1, 0.02);
        }
        if (!damageTick) {
            return;
        }
        var cone = Math.cos(Math.toRadians(config.FLAMER_CONE_DEGREES * 0.5));
        for (
            var victim : level.getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(range),
                living -> living != this && living.isAlive() && !ally(living)
            )
        ) {
            var offset = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(origin);
            var distance = offset.length();
            if (distance > range || offset.normalize().dot(aim) < cone) {
                continue;
            }
            if (!victim.fireImmune()) {
                victim.igniteForSeconds((float) config.FLAMER_BURN_SECONDS);
            }
            victim.hurtServer(level, damageSources().inFire(), (float) config.FLAMER_DAMAGE);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientAnimations();
            clientEffects();
            return;
        }
        if (brain != null && isAlive() && !isNoAi()) {
            brain.tick();
            var target = brain.target();
            if (getTarget() != target) {
                setTarget(target);
            }
        }
        if (isAlive() && tickCount % HOVER_SOUND_INTERVAL == 0) {
            playSound(ADSounds.MISTER_HANDY_HOVER.get(), 0.4F, 0.9F + random.nextFloat() * 0.2F);
        }
        updateState();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        var motion = getDeltaMovement();
        if (isAlive() && !onGround() && motion.y < MAX_SINK) {
            setDeltaMovement(motion.x, MAX_SINK, motion.z);
        }
    }

    private void updateState() {
        MisterHandyState state;
        if (!isAlive()) {
            state = MisterHandyState.DEAD;
        } else {
            var dx = getX() - xo;
            var dz = getZ() - zo;
            if (dx * dx + dz * dz > MOVE_THRESHOLD_SQR) {
                state = MisterHandyState.MOVE;
            } else {
                state = entityData.get(DATA_SCANNING) ? MisterHandyState.SCAN : MisterHandyState.IDLE;
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
            MisterHandyAnimations.state(this, MisterHandyState.byIndex(state));
        }
        var actionId = entityData.get(DATA_ACTION_ID);
        if (actionId != clientActionId) {
            clientActionId = actionId;
            MisterHandyAnimations.action(this, MisterHandyAction.byIndex(entityData.get(DATA_ACTION)));
        }
    }

    private void clientEffects() {
        if (!isAlive()) {
            return;
        }
        var level = level();
        if (tickCount % 2 == 0) {
            level.addParticle(
                ParticleTypes.FLAME,
                getX() + (random.nextDouble() - 0.5) * 0.15,
                getY() + 0.45,
                getZ() + (random.nextDouble() - 0.5) * 0.15,
                0,
                -0.06,
                0
            );
        }
        if (getHealth() < getMaxHealth() * 0.3F && random.nextInt(6) == 0) {
            level.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                getY() + getBbHeight() * (0.4 + random.nextDouble() * 0.5),
                getZ() + (random.nextDouble() - 0.5) * getBbWidth(),
                (random.nextDouble() - 0.5) * 0.2,
                0.1,
                (random.nextDouble() - 0.5) * 0.2
            );
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        if (source.getEntity() != null && ally(source.getEntity())) {
            return false;
        }
        var hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive()) {
            if (!busy() && random.nextInt(3) == 0) {
                playAction(MisterHandyAction.HURT);
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
        playAction(MisterHandyAction.STOP);
        updateState();
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (level() instanceof ServerLevel server && deathTime % 6 == 0) {
            server.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                getX(),
                getY() + getBbHeight() * 0.6,
                getZ(),
                6,
                0.3,
                0.3,
                0.3,
                0.1
            );
        }
        if (deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.8, getZ(), 2, 0.3, 0.3, 0.3, 0);
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                playSound(ADSounds.MISTER_HANDY_EXPLODE.get(), 1.5F, 1.3F);
            }
            level().broadcastEntityEvent(this, EntityEvent.POOF);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance effect) {
        var holder = effect.getEffect();
        if (holder.is(MobEffects.POISON) || holder.is(MobEffects.WITHER) || holder.is(MobEffects.REGENERATION)) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull EntitySpawnReason reason,
        @Nullable SpawnGroupData data
    ) {
        home = blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
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
        home = input.getBooleanOr("HasHome", false)
            ? new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0))
            : null;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ADSounds.MISTER_HANDY_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return ADSounds.MISTER_HANDY_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return ADSounds.MISTER_HANDY_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {}

    @Override
    protected boolean considersEntityAsAlly(@NotNull Entity other) {
        return ally(other) || super.considersEntityAsAlly(other);
    }

    public float yawTo(Entity target) {
        return (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90F;
    }

    public void face(Entity target) {
        var yaw = Mth.approachDegrees(getYRot(), yawTo(target), 20F);
        setYRot(yaw);
        yBodyRot = yaw;
        setYHeadRot(yaw);
        getLookControl().setLookAt(target, 30F, 30F);
    }
}

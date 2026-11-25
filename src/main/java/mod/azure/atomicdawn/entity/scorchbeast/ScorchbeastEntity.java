package mod.azure.atomicdawn.entity.scorchbeast;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ai.ScorchbeastBrain;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedSpawning;
import mod.azure.atomicdawn.entity.scorched.ai.ScorchedBrain;
import mod.azure.atomicdawn.limb.LimbService;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.vats.BodyPart;

@SuppressWarnings({ "unused", "deprecation" })
public class ScorchbeastEntity extends Monster implements AzHitboxHolder {

    private static final EntityDataAccessor<Integer> DATA_FLIGHT = SynchedEntityData.defineId(
        ScorchbeastEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(
        ScorchbeastEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION_ID = SynchedEntityData.defineId(
        ScorchbeastEntity.class,
        EntityDataSerializers.INT
    );

    public static final int TAKEOFF_TICKS = 49;

    public static final int LAND_TICKS = 27;

    private static final int LIFT_START = 13;

    private static final int FLAP_INTERVAL = 36;

    private static final double MIN_CLEARANCE = 5.0;

    private static final double DEFAULT_AGILITY = 0.12;

    private static final double AIR_DRAG = 0.91;

    private static final double VERTICAL_DRAG = 0.98;

    private static final float MAX_TURN = 9F;

    private static final double WALK_START_SQR = 0.03 * 0.03;

    private static final double WALK_STOP_SQR = 0.012 * 0.012;

    private static final int WALK_STOP_DELAY = 6;

    @Nullable
    private final ScorchbeastBrain brain;

    @Nullable
    private BlockPos home;

    @Nullable
    private Vec3 flightGoal;

    private double flightSpeed;

    private double flightAgility = DEFAULT_AGILITY;

    private double clearance = MIN_CLEARANCE;

    @Nullable
    private BlockPos landingSpot;

    private boolean forcedDescent;

    @Nullable
    private Vec3 facePoint;

    private int flightTicks;

    private int busyUntil;

    private int clientFlight = -1;

    private boolean clientWalking;

    private int clientStillTicks;

    private int clientActionId;

    public ScorchbeastEntity(EntityType<? extends ScorchbeastEntity> type, Level level) {
        super(type, level);
        this.brain = level.isClientSide() ? null : new ScorchbeastBrain(this);
        this.xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = AtomicDawn.CONFIG.scorchbeast;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, config.WING_SWEEP_DAMAGE)
            .add(Attributes.ARMOR, config.ARMOR)
            .add(Attributes.ARMOR_TOUGHNESS, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.STEP_HEIGHT, 1.5)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    public static boolean ally(Entity entity) {
        return entity instanceof ScorchbeastEntity || entity instanceof ScorchedEntity || BuiltInRegistries.ENTITY_TYPE
            .wrapAsHolder(entity.getType())
            .is(ScorchedBrain.ALLIES);
    }

    public static boolean validTarget(LivingEntity candidate) {
        return !(candidate instanceof ScorchbeastEntity) && ScorchedBrain.validTarget(candidate);
    }

    @Override
    public AzHitboxSet getHitboxSet() {
        return ScorchbeastHitboxes.SET;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLIGHT, ScorchbeastFlight.GROUNDED.ordinal());
        builder.define(DATA_ACTION, ScorchbeastAction.NONE.ordinal());
        builder.define(DATA_ACTION_ID, 0);
    }

    @Nullable
    public ScorchbeastBrain brain() {
        return brain;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public ScorchbeastFlight flight() {
        return ScorchbeastFlight.byIndex(entityData.get(DATA_FLIGHT));
    }

    public int flightTicks() {
        return flightTicks;
    }

    public boolean flying() {
        return flight() == ScorchbeastFlight.FLYING;
    }

    public boolean grounded() {
        return flight() == ScorchbeastFlight.GROUNDED;
    }

    public boolean wingCrippled() {
        return LimbService.crippled(this, BodyPart.LEFT_ARM) || LimbService.crippled(this, BodyPart.RIGHT_ARM);
    }

    public boolean busy() {
        return tickCount < busyUntil;
    }

    public void lockFor(int ticks) {
        busyUntil = Math.max(busyUntil, tickCount + ticks);
    }

    public void playAction(ScorchbeastAction action) {
        entityData.set(DATA_ACTION, action.ordinal());
        entityData.set(DATA_ACTION_ID, entityData.get(DATA_ACTION_ID) + 1);
    }

    private void setFlight(ScorchbeastFlight next) {
        if (next.ordinal() == entityData.get(DATA_FLIGHT)) {
            return;
        }
        entityData.set(DATA_FLIGHT, next.ordinal());
        flightTicks = 0;
        var weightless = next == ScorchbeastFlight.TAKEOFF || next == ScorchbeastFlight.FLYING
            || next == ScorchbeastFlight.DESCENDING;
        setNoGravity(weightless);
        getNavigation().stop();
        if (next != ScorchbeastFlight.FLYING) {
            flightGoal = null;
        }
        if (brain != null) {
            brain.onFlightChanged(next);
        }
    }

    public boolean beginTakeoff() {
        if (!grounded() || wingCrippled() || !isAlive()) {
            return false;
        }
        setFlight(ScorchbeastFlight.TAKEOFF);
        playSound(ADSounds.SCORCHBEAST_SCREECH.get(), 3F, getVoicePitch());
        return true;
    }

    public void beginDescent(BlockPos spot, boolean forced) {
        landingSpot = spot;
        forcedDescent = forced;
        setFlight(ScorchbeastFlight.DESCENDING);
    }

    public boolean beginLunge(Vec3 velocity) {
        if (!grounded() || !onGround()) {
            return false;
        }
        setFlight(ScorchbeastFlight.LUNGING);
        setDeltaMovement(velocity);
        syncVelocity = true;
        playSound(ADSounds.SCORCHBEAST_FLAP.get(), 3F, 0.85F);
        return true;
    }

    public void spawnAirborne(BlockPos site) {
        home = site;
        setFlight(ScorchbeastFlight.FLYING);
        hover();
    }

    public void flyTo(Vec3 goal, double speed) {
        flyTo(goal, speed, DEFAULT_AGILITY);
    }

    public void flyTo(Vec3 goal, double speed, double agility) {
        flightGoal = goal;
        flightSpeed = speed;
        flightAgility = agility;
    }

    public void hover() {
        flyTo(position(), 0.08, DEFAULT_AGILITY);
    }

    public void allowLowFlight(double minimum) {
        clearance = Math.min(clearance, minimum);
    }

    public void faceTowards(@Nullable Vec3 point) {
        facePoint = point;
    }

    public double groundY(double x, double z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    public Vec3 mouth() {
        var forward = Vec3.directionFromRotation(getXRot(), getYHeadRot());
        return position().add(0, getBbHeight() * 0.75, 0).add(forward.scale(getBbWidth() * 0.9));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientAnimations();
            return;
        }
        flightTicks++;
        if (isAlive()) {
            if (brain != null && !isNoAi()) {
                brain.tick();
                var target = brain.target();
                if (getTarget() != target) {
                    setTarget(target);
                }
            }
            updateFlight();
            if (flight().airborne() && tickCount % FLAP_INTERVAL == 0) {
                playSound(ADSounds.SCORCHBEAST_FLAP.get(), 3.5F, 0.85F + random.nextFloat() * 0.15F);
            }
        }
        facePoint = null;
        clearance = MIN_CLEARANCE;
    }

    private void updateFlight() {
        if (wingCrippled() && flight() == ScorchbeastFlight.TAKEOFF) {
            beginDescent(BlockPos.containing(getX(), groundY(getX(), getZ()), getZ()), true);
        }
        switch (flight()) {
            case TAKEOFF -> {
                if (flightTicks >= LIFT_START) {
                    var lift = Math.min(0.32, 0.06 + (flightTicks - LIFT_START) * 0.012);
                    setDeltaMovement(getDeltaMovement().multiply(0.8, 0, 0.8).add(0, lift, 0));
                }
                if (flightTicks >= TAKEOFF_TICKS) {
                    setFlight(ScorchbeastFlight.FLYING);
                    hover();
                }
            }
            case FLYING -> {
                if (flightGoal == null) {
                    hover();
                }
                steer(flightGoal, flightSpeed, flightAgility);
                keepClearance();
                orient();
            }
            case DESCENDING -> descend();
            case LANDING -> {
                setDeltaMovement(getDeltaMovement().multiply(0.4, 1.0, 0.4));
                if (flightTicks >= LAND_TICKS) {
                    setFlight(ScorchbeastFlight.GROUNDED);
                }
            }
            case LUNGING -> {
                orient();
                if ((onGround() && flightTicks > 6) || flightTicks > 50) {
                    setFlight(ScorchbeastFlight.GROUNDED);
                }
            }
            case GROUNDED -> {
                if (!onGround() && flightTicks > 40 && !wingCrippled() && fallDistance > 8) {
                    setFlight(ScorchbeastFlight.FLYING);
                    hover();
                }
                if (facePoint != null) {
                    orient();
                }
            }
            default -> {}
        }
        if (flight().airborne()) {
            fallDistance = 0;
        }
    }

    private void descend() {
        var spot = landingSpot;
        if (spot == null) {
            spot = BlockPos.containing(getX(), groundY(getX(), getZ()), getZ());
            landingSpot = spot;
        }
        var target = Vec3.atBottomCenterOf(spot);
        var dx = target.x - getX();
        var dz = target.z - getZ();
        var horizontal = Math.sqrt(dx * dx + dz * dz);
        var sink = forcedDescent ? 0.3 : 0.2;
        Vec3 goal;
        if (horizontal > 3.0 && !forcedDescent) {
            var altitude = Math.max(target.y + 6.0, getY() - sink);
            goal = new Vec3(target.x, altitude, target.z);
            steer(goal, 0.6, 0.15);
        } else {
            goal = new Vec3(target.x, getY() - 4.0, target.z);
            steer(goal, forcedDescent ? 0.45 : 0.3, 0.2);
            var motion = getDeltaMovement();
            setDeltaMovement(motion.x, Math.min(motion.y, -sink), motion.z);
        }
        orient();
        if (onGround() || getY() - groundY(getX(), getZ()) < 0.4 || flightTicks > 600) {
            setNoGravity(false);
            setFlight(ScorchbeastFlight.LANDING);
            playSound(ADSounds.SCORCHBEAST_LAND.get(), 3F, 0.8F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 20, 2.0, 0.1, 2.0, 0.05);
            }
        }
    }

    private void steer(@Nullable Vec3 goal, double speed, double agility) {
        if (goal == null) {
            return;
        }
        var offset = goal.subtract(position());
        var distance = offset.length();
        var desired = distance < 0.3 ? Vec3.ZERO : offset.scale(Math.min(speed, distance * 0.2 + 0.02) / distance);
        var motion = getDeltaMovement().multiply(1.0 / AIR_DRAG, 1.0 / VERTICAL_DRAG, 1.0 / AIR_DRAG);
        setDeltaMovement(motion.add(desired.subtract(motion).scale(agility)));
    }

    private void keepClearance() {
        var height = getY() - groundY(getX(), getZ());
        if (height < clearance || horizontalCollision) {
            var motion = getDeltaMovement();
            setDeltaMovement(motion.x, Math.max(motion.y, horizontalCollision ? 0.35 : 0.18), motion.z);
        }
    }

    private void orient() {
        var motion = getDeltaMovement();
        float yaw;
        if (facePoint != null) {
            yaw = (float) (Mth.atan2(facePoint.z - getZ(), facePoint.x - getX()) * Mth.RAD_TO_DEG) - 90F;
        } else if (motion.horizontalDistanceSqr() > 0.004) {
            yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90F;
        } else {
            return;
        }
        var turned = Mth.approachDegrees(getYRot(), yaw, MAX_TURN);
        setYRot(turned);
        yBodyRot = turned;
        setYHeadRot(turned);
    }

    public boolean facing(Vec3 point, float tolerance) {
        var yaw = (float) (Mth.atan2(point.z - getZ(), point.x - getX()) * Mth.RAD_TO_DEG) - 90F;
        return Math.abs(Mth.wrapDegrees(yaw - getYRot())) <= tolerance;
    }

    private void clientAnimations() {
        int flight = entityData.get(DATA_FLIGHT);
        var grounded = flight == ScorchbeastFlight.GROUNDED.ordinal() && isAlive();
        var dx = getX() - xo;
        var dz = getZ() - zo;
        var speed = dx * dx + dz * dz;
        var walking = clientWalking;
        if (!grounded) {
            walking = false;
            clientStillTicks = 0;
        } else if (speed > WALK_START_SQR) {
            walking = true;
            clientStillTicks = 0;
        } else if (speed < WALK_STOP_SQR && ++clientStillTicks >= WALK_STOP_DELAY) {
            walking = false;
        }
        if (flight != clientFlight || walking != clientWalking) {
            clientFlight = flight;
            clientWalking = walking;
            if (walking) {
                ScorchbeastAnimations.walk(this);
            } else {
                ScorchbeastAnimations.flight(this, ScorchbeastFlight.byIndex(flight));
            }
        }
        var actionId = entityData.get(DATA_ACTION_ID);
        if (actionId != clientActionId) {
            clientActionId = actionId;
            ScorchbeastAnimations.action(this, ScorchbeastAction.byIndex(entityData.get(DATA_ACTION)));
        }
    }

    public void sweep(ServerLevel level, boolean left) {
        var config = AtomicDawn.CONFIG.scorchbeast;
        var reach = config.WING_SWEEP_RANGE + getBbWidth() * 0.5;
        var forward = Vec3.directionFromRotation(0F, getYRot());
        var box = getBoundingBox().inflate(reach, 1.0, reach);
        var hit = false;
        for (
            var victim : level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                living -> living != this && living.isAlive() && !ally(living)
            )
        ) {
            var offset = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ());
            var distance = offset.length() - victim.getBbWidth() * 0.5;
            if (distance > reach) {
                continue;
            }
            var angle = Math.toDegrees(Math.acos(Mth.clamp(offset.normalize().dot(forward), -1.0, 1.0)));
            if (distance > getBbWidth() * 0.6 && angle > config.WING_SWEEP_ARC * 0.5) {
                continue;
            }
            if (victim.hurtServer(level, damageSources().mobAttack(this), (float) config.WING_SWEEP_DAMAGE)) {
                hit = true;
                var side = forward.yRot((float) Math.toRadians(left ? 90 : -90));
                var push = offset.normalize()
                    .scale(0.7)
                    .add(side.scale(0.8))
                    .normalize()
                    .scale(config.WING_SWEEP_KNOCKBACK);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.45, push.z));
                victim.syncVelocity = true;
            }
        }
        playSound(ADSounds.SCORCHBEAST_SWEEP.get(), 2.5F, hit ? 0.8F : 0.95F);
    }

    public void shockwave(ServerLevel level) {
        var config = AtomicDawn.CONFIG.scorchbeast;
        var radius = config.SHOCKWAVE_RADIUS;
        var center = position();
        for (
            var victim : level.getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(radius, 3.0, radius),
                living -> living != this && living.isAlive() && !ally(living)
            )
        ) {
            var distance = victim.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            var falloff = (float) Mth.clamp(1.0 - distance / (radius * 1.25), 0.3, 1.0);
            if (victim.hurtServer(level, damageSources().sonicBoom(this), (float) config.SHOCKWAVE_DAMAGE * falloff)) {
                var push = victim.position().subtract(center).multiply(1, 0, 1).normalize().scale(1.4 * falloff);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.5 * falloff, push.z));
                victim.syncVelocity = true;
            }
        }
        for (var i = 0; i < 12; i++) {
            var angle = i * Mth.TWO_PI / 12;
            var ring = radius * 0.6;
            level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                getX() + Mth.cos(angle) * ring,
                getY() + 1.0,
                getZ() + Mth.sin(angle) * ring,
                1,
                0,
                0,
                0,
                0
            );
        }
        playSound(ADSounds.SCORCHBEAST_SHOCKWAVE.get(), 4F, 0.8F);
    }

    public void lungeContact(ServerLevel level, LivingEntity target) {
        var config = AtomicDawn.CONFIG.scorchbeast;
        if (target.hurtServer(level, damageSources().mobAttack(this), (float) config.LUNGE_DAMAGE)) {
            var push = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.2);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.6, push.z));
            target.syncVelocity = true;
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        if (source.getEntity() != null && ally(source.getEntity())) {
            return false;
        }
        var hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive() && brain != null && source.getEntity() instanceof LivingEntity attacker) {
            brain.onHurt(attacker, level.getGameTime());
        }
        return hurt;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        setNoGravity(false);
        entityData.set(DATA_FLIGHT, ScorchbeastFlight.DEAD.ordinal());
        playAction(ScorchbeastAction.STOP);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull EntitySpawnReason reason,
        @Nullable SpawnGroupData data
    ) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            home = ScorchedSpawning.fissureCenter(level.getLevel(), blockPosition()).orElse(blockPosition());
        } else {
            home = blockPosition();
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Flight", flight() == ScorchbeastFlight.GROUNDED ? 0 : 1);
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
        if (input.getIntOr("Flight", 0) == 1) {
            setFlight(ScorchbeastFlight.FLYING);
        }
        home = input.getBooleanOr("HasHome", false)
            ? new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0))
            : null;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F + (random.nextFloat() - random.nextFloat()) * 0.08F;
    }

    @Override
    protected float getSoundVolume() {
        return 3F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ADSounds.SCORCHBEAST_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return ADSounds.SCORCHBEAST_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return ADSounds.SCORCHBEAST_DEATH.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        playSound(ADSounds.SCORCHBEAST_STEP.get(), 1.0F, 0.8F);
    }

    @Override
    protected boolean considersEntityAsAlly(@NotNull Entity other) {
        return ally(other) || super.considersEntityAsAlly(other);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return home == null && super.removeWhenFarAway(distance);
    }

    public AABB contactBox() {
        return getBoundingBox().inflate(1.0);
    }
}

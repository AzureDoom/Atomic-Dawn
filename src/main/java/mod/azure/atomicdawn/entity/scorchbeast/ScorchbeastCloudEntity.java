package mod.azure.atomicdawn.entity.scorchbeast;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.helper.ADSerialization;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADEntities;

public class ScorchbeastCloudEntity extends Entity {

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(
        ScorchbeastCloudEntity.class,
        EntityDataSerializers.FLOAT
    );

    private static final DustParticleOptions GREEN = new DustParticleOptions(0x9BE84A, 2.0F);

    private static final DustParticleOptions YELLOW = new DustParticleOptions(0xD8D24A, 1.6F);

    private static final int PULSE_TICKS = 10;

    private int remainingTicks;

    public ScorchbeastCloudEntity(EntityType<? extends ScorchbeastCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static ScorchbeastCloudEntity create(ServerLevel level, double x, double y, double z) {
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.cropDusting;
        var cloud = new ScorchbeastCloudEntity(ADEntities.SCORCHBEAST_CLOUD.get(), level);
        cloud.setPos(x, y, z);
        cloud.entityData.set(RADIUS, (float) config.DUST_CLOUD_RADIUS);
        cloud.remainingTicks = config.DUST_CLOUD_SECONDS * 20;
        return cloud;
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(RADIUS, 4F);
    }

    @Override
    public void tick() {
        super.tick();
        var level = level();
        var radius = radius();
        if (level.isClientSide()) {
            particles(level, radius);
            return;
        }
        setPos(getX(), getY() - 0.04, getZ());
        if (--remainingTicks <= 0) {
            discard();
            return;
        }
        if (tickCount % PULSE_TICKS != 0) {
            return;
        }
        var rate = (float) AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.cropDusting.DUST_RADS_PER_SECOND
            * PULSE_TICKS / 20F;
        var area = new AABB(position(), position()).inflate(radius, radius * 1.5, radius);
        for (
            var living : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> candidate.isAlive() && !ScorchbeastEntity.ally(candidate)
            )
        ) {
            var dx = living.getX() - getX();
            var dz = living.getZ() - getZ();
            var horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal <= radius) {
                RadiationService.expose(living, rate * (1F - 0.4F * (float) (horizontal / radius)));
            }
        }
    }

    private void particles(Level level, float radius) {
        var random = level.getRandom();
        var count = Math.max(2, (int) radius);
        for (var i = 0; i < count; i++) {
            var angle = random.nextDouble() * Math.PI * 2;
            var distance = Math.sqrt(random.nextDouble()) * radius;
            var x = getX() + Math.cos(angle) * distance;
            var z = getZ() + Math.sin(angle) * distance;
            var y = getY() + (random.nextDouble() - 0.5) * radius;
            level.addParticle(random.nextInt(3) == 0 ? YELLOW : GREEN, x, y, z, 0, -0.03, 0);
            if (random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, -0.05, 0);
            }
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        entityData.set(RADIUS, ADSerialization.getFloat(input, "Radius", 4F));
        remainingTicks = ADSerialization.getInt(input, "Remaining", 0);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        output.putFloat("Radius", radius());
        output.putInt("Remaining", remainingTicks);
    }
}

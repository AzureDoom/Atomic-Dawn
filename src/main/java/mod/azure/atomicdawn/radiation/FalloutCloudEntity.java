package mod.azure.atomicdawn.radiation;

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
import mod.azure.atomicdawn.registry.ADEntities;

public class FalloutCloudEntity extends Entity {

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(
        FalloutCloudEntity.class,
        EntityDataSerializers.FLOAT
    );

    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(0x7BE04A, 1.6F);

    private int remainingTicks;

    public FalloutCloudEntity(EntityType<? extends FalloutCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static FalloutCloudEntity create(ServerLevel level, double x, double y, double z, float radius, int ticks) {
        var cloud = new FalloutCloudEntity(ADEntities.FALLOUT_CLOUD.get(), level);
        cloud.setPos(x, y, z);
        cloud.setRadius(radius);
        cloud.remainingTicks = ticks;
        return cloud;
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        entityData.set(RADIUS, radius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(RADIUS, 8F);
    }

    @Override
    public void tick() {
        super.tick();
        var level = level();
        var radius = radius();
        if (level.isClientSide()) {
            clientParticles(level, radius);
            return;
        }
        if (--remainingTicks <= 0) {
            discard();
            return;
        }
        if (tickCount % 20 != 0) {
            return;
        }
        var rate = (float) AtomicDawn.CONFIG.radiation.FALLOUT_RADS_PER_SECOND;
        var area = new AABB(position(), position()).inflate(radius, radius * 0.75, radius);
        for (var living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            var distance = living.position().distanceTo(position());
            if (distance <= radius) {
                RadiationService.expose(living, rate * (1F - 0.5F * (float) (distance / radius)));
            }
        }
    }

    private void clientParticles(Level level, float radius) {
        var random = level.getRandom();
        var count = Math.max(1, (int) (radius / 4));
        for (var i = 0; i < count; i++) {
            var angle = random.nextDouble() * Math.PI * 2;
            var distance = Math.sqrt(random.nextDouble()) * radius;
            var x = getX() + Math.cos(angle) * distance;
            var z = getZ() + Math.sin(angle) * distance;
            var y = getY() + random.nextDouble() * radius * 0.6;
            level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, -0.02, 0);
            if (random.nextInt(4) == 0) {
                level.addParticle(GREEN_DUST, x, y, z, 0, -0.01, 0);
            }
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        setRadius(input.getFloatOr("Radius", 8F));
        remainingTicks = input.getIntOr("Remaining", 0);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        output.putFloat("Radius", radius());
        output.putInt("Remaining", remainingTicks);
    }
}

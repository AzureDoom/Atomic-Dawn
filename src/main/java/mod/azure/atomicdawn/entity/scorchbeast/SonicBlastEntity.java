package mod.azure.atomicdawn.entity.scorchbeast;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADEntities;
import mod.azure.atomicdawn.registry.ADSounds;

public class SonicBlastEntity extends ThrowableProjectile {

    private static final DustParticleOptions GLOW = new DustParticleOptions(0xFF9A3C, 2.2F);

    private static final int LIFETIME = 80;

    private boolean detonated;

    public SonicBlastEntity(EntityType<? extends SonicBlastEntity> type, Level level) {
        super(type, level);
    }

    public static SonicBlastEntity create(ServerLevel level, LivingEntity owner, Vec3 origin, Vec3 velocity) {
        var blast = new SonicBlastEntity(ADEntities.SONIC_BLAST.get(), level);
        blast.setOwner(owner);
        blast.setPos(origin.x, origin.y, origin.z);
        blast.setDeltaMovement(velocity);
        return blast;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NonNull Builder entityData) {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            var random = level().getRandom();
            for (var i = 0; i < 3; i++) {
                level().addParticle(
                    GLOW,
                    getX() + (random.nextDouble() - 0.5) * 0.6,
                    getY() + (random.nextDouble() - 0.5) * 0.6,
                    getZ() + (random.nextDouble() - 0.5) * 0.6,
                    0,
                    0,
                    0
                );
            }
            if (tickCount % 4 == 0) {
                level().addParticle(ParticleTypes.SONIC_BOOM, getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        if (tickCount >= LIFETIME && level() instanceof ServerLevel level) {
            detonate(level);
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity target) {
        return super.canHitEntity(target) && !ScorchbeastEntity.ally(target);
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            detonate(level);
        }
    }

    private void detonate(ServerLevel level) {
        if (detonated) {
            return;
        }
        detonated = true;
        var config = AtomicDawn.CONFIG.scorchbeast;
        var radius = config.SONIC_BLAST_RADIUS;
        var damage = (float) config.SONIC_BLAST_DAMAGE;
        var center = position();
        var owner = getOwner();
        var source = damageSources().explosion(this, owner);
        for (
            var victim : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(radius),
                living -> living.isAlive() && living != owner && !ScorchbeastEntity.ally(living)
            )
        ) {
            var distance = victim.position().add(0, victim.getBbHeight() * 0.5, 0).distanceTo(center);
            if (distance > radius) {
                continue;
            }
            var falloff = (float) Mth.clamp(1.0 - distance / radius, 0.25, 1.0);
            if (victim.hurtServer(level, source, damage * falloff)) {
                var push = victim.position().subtract(center).normalize().scale(0.9 * falloff);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.3 * falloff, push.z));
                victim.syncVelocity = true;
            }
        }
        if (config.SONIC_BREAKS_BLOCKS) {
            level.explode(this, center.x, center.y, center.z, 1.5F, Level.ExplosionInteraction.MOB);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 0.6, 0.6, 0.6, 0.0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(
            null,
            center.x,
            center.y,
            center.z,
            ADSounds.SCORCHBEAST_SONIC_IMPACT.get(),
            getSoundSource(),
            3F,
            0.9F
        );
        discard();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Detonated", detonated);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        detonated = input.getBooleanOr("Detonated", false);
    }
}

package mod.azure.atomicdawn.weapon.combat;

import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import mod.azure.azurelib.hitbox.AzHitboxes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.weapon.action.ShooterState;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public final class HitscanResolver {

    private HitscanResolver() {}

    public static boolean fire(
        ServerLevel level,
        ServerPlayer shooter,
        ItemStack weapon,
        WeaponDefinition definition,
        ShooterState state,
        float coneDegrees,
        float damageMultiplier
    ) {
        var damage = definition.damage();
        var eye = shooter.getEyePosition();
        var forward = shooter.getViewVector(1F);
        var random = shooter.getRandom();
        var hitAnything = false;

        var pending = new Object2FloatLinkedOpenHashMap<Entity>();
        Map<Entity, Vec3> hitPositions = new HashMap<>();
        var headshots = new HashSet<Entity>();

        for (var pellet = 0; pellet < damage.pellets(); pellet++) {
            var direction = SpreadModel.randomDirection(forward, coneDegrees, random);
            var maxEnd = eye.add(direction.scale(damage.range()));

            var blockHit = level.clip(
                new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)
            );
            var end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();

            var searchBox = shooter.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
            var entityHit = ProjectileUtil.getEntityHitResult(
                shooter,
                eye,
                end,
                searchBox,
                target -> canHit(shooter, target),
                eye.distanceToSqr(end)
            );

            var tracerEnd = end;
            if (entityHit != null) {
                var target = entityHit.getEntity();
                var hitPos = entityHit.getLocation();
                var amount = damage.base() * damage.falloffMultiplier(eye.distanceTo(hitPos)) * damageMultiplier;
                if (isVanillaHeadshot(target, hitPos)) {
                    amount *= damage.headshotMultiplier();
                    headshots.add(target);
                }
                pending.addTo(target, amount);
                hitPositions.putIfAbsent(target, hitPos);
                impactEntity(level, hitPos);
                tracerEnd = hitPos;
                hitAnything = true;
            } else if (blockHit.getType() == HitResult.Type.BLOCK) {
                impactBlock(level, blockHit.getLocation(), level.getBlockState(blockHit.getBlockPos()));
                hitAnything = true;
            }

            var beamEnd = tracerEnd;
            definition.tracer().ifPresent(tracer -> drawTracer(level, shooter, beamEnd, tracer));
        }

        for (var entry : pending.object2FloatEntrySet()) {
            var target = entry.getKey();
            DamageApplier.apply(
                level,
                shooter,
                weapon,
                definition,
                state,
                target,
                hitPositions.get(target),
                entry.getFloatValue()
            );
            if (headshots.contains(target) && AtomicDawn.CONFIG.weapons.IMPACT_PARTICLES) {
                level.sendParticles(
                    ParticleTypes.CRIT,
                    target.getX(),
                    target.getEyeY(),
                    target.getZ(),
                    6,
                    0.15,
                    0.1,
                    0.15,
                    0.1
                );
            }
        }
        return hitAnything;
    }

    private static void drawTracer(ServerLevel level, ServerPlayer shooter, Vec3 end, WeaponDefinition.Tracer tracer) {
        var start = Muzzle.position(shooter);
        var path = end.subtract(start);
        var length = path.length();
        if (length < 0.1) {
            return;
        }
        var points = (int) Math.min(48, Math.ceil(length / 0.5D));
        var step = path.scale(1.0 / points);
        var particle = new DustParticleOptions(tracer.color(), tracer.size());
        for (var i = 0; i <= points; i++) {
            var p = start.add(step.scale(i));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static boolean canHit(ServerPlayer shooter, Entity target) {
        return target != shooter
            && target.isPickable()
            && !target.isSpectator()
            && target.getRootVehicle() != shooter.getRootVehicle();
    }

    private static boolean isVanillaHeadshot(Entity target, Vec3 hitPos) {
        if (!(target instanceof LivingEntity living) || !AzHitboxes.getActiveHitboxes(target).isEmpty()) {
            return false;
        }
        var headBand = Math.max(0.15, living.getBbHeight() * 0.12);
        return hitPos.y >= living.getEyeY() - headBand;
    }

    private static void impactBlock(ServerLevel level, Vec3 pos, BlockState state) {
        if (!AtomicDawn.CONFIG.weapons.IMPACT_PARTICLES || state.isAir()) {
            return;
        }
        var particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        level.sendParticles(particle, pos.x, pos.y, pos.z, 6, 0.05, 0.05, 0.05, 0.1);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 2, 0.02, 0.02, 0.02, 0.01);
    }

    private static void impactEntity(ServerLevel level, Vec3 pos) {
        if (AtomicDawn.CONFIG.weapons.IMPACT_PARTICLES) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}

package mod.azure.atomicdawn.weapon.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.weapon.action.ShooterState;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public final class MeleeResolver {

    private MeleeResolver() {}

    public static boolean swing(
        ServerLevel level,
        ServerPlayer attacker,
        ItemStack weapon,
        WeaponDefinition definition,
        ShooterState shooter,
        float damageMultiplier
    ) {
        var melee = definition.melee().orElseThrow();
        var eye = attacker.getEyePosition();
        var look = attacker.getViewVector(1F);
        var maxEnd = eye.add(look.scale(melee.reach()));

        var blockHit = level.clip(
            new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker)
        );
        var end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        var searchBox = attacker.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        var hit = ProjectileUtil.getEntityHitResult(
            attacker,
            eye,
            end,
            searchBox,
            target -> target != attacker && target.isPickable() && !target.isSpectator(),
            eye.distanceToSqr(end)
        );
        if (hit == null) {
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                definition.sounds()
                    .impact()
                    .ifPresent(
                        sound -> level.playSound(
                            null,
                            blockHit.getLocation().x,
                            blockHit.getLocation().y,
                            blockHit.getLocation().z,
                            sound.value(),
                            SoundSource.PLAYERS,
                            0.6F,
                            1.2F
                        )
                    );
            }
            return false;
        }

        var target = hit.getEntity();
        var amount = definition.damage().base() * damageMultiplier;
        DamageApplier.apply(level, attacker, weapon, definition, shooter, target, hit.getLocation(), amount);
        knockback(attacker, target, melee.knockback() * Math.max(1F, damageMultiplier));

        if (melee.sweepRadius() > 0F) {
            var area = new AABB(hit.getLocation(), hit.getLocation()).inflate(melee.sweepRadius());
            for (
                var other : level.getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    e -> e != attacker && e != target && e != attacker.getVehicle()
                )
            ) {
                DamageApplier.apply(
                    level,
                    attacker,
                    weapon,
                    definition,
                    shooter,
                    other,
                    other.position(),
                    amount * melee.sweepDamageMultiplier()
                );
                knockback(attacker, other, melee.knockback() * 0.5F);
            }
            level.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                1,
                0,
                0,
                0,
                0
            );
        }

        definition.sounds()
            .impact()
            .ifPresent(
                sound -> level.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    sound.value(),
                    SoundSource.PLAYERS,
                    1F,
                    1F
                )
            );
        if (AtomicDawn.CONFIG.common.combat.weapons.IMPACT_PARTICLES) {
            level.sendParticles(
                ParticleTypes.CRIT,
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                8,
                0.2,
                0.2,
                0.2,
                0.2
            );
        }
        return true;
    }

    private static void knockback(ServerPlayer attacker, Entity target, float strength) {
        if (strength > 0F && target instanceof LivingEntity living) {
            var yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
            living.knockback(
                strength,
                Mth.sin(yaw),
                -Mth.cos(yaw),
                living.damageSources().mobAttack(attacker),
                0.0F
            );
        }
    }
}

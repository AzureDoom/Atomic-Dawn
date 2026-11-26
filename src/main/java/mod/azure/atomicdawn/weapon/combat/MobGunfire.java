package mod.azure.atomicdawn.weapon.combat;

import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import mod.azure.azurelib.hitbox.AzHitboxes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantDisturbances;
import mod.azure.atomicdawn.limb.LimbHits;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.weapon.action.WeaponAnimations;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class MobGunfire {

    private static final float FIRE_VOLUME = 4F;

    private static final int FALLBACK_TRACER_COLOR = 0xFFE08A;

    private MobGunfire() {}

    public static boolean canFire(WeaponDefinition definition) {
        return !definition.isMelee() && !definition.isProjectile() && !definition.category().melee();
    }

    public static int loaded(ItemStack stack, WeaponDefinition definition) {
        return definition.usesAmmo() ? Weapons.state(stack, definition).loadedAmmo() : Integer.MAX_VALUE;
    }

    public static boolean needsReload(ItemStack stack, WeaponDefinition definition) {
        return definition.usesAmmo() && loaded(stack, definition) < definition.ammo().perShot();
    }

    public static int reloadTicks(ItemStack stack, WeaponDefinition definition) {
        var missing = Math.max(1, definition.ammo().magazineSize() - loaded(stack, definition));
        return definition.reload().estimateTicks(missing, loaded(stack, definition) <= 0);
    }

    public static void startReload(Mob shooter, ItemStack stack, WeaponDefinition definition) {
        WeaponAnimations.reload(shooter, stack, loaded(stack, definition) <= 0);
        definition.sounds()
            .reload()
            .ifPresent(
                sound -> shooter.level()
                    .playSound(
                        null,
                        shooter.getX(),
                        shooter.getEyeY(),
                        shooter.getZ(),
                        sound.value(),
                        SoundSource.HOSTILE,
                        1F,
                        1F
                    )
            );
    }

    public static void finishReload(ItemStack stack, WeaponDefinition definition) {
        if (definition.usesAmmo()) {
            Weapons.setState(stack, Weapons.state(stack, definition).withLoadedAmmo(definition.ammo().magazineSize()));
        }
    }

    public static boolean fire(
        ServerLevel level,
        Mob shooter,
        ItemStack stack,
        WeaponDefinition definition,
        LivingEntity target,
        float extraSpread,
        float damageMultiplier,
        Predicate<Entity> canHit
    ) {
        if (!canFire(definition) || needsReload(stack, definition)) {
            return false;
        }
        if (definition.usesAmmo()) {
            var state = Weapons.state(stack, definition);
            Weapons.setState(stack, state.withLoadedAmmo(state.loadedAmmo() - definition.ammo().perShot()));
        }
        var damage = definition.damage();
        var eye = shooter.getEyePosition();
        var aim = target.getBoundingBox().getCenter().add(0, target.getBbHeight() * 0.15, 0);
        var forward = aim.subtract(eye).normalize();
        var cone = definition.accuracy().baseSpread() + extraSpread;
        var random = shooter.getRandom();
        var pending = new Object2FloatLinkedOpenHashMap<Entity>();
        Map<Entity, Vec3> hitPositions = new HashMap<>();
        var muzzle = muzzle(shooter, forward);
        for (var pellet = 0; pellet < damage.pellets(); pellet++) {
            var direction = SpreadModel.randomDirection(forward, cone, random);
            var maxEnd = eye.add(direction.scale(damage.range()));
            var blockHit = level.clip(
                new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)
            );
            var end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
            var box = shooter.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
            var hit = ProjectileUtil.getEntityHitResult(
                shooter,
                eye,
                end,
                box,
                entity -> entity != shooter && entity.isPickable() && !entity.isSpectator() && canHit.test(entity),
                eye.distanceToSqr(end)
            );
            if (hit != null) {
                var hitPos = hit.getLocation();
                var amount = damage.base() * damage.falloffMultiplier(eye.distanceTo(hitPos));
                if (isVanillaHeadshot(hit.getEntity(), hitPos)) {
                    amount *= damage.headshotMultiplier();
                }
                pending.addTo(hit.getEntity(), amount);
                hitPositions.putIfAbsent(hit.getEntity(), hitPos);
                end = hitPos;
            } else if (
                blockHit.getType() == HitResult.Type.BLOCK && AtomicDawn.CONFIG.common.combat.weapons.IMPACT_PARTICLES
            ) {
                var state = level.getBlockState(blockHit.getBlockPos());
                if (!state.isAir()) {
                    level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        end.x,
                        end.y,
                        end.z,
                        4,
                        0.05,
                        0.05,
                        0.05,
                        0.1
                    );
                }
            }
            tracer(level, definition, muzzle, end);
        }
        var multiplier = (float) AtomicDawn.CONFIG.common.combat.weapons.DAMAGE_MULTIPLIER * damageMultiplier;
        for (var entry : pending.object2FloatEntrySet()) {
            var entity = entry.getKey();
            if (entity instanceof LivingEntity living) {
                living.setInvulnerableTime(0);
            }
            var type = ADDamageTypes.forTarget(damage.type(), entity);
            LimbHits.begin(entity, null, hitPositions.get(entity));
            try {
                entity.hurtServer(
                    level,
                    ADDamageTypes.source(level, type, shooter),
                    entry.getFloatValue() * multiplier
                );
            } finally {
                LimbHits.end();
            }
        }
        level.playSound(
            null,
            shooter.getX(),
            shooter.getEyeY(),
            shooter.getZ(),
            definition.sounds().fire().value(),
            SoundSource.HOSTILE,
            FIRE_VOLUME,
            1F + (random.nextFloat() - 0.5F) * 0.08F
        );
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.0);
        WeaponAnimations.fire(shooter, stack, false);
        SuperMutantDisturbances.gunfire(level, shooter);
        return true;
    }

    private static Vec3 muzzle(Mob shooter, Vec3 forward) {
        var right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? Vec3.ZERO : right.normalize();
        return shooter.getEyePosition().add(forward.scale(0.8)).add(right.scale(0.3)).add(0, -0.2, 0);
    }

    private static void tracer(ServerLevel level, WeaponDefinition definition, Vec3 from, Vec3 to) {
        var delta = to.subtract(from);
        var length = delta.length();
        if (length < 1.0) {
            return;
        }
        var particle = definition.tracer()
            .map(tracer -> new DustParticleOptions(tracer.color(), tracer.size()))
            .orElseGet(() -> new DustParticleOptions(FALLBACK_TRACER_COLOR, 0.5F));
        var points = (int) Math.min(48, Math.ceil(length / 1.5));
        var step = delta.scale(1.0 / points);
        for (var i = 1; i < points; i++) {
            var point = from.add(step.scale(i));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static boolean isVanillaHeadshot(Entity target, Vec3 hitPos) {
        if (!(target instanceof LivingEntity living) || !AzHitboxes.getActiveHitboxes(target).isEmpty()) {
            return false;
        }
        var headBand = Math.max(0.15, living.getBbHeight() * 0.12);
        return hitPos.y >= living.getEyeY() - headBand;
    }
}

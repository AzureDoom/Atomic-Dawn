package mod.azure.atomicdawn.vats;

import mod.azure.azurelib.hitbox.AzHitboxes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.limb.LimbProfiles;
import mod.azure.atomicdawn.limb.LimbService;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

@SuppressWarnings("unused")
public final class VatsTargeting {

    private VatsTargeting() {}

    public static Map<BodyPart, PartPoint> parts(Entity target) {
        var parts = new EnumMap<BodyPart, PartPoint>(BodyPart.class);

        for (var hit : AzHitboxes.getActiveHitboxes(target)) {
            var hitbox = hit.hitbox();
            var part = LimbProfiles.classify(target, hitbox.name(), hitbox.bone(), hitbox::hasTag);
            if (part != null && !parts.containsKey(part)) {
                parts.put(
                    part,
                    new PartPoint(part, hit.location(), hitbox.damageMultiplier(), part.hitChanceMultiplier(), true)
                );
            }
        }

        var box = target.getBoundingBox();
        var height = box.getYsize();
        var width = Math.max(box.getXsize(), box.getZsize());
        var center = box.getCenter();
        var yaw = (target instanceof LivingEntity living ? living.yBodyRot : target.getYRot()) * Mth.DEG_TO_RAD;
        var left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        var headY = target instanceof LivingEntity ? target.getEyeY() : box.minY + height * 0.85;

        fallback(parts, BodyPart.HEAD, new Vec3(center.x, headY, center.z));
        fallback(parts, BodyPart.TORSO, new Vec3(center.x, box.minY + height * 0.6, center.z));
        fallback(
            parts,
            BodyPart.LEFT_ARM,
            new Vec3(center.x, box.minY + height * 0.6, center.z).add(left.scale(width * 0.45))
        );
        fallback(
            parts,
            BodyPart.RIGHT_ARM,
            new Vec3(center.x, box.minY + height * 0.6, center.z).add(left.scale(-width * 0.45))
        );
        fallback(
            parts,
            BodyPart.LEFT_LEG,
            new Vec3(center.x, box.minY + height * 0.2, center.z).add(left.scale(width * 0.25))
        );
        fallback(
            parts,
            BodyPart.RIGHT_LEG,
            new Vec3(center.x, box.minY + height * 0.2, center.z).add(left.scale(-width * 0.25))
        );
        return parts;
    }

    private static void fallback(Map<BodyPart, PartPoint> parts, BodyPart part, Vec3 position) {
        parts.putIfAbsent(
            part,
            new PartPoint(part, position, part.damageMultiplier(), part.hitChanceMultiplier(), false)
        );
    }

    public static float hitboxMultiplierAt(Entity target, Vec3 position) {
        var best = 1F;
        var bestDistance = Double.MAX_VALUE;
        for (var hit : AzHitboxes.getActiveHitboxes(target)) {
            var distance = hit.location().distanceToSqr(position);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = hit.hitbox().damageMultiplier();
            }
        }
        return best;
    }

    public static boolean visible(Level level, Player shooter, Vec3 point) {
        var eye = shooter.getEyePosition();
        var hit = level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(eye) >= point.distanceToSqr(eye)
            - 0.01;
    }

    public static double maxRange(WeaponDefinition definition) {
        return definition.melee()
            .map(melee -> (double) melee.reach() + 1.5)
            .orElse(Math.min(definition.damage().range(), AtomicDawn.CONFIG.vats.VATS_MAX_RANGE));
    }

    public static double angleTo(Player shooter, Vec3 point) {
        var look = shooter.getViewVector(1F);
        var to = point.subtract(shooter.getEyePosition()).normalize();
        return Math.toDegrees(Math.acos(Mth.clamp(look.dot(to), -1.0, 1.0)));
    }

    public static boolean isValidTarget(
        Player shooter,
        Entity target,
        WeaponDefinition definition,
        double coneDegrees
    ) {
        if (
            !(target instanceof LivingEntity living) || !living.isAlive() || target == shooter || target.isSpectator()
        ) {
            return false;
        }
        var torso = parts(target).get(BodyPart.TORSO).position();
        return shooter.getEyePosition().distanceTo(torso) <= maxRange(definition)
            && angleTo(shooter, torso) <= coneDegrees
            && visible(shooter.level(), shooter, torso);
    }

    public static @Nullable LivingEntity acquire(Player shooter, WeaponDefinition definition, double coneDegrees) {
        var range = maxRange(definition);
        var eye = shooter.getEyePosition();
        var area = new AABB(eye, eye).inflate(range);
        List<LivingEntity> candidates = shooter.level()
            .getEntitiesOfClass(LivingEntity.class, area, e -> e != shooter && e != shooter.getVehicle());
        LivingEntity best = null;
        var bestAngle = Double.MAX_VALUE;
        for (var candidate : candidates) {
            if (!isValidTarget(shooter, candidate, definition, coneDegrees)) {
                continue;
            }
            var angle = angleTo(shooter, candidate.getBoundingBox().getCenter());
            if (angle < bestAngle) {
                bestAngle = angle;
                best = candidate;
            }
        }
        return best;
    }

    public static float hitChance(Player shooter, Entity target, WeaponDefinition definition, PartPoint part) {
        if (!visible(shooter.level(), shooter, part.position())) {
            return 0F;
        }
        var distance = shooter.getEyePosition().distanceTo(part.position());
        var cap = (float) AtomicDawn.CONFIG.vats.VATS_MAX_HIT_CHANCE;
        if (definition.isMelee()) {
            var inReach = distance <= maxRange(definition);
            if (!inReach) {
                return 0F;
            }
            var melee = Math.min(cap, 0.95F * part.hitChanceMultiplier() * definition.vats().hitChanceMultiplier());
            return withPerception(shooter, melee, cap);
        }
        var damage = definition.damage();
        if (distance > damage.range()) {
            return 0F;
        }
        var distanceFactor = 1F;
        if (distance > damage.falloffStart() && damage.range() > damage.falloffStart()) {
            var t = (float) ((distance - damage.falloffStart()) / (damage.range() - damage.falloffStart()));
            distanceFactor = 1F - 0.65F * t;
        }
        var accuracyFactor = 1F / (1F + definition.accuracy().baseSpread() * 0.08F);
        var sizeFactor = Mth.clamp(target.getBbHeight() / 1.8F, 0.6F, 1.2F);
        var speed = target.getKnownMovement().horizontalDistance();
        var movementFactor = 1F - (float) Math.min(0.3, speed * 1.5);
        var chance = 0.95F * distanceFactor * accuracyFactor * sizeFactor * movementFactor
            * part.hitChanceMultiplier() * definition.vats().hitChanceMultiplier();
        return withPerception(shooter, Mth.clamp(chance, 0F, cap), cap);
    }

    private static float withPerception(Player shooter, float chance, float cap) {
        if (chance <= 0F) {
            return chance;
        }
        var boosted = chance >= cap
            ? chance
            : Math.min(cap, chance + SpecialStatResolver.resolve(shooter).vatsHitBonus);
        return Math.clamp(
            boosted * GhoulService.vatsHitMultiplier(shooter) * LimbService.vatsHitMultiplier(shooter),
            0F,
            cap
        );
    }
}

package mod.azure.atomicdawn.npc;

import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.weapon.combat.SpreadModel;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class NpcWeapons {

    public enum Kind {
        NONE,
        GUN,
        BOW
    }

    private static final int BOW_INTERVAL = 30;

    private static final int CROSSBOW_INTERVAL = 40;

    private static final int TRACER_COLOR = 0xFFE08A;

    private NpcWeapons() {}

    @Nullable
    public static WeaponDefinition definition(NpcEntity npc) {
        return Weapons.definition(npc.level(), npc.getMainHandItem());
    }

    public static Kind kind(NpcEntity npc) {
        var stack = npc.getMainHandItem();
        if (stack.isEmpty()) {
            return Kind.NONE;
        }
        var definition = definition(npc);
        if (definition != null) {
            return !definition.isMelee() && !definition.isProjectile() ? Kind.GUN : Kind.NONE;
        }
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
            return Kind.BOW;
        }
        return Kind.NONE;
    }

    public static int interval(NpcEntity npc) {
        var definition = definition(npc);
        if (definition != null) {
            return Math.max(3, Math.round(definition.firing().intervalTicks() * 1.5F));
        }
        return npc.getMainHandItem().getItem() instanceof CrossbowItem ? CROSSBOW_INTERVAL : BOW_INTERVAL;
    }

    public static int burst(NpcEntity npc) {
        var definition = definition(npc);
        if (definition == null) {
            return 1;
        }
        return definition.firing().defaultMode().repeatsWhileHeld() ? 4 : 1;
    }

    public static int magazine(NpcEntity npc) {
        var definition = definition(npc);
        return definition == null || !definition.usesAmmo() ? Integer.MAX_VALUE : definition.ammo().magazineSize();
    }

    public static int reloadTicks(NpcEntity npc) {
        var definition = definition(npc);
        return definition == null ? 20 : Math.max(10, definition.reload().ticksFor(true));
    }

    public static void fire(NpcEntity npc, LivingEntity target) {
        if (!(npc.level() instanceof ServerLevel level)) {
            return;
        }
        switch (kind(npc)) {
            case GUN -> fireGun(level, npc, target);
            case BOW -> fireBow(level, npc, target);
            case NONE -> {}
        }
    }

    private static void fireGun(ServerLevel level, NpcEntity npc, LivingEntity target) {
        var definition = definition(npc);
        if (definition == null) {
            return;
        }
        var damage = definition.damage();
        var eye = npc.getEyePosition();
        var aim = target.getBoundingBox().getCenter().add(0, target.getBbHeight() * 0.15, 0);
        var forward = aim.subtract(eye).normalize();
        var cone = definition.accuracy().baseSpread() + npc.config().combat().aimSpread();
        var random = npc.getRandom();
        var pending = new Object2FloatLinkedOpenHashMap<Entity>();
        for (var pellet = 0; pellet < damage.pellets(); pellet++) {
            var direction = SpreadModel.randomDirection(forward, cone, random);
            var maxEnd = eye.add(direction.scale(damage.range()));
            var blockHit = level.clip(
                new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)
            );
            var end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
            var box = npc.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
            var hit = ProjectileUtil.getEntityHitResult(
                npc,
                eye,
                end,
                box,
                entity -> entity != npc && entity.isPickable() && !entity.isSpectator() && npc.canShoot(entity, target),
                eye.distanceToSqr(end)
            );
            if (hit != null) {
                var amount = damage.base() * damage.falloffMultiplier(eye.distanceTo(hit.getLocation()));
                pending.addTo(hit.getEntity(), amount);
                end = hit.getLocation();
            }
            tracer(level, eye, end);
        }
        var multiplier = (float) AtomicDawn.CONFIG.weapons.DAMAGE_MULTIPLIER * npc.config().combat().damageMultiplier();
        for (var entry : pending.object2FloatEntrySet()) {
            var entity = entry.getKey();
            if (entity instanceof LivingEntity living) {
                living.setInvulnerableTime(0);
            }
            entity.hurtServer(
                level,
                ADDamageTypes.source(level, damage.type(), npc),
                entry.getFloatValue() * multiplier
            );
        }
        level.playSound(
            null,
            npc.getX(),
            npc.getEyeY(),
            npc.getZ(),
            definition.sounds().fire().value(),
            SoundSource.HOSTILE,
            1F,
            1F
        );
        level.sendParticles(
            ParticleTypes.SMOKE,
            eye.x + forward.x * 0.6,
            eye.y - 0.15,
            eye.z + forward.z * 0.6,
            2,
            0.02,
            0.02,
            0.02,
            0.0
        );
    }

    private static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
        var delta = to.subtract(from);
        var length = delta.length();
        if (length < 1.0) {
            return;
        }
        var step = delta.scale(1.5 / length);
        var particle = new DustParticleOptions(TRACER_COLOR, 0.5F);
        var point = from.add(step);
        for (var travelled = 1.5; travelled < length; travelled += 1.5) {
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            point = point.add(step);
        }
    }

    private static void fireBow(ServerLevel level, NpcEntity npc, LivingEntity target) {
        var weapon = npc.getMainHandItem();
        var ammo = new ItemStack(Items.ARROW);
        var arrow = ProjectileUtil.getMobArrow(npc, ammo, 1F, weapon);
        var dx = target.getX() - npc.getX();
        var dy = target.getY(0.3333) - arrow.getY();
        var dz = target.getZ() - npc.getZ();
        var horizontal = Math.sqrt(dx * dx + dz * dz);
        var inaccuracy = Math.max(0F, npc.config().combat().aimSpread() * 1.5F);
        Projectile.spawnProjectileUsingShoot(arrow, level, ammo, dx, dy + horizontal * 0.2, dz, 1.6F, inaccuracy);
        npc.playSound(SoundEvents.SKELETON_SHOOT, 1F, 1F / (npc.getRandom().nextFloat() * 0.4F + 0.8F));
    }
}

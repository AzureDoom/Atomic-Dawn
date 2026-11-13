package mod.azure.atomicdawn.perks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.party.PartyService;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;

public final class PerkCombat {

    public static final int MARK_TICKS = 100;

    public static final int BUFF_TICKS = 600;

    public static final int RETRIBUTION_TICKS = 100;

    public static final double LONG_RANGE = 24.0;

    public static final float LOW_HEALTH = 0.35F;

    private static final int MAX_CHAIN = 3;

    record Hit(
        int targetId,
        WeaponCategory category,
        boolean vats,
        long tick
    ) {}

    static final class Mark {

        float bonus;

        float weaken;

        long bonusUntil;

        long weakenUntil;
    }

    static final class Bleed {

        final WeakReference<LivingEntity> target;

        final WeakReference<ServerPlayer> attacker;

        final float damage;

        int seconds;

        Bleed(LivingEntity target, ServerPlayer attacker, float damage, int seconds) {
            this.target = new WeakReference<>(target);
            this.attacker = new WeakReference<>(attacker);
            this.damage = damage;
            this.seconds = seconds;
        }
    }

    static final class Buffs {

        long killDamageUntil;

        long retributionUntil;
    }

    static final Map<UUID, Hit> LAST_HIT = new HashMap<>();

    static final Map<UUID, Mark> MARKS = new HashMap<>();

    static final List<Bleed> BLEEDS = new ArrayList<>();

    static final Map<UUID, Buffs> BUFFS = new HashMap<>();

    private static int weaponDepth;

    private static int procDepth;

    private static int chain;

    private PerkCombat() {}

    public static boolean inWeaponHit() {
        return weaponDepth > 0;
    }

    public static boolean inProc() {
        return procDepth > 0;
    }

    public static void beginWeaponHit() {
        weaponDepth++;
    }

    public static void endWeaponHit() {
        weaponDepth = Math.max(0, weaponDepth - 1);
    }

    static Buffs buffs(Player player) {
        return BUFFS.computeIfAbsent(player.getUUID(), _ -> new Buffs());
    }

    static void forget(UUID id) {
        LAST_HIT.remove(id);
        MARKS.remove(id);
        BUFFS.remove(id);
    }

    public static boolean isNight(Entity entity) {
        var level = entity.level();
        var time = level.getDefaultClockTime() % 24000L;
        return level.dimensionType().hasSkyLight() && time > 13000L && time < 23000L;
    }

    public static boolean lowHealth(LivingEntity entity) {
        return entity.getHealth() <= entity.getMaxHealth() * LOW_HEALTH;
    }

    public static boolean sneaking(ServerPlayer attacker, Entity target) {
        if (!attacker.isCrouching()) {
            return false;
        }
        return !(target instanceof Mob mob) || mob.getTarget() != attacker;
    }

    public static float outgoingMultiplier(
        ServerPlayer attacker,
        Entity target,
        WeaponCategory category,
        boolean automatic,
        boolean vats
    ) {
        var stats = PerkHooks.stats(attacker);
        var bonus = 0F;
        var now = attacker.level().getGameTime();
        if (sneaking(attacker, target)) {
            bonus += stats.value(PerkEffectType.SNEAK_DAMAGE, category, automatic);
        }
        if (isNight(attacker)) {
            bonus += stats.value(PerkEffectType.NIGHT_DAMAGE, category, automatic);
        }
        if (lowHealth(attacker)) {
            bonus += stats.value(PerkEffectType.LOW_HEALTH_DAMAGE, category, automatic);
        }
        if (vats) {
            bonus += stats.value(PerkEffectType.VATS_DAMAGE, category, automatic);
        }
        if (attacker.distanceToSqr(target) >= LONG_RANGE * LONG_RANGE) {
            bonus += stats.value(PerkEffectType.LONG_RANGE_DAMAGE, category, automatic);
        }
        if (target.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD)) {
            bonus += stats.value(PerkEffectType.BUG_DAMAGE, category, automatic);
        }
        if (target.isCurrentlyGlowing()) {
            bonus += stats.value(PerkEffectType.GLOWING_DAMAGE, category, automatic);
        }
        var buffs = BUFFS.get(attacker.getUUID());
        if (buffs != null) {
            if (now < buffs.killDamageUntil) {
                bonus += stats.value(PerkEffectType.KILL_DAMAGE, category, automatic);
            }
            if (now < buffs.retributionUntil) {
                bonus += stats.extra(PerkEffectType.RETRIBUTION);
            }
        }
        return 1F + Math.clamp(bonus, 0F, (float) AtomicDawn.CONFIG.perks.PERKS_MAX_DAMAGE_BONUS);
    }

    public static float armorPenetration(ServerPlayer attacker, WeaponCategory category, boolean automatic) {
        return Math.clamp(
            PerkHooks.stats(attacker).value(PerkEffectType.ARMOR_PENETRATION, category, automatic),
            0F,
            0.9F
        );
    }

    public static void onHit(
        ServerLevel level,
        ServerPlayer attacker,
        Entity target,
        WeaponCategory category,
        boolean automatic,
        boolean vats,
        float damage,
        Vec3 position
    ) {
        if (!(target instanceof LivingEntity living) || inProc()) {
            return;
        }
        var now = level.getGameTime();
        LAST_HIT.put(attacker.getUUID(), new Hit(target.getId(), category, vats, now));
        var stats = PerkHooks.stats(attacker);
        var random = attacker.getRandom();
        var melee = category.melee();

        if (roll(random, stats.value(PerkEffectType.CRIPPLE_CHANCE, category, automatic))) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), attacker);
        }
        if (melee && roll(random, stats.value(PerkEffectType.STAGGER_CHANCE, category, automatic))) {
            var away = living.position().subtract(attacker.position());
            living.knockback(0.6, -away.x, -away.z, living.damageSources().generic(), 0.0F);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), attacker);
        } else if (!melee && roll(random, stats.value(PerkEffectType.STAGGER_CHANCE, category, automatic))) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2), attacker);
        }
        if (roll(random, stats.value(PerkEffectType.BLEED_CHANCE, category, automatic))) {
            BLEEDS.add(new Bleed(living, attacker, Math.max(0.5F, damage * 0.1F), 4));
        }
        if (roll(random, stats.value(PerkEffectType.FIRE_CHANCE, category, automatic))) {
            living.igniteForSeconds(3F);
        }
        var mark = stats.value(PerkEffectType.TARGET_MARK, category, automatic);
        if (!melee && sneaking(attacker, target)) {
            mark = Math.max(mark, stats.value(PerkEffectType.SNEAK_MARK, category, automatic));
        }
        if (mark > 0F) {
            mark(living, mark, 0F, now);
        }
        var weaken = stats.value(PerkEffectType.WEAKEN_TARGET, category, automatic);
        if (weaken > 0F) {
            mark(living, 0F, weaken, now);
        }
        if (melee && roll(random, stats.extra(PerkEffectType.CHEM_PROC))) {
            attacker.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
        }

        procDepth++;
        try {
            if (vats && roll(random, stats.value(PerkEffectType.VATS_PROC, category, automatic)) && living.isAlive()) {
                living.setInvulnerableTime(0);
                living.hurtServer(level, level.damageSources().playerAttack(attacker), damage);
                level.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z, 8, 0.2, 0.2, 0.2, 0.1);
            }
            var cleave = stats.value(PerkEffectType.VATS_CLEAVE, category, automatic);
            if (vats && melee && cleave > 0F) {
                var area = living.getBoundingBox().inflate(2.5);
                for (var other : level.getEntitiesOfClass(LivingEntity.class, area, e -> isFoe(attacker, e))) {
                    if (other != living) {
                        other.setInvulnerableTime(0);
                        other.hurtServer(level, level.damageSources().playerAttack(attacker), damage * cleave);
                    }
                }
            }
        } finally {
            procDepth--;
        }
        if (
            category == WeaponCategory.UNARMED
                && roll(random, stats.value(PerkEffectType.UNARMED_EXPLOSION, category, automatic))
        ) {
            explode(level, attacker, position, damage * 0.5F, 2.0F);
        }
    }

    static void mark(LivingEntity target, float bonus, float weaken, long now) {
        var mark = MARKS.computeIfAbsent(target.getUUID(), _ -> new Mark());
        if (bonus > 0F) {
            mark.bonus = now < mark.bonusUntil ? Math.max(mark.bonus, bonus) : bonus;
            mark.bonusUntil = now + MARK_TICKS;
        }
        if (weaken > 0F) {
            mark.weaken = now < mark.weakenUntil ? Math.max(mark.weaken, weaken) : weaken;
            mark.weakenUntil = now + MARK_TICKS;
        }
    }

    static float markBonus(Entity entity, long now) {
        var mark = MARKS.get(entity.getUUID());
        return mark != null && now < mark.bonusUntil ? mark.bonus : 0F;
    }

    static float markWeaken(Entity entity, long now) {
        var mark = MARKS.get(entity.getUUID());
        return mark != null && now < mark.weakenUntil ? mark.weaken : 0F;
    }

    static boolean roll(net.minecraft.util.RandomSource random, float chance) {
        return chance > 0F && random.nextFloat() < Math.min(1F, chance);
    }

    static boolean isFoe(ServerPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player other) {
            return !PartyService.sameParty(player, other) && !player.isAlliedTo(other);
        }
        return !player.isAlliedTo(entity);
    }

    static void explode(ServerLevel level, ServerPlayer attacker, Vec3 at, float damage, float radius) {
        if (damage <= 0F || chain >= MAX_CHAIN) {
            return;
        }
        chain++;
        procDepth++;
        try {
            var source = ADDamageTypes.source(level, ADDamageTypes.LEGENDARY_EXPLOSION, attacker);
            var area = new AABB(at, at).inflate(radius);
            for (var entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> isFoe(attacker, e))) {
                if (entity.position().distanceToSqr(at) <= radius * radius + entity.getBbWidth()) {
                    entity.setInvulnerableTime(0);
                    entity.hurtServer(level, source, damage);
                }
            }
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            level.playSound(
                null,
                at.x,
                at.y,
                at.z,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                0.5F,
                1.4F
            );
        } finally {
            procDepth--;
            chain--;
        }
    }

    static void tickBleeds() {
        if (BLEEDS.isEmpty()) {
            return;
        }
        procDepth++;
        try {
            for (var it = BLEEDS.iterator(); it.hasNext();) {
                var bleed = it.next();
                var target = bleed.target.get();
                var attacker = bleed.attacker.get();
                if (target == null || !target.isAlive() || !(target.level() instanceof ServerLevel level)) {
                    it.remove();
                    continue;
                }
                var source = attacker != null
                    ? level.damageSources().playerAttack(attacker)
                    : level.damageSources().generic();
                target.setInvulnerableTime(0);
                target.hurtServer(level, source, bleed.damage);
                level.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(),
                    target.getY(0.5),
                    target.getZ(),
                    2,
                    0.2,
                    0.2,
                    0.2,
                    0.0
                );
                if (--bleed.seconds <= 0) {
                    it.remove();
                }
            }
        } finally {
            procDepth--;
        }
    }

    static void prune(long now) {
        MARKS.values().removeIf(mark -> now >= mark.bonusUntil && now >= mark.weakenUntil);
        LAST_HIT.values().removeIf(hit -> now - hit.tick() > 200);
    }
}

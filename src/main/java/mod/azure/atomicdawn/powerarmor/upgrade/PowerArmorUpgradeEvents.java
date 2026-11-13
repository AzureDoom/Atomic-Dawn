package mod.azure.atomicdawn.powerarmor.upgrade;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.mixin.FoodDataAccessor;
import mod.azure.atomicdawn.mutation.MutationEvents;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.registry.ADMobEffects;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class PowerArmorUpgradeEvents {

    private static final Identifier MAX_HEALTH_MODIFIER = AtomicDawn.id("power_armor_legendary");

    private static final int COMBAT_TICKS = 100;

    private static final int AUTO_STIM_COOLDOWN = 1200;

    private static final int CLOAK_COOLDOWN = 400;

    private static final int CHAMELEON_STILL_TICKS = 40;

    private static final class PilotState {

        long lastCombat = Long.MIN_VALUE / 2;

        long autoStimReady;

        long cloakReady;

        long cloakUntil;

        long stillSince;

        double lastX;

        double lastZ;

        float lastExhaustion = -1F;

        final Set<Holder<MobEffect>> granted = new HashSet<>();
    }

    private static final Map<UUID, PilotState> PILOTS = new HashMap<>();

    private PowerArmorUpgradeEvents() {}

    public static int specialBonus(Player player, SpecialAttribute attribute) {
        var bonuses = PowerArmorBonuses.of(player);
        if (bonuses == PowerArmorBonuses.NONE) {
            return 0;
        }
        var total = 0F;
        for (var type : PowerArmorEffectType.values()) {
            if (type.special() == attribute) {
                total += bonuses.get(type);
            }
        }
        return Math.round(total);
    }

    public static float reductionMultiplier(Player player, PowerArmorEffectType type) {
        return 1F - Math.clamp(PowerArmorBonuses.of(player).get(type), 0F, 0.9F);
    }

    public static float apCostMultiplier(Player player, WeaponCategory category) {
        var bonuses = PowerArmorBonuses.of(player);
        var reduction = bonuses.get(PowerArmorEffectType.AP_COST);
        if (category == WeaponCategory.MELEE || category == WeaponCategory.UNARMED) {
            reduction += bonuses.unarmed(player, PowerArmorEffectType.MELEE_AP_COST);
        }
        return 1F - Math.clamp(reduction, 0F, 0.6F);
    }

    public static float speedMultiplier(Player pilot, PowerArmorEntity armor) {
        return 1F + Math.clamp(PowerArmorBonuses.of(armor).get(PowerArmorEffectType.SPEED), -0.5F, 1F);
    }

    public static int headlamp(Player player) {
        return Math.round(Math.clamp(PowerArmorBonuses.of(player).get(PowerArmorEffectType.HEADLAMP), 0F, 15F));
    }

    private static boolean isMelee(DamageSource source, LivingEntity victim) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypes.THORNS)
            && attacker.distanceToSqr(victim) < 36.0;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        var victim = event.getEntity();
        var level = victim.level();
        if (level.isClientSide()) {
            return;
        }

        if (
            source.getEntity() instanceof ServerPlayer attacker && attacker
                .getVehicle() instanceof PowerArmorEntity armor && victim != armor && isMelee(source, victim)
                && attacker.getMainHandItem().isEmpty()
        ) {
            var bonuses = PowerArmorBonuses.of(armor);
            var amount = event.getAmount() * (1F + bonuses.unarmed(attacker, PowerArmorEffectType.UNARMED_DAMAGE))
                + bonuses.unarmed(attacker, PowerArmorEffectType.UNARMED_ENERGY);
            event.setAmount(amount);
            var bleed = bonuses.unarmed(attacker, PowerArmorEffectType.BLEED);
            if (bleed > 0F) {
                victim.addEffect(new MobEffectInstance(ADMobEffects.BLEEDING, Math.round(bleed * 20F), 0), attacker);
            }
            state(attacker).lastCombat = level.getGameTime();
        }

        if (!(victim instanceof ServerPlayer pilot) || !(pilot.getVehicle() instanceof PowerArmorEntity armor)) {
            return;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        var bonuses = PowerArmorBonuses.of(armor);
        if (bonuses.isEmpty()) {
            return;
        }
        state(pilot).lastCombat = level.getGameTime();
        var reduction = reduction(pilot, armor, bonuses, source);
        if (reduction > 0F) {
            var cap = (float) AtomicDawn.CONFIG.powerArmor.POWER_ARMOR_LEGENDARY_MAX_REDUCTION;
            event.setAmount(event.getAmount() * (1F - Math.min(reduction, cap)));
        }
        if (isMelee(source, pilot) && source.getEntity() instanceof LivingEntity attacker && attacker != armor) {
            retaliate(pilot, attacker, bonuses);
        }
    }

    private static float reduction(
        ServerPlayer pilot,
        PowerArmorEntity armor,
        PowerArmorBonuses bonuses,
        DamageSource source
    ) {
        var total = bonuses.reductionFrom(source.getEntity());
        var health = pilot.getHealth() / Math.max(1F, pilot.getMaxHealth());
        total += bonuses.get(PowerArmorEffectType.REDUCE_LOW_HEALTH) * (1F - health);
        total += bonuses.get(PowerArmorEffectType.REDUCE_HIGH_HEALTH) * Math.clamp((health - 0.5F) * 2F, 0F, 1F);
        var level = pilot.level();
        var time = level.getDefaultClockTime() % 24000L;
        if (level.dimensionType().hasSkyLight() && time > 13000L && time < 23000L) {
            total += bonuses.get(PowerArmorEffectType.REDUCE_AT_NIGHT);
        }
        if (pilot.getFoodData().getFoodLevel() >= 18) {
            total += bonuses.get(PowerArmorEffectType.REDUCE_WELL_FED);
        }
        total += bonuses.get(PowerArmorEffectType.REDUCE_EXPERIENCE) * Math.min(1F, pilot.experienceLevel / 30F);
        if (RadiationService.rads(pilot) >= RadiationService.maxRads() * 0.25F) {
            total += bonuses.get(PowerArmorEffectType.REDUCE_IRRADIATED);
        }
        var dx = armor.getX() - armor.xo;
        var dz = armor.getZ() - armor.zo;
        var moving = dx * dx + dz * dz > 1.0E-4;
        total += bonuses.get(moving ? PowerArmorEffectType.REDUCE_MOVING : PowerArmorEffectType.REDUCE_STILL);
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            total += bonuses.get(PowerArmorEffectType.REDUCE_PROJECTILE);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            total += bonuses.get(PowerArmorEffectType.REDUCE_EXPLOSION);
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            total += bonuses.get(PowerArmorEffectType.REDUCE_FIRE);
        }
        if (source.is(ADDamageTypes.ENERGY) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            total += bonuses.get(PowerArmorEffectType.REDUCE_ENERGY);
        }
        return total;
    }

    private static void retaliate(ServerPlayer pilot, LivingEntity attacker, PowerArmorBonuses bonuses) {
        var fire = bonuses.get(PowerArmorEffectType.RETALIATE_FIRE);
        if (fire > 0F) {
            attacker.igniteForSeconds(fire);
        }
        var slow = bonuses.get(PowerArmorEffectType.RETALIATE_SLOW);
        if (slow > 0F) {
            attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.round(slow * 20F), 1), pilot);
        }
        var poison = bonuses.get(PowerArmorEffectType.RETALIATE_POISON);
        if (poison > 0F) {
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, Math.round(poison * 20F), 0), pilot);
        }
        var energy = bonuses.get(PowerArmorEffectType.RETALIATE_ENERGY);
        if (energy > 0F) {
            ServerLevel level = pilot.level();
            attacker.hurtServer(level, level.damageSources().thorns(pilot), energy);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (
            !(event.getEntity() instanceof ServerPlayer pilot) || !(pilot
                .getVehicle() instanceof PowerArmorEntity armor)
        ) {
            return;
        }
        var bonuses = PowerArmorBonuses.of(armor);
        if (bonuses.isEmpty() || event.getInflictedDamage() <= 0F || !pilot.isAlive()) {
            return;
        }
        var state = state(pilot);
        var now = pilot.level().getGameTime();
        var dissipate = bonuses.get(PowerArmorEffectType.DISSIPATE);
        if (dissipate > 0F) {
            RadiationService.remove(pilot, dissipate);
        }
        var cloak = bonuses.get(PowerArmorEffectType.CLOAKING);
        if (cloak > 0F && now >= state.cloakReady) {
            state.cloakUntil = now + Math.round(cloak * 20F);
            state.cloakReady = now + CLOAK_COOLDOWN;
        }
        var stim = bonuses.get(PowerArmorEffectType.AUTO_STIM);
        if (stim > 0F && now >= state.autoStimReady && pilot.getHealth() < pilot.getMaxHealth() * 0.25F) {
            state.autoStimReady = now + AUTO_STIM_COOLDOWN;
            pilot.heal(pilot.getMaxHealth() * Math.min(1F, stim));
            pilot.level()
                .playSound(
                    null,
                    pilot.blockPosition(),
                    SoundEvents.BREWING_STAND_BREW,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.6F
                );
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer pilot && pilot.getVehicle() instanceof PowerArmorEntity armor) {
            var bonus = PowerArmorBonuses.of(armor).get(PowerArmorEffectType.HEALING_RECEIVED);
            if (bonus > 0F) {
                event.setAmount(event.getAmount() * (1F + bonus));
            }
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (
            event.getSource().getEntity() instanceof ServerPlayer pilot && pilot
                .getVehicle() instanceof PowerArmorEntity armor
        ) {
            var adrenal = PowerArmorBonuses.of(armor).get(PowerArmorEffectType.ADRENAL);
            if (adrenal > 0F) {
                pilot.addEffect(new MobEffectInstance(MobEffects.SPEED, Math.round(adrenal * 20F), 0));
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (
            event.getEntity() instanceof Player pilot && pilot.getVehicle() instanceof PowerArmorEntity armor
                && PowerArmorBonuses.of(armor).immuneTo(event.getEffectInstance().getEffect())
        ) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (
            event.getEntity() instanceof LivingEntity living && living.level() instanceof ServerLevel level
                && living.tickCount % 20 == 0
        ) {
            var bleeding = living.getEffect(ADMobEffects.BLEEDING);
            if (bleeding != null) {
                living.hurtServer(level, level.damageSources().generic(), 1F + bleeding.getAmplifier());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.getVehicle() instanceof PowerArmorEntity armor)) {
            var state = PILOTS.remove(player.getUUID());
            if (state != null) {
                cleanup(player, state);
            }
            return;
        }
        var bonuses = PowerArmorBonuses.of(armor);
        var state = state(player);
        var now = player.level().getGameTime();

        var moved = Math.abs(armor.getX() - state.lastX) + Math.abs(armor.getZ() - state.lastZ) > 0.01;
        state.lastX = armor.getX();
        state.lastZ = armor.getZ();
        if (moved || state.stillSince == 0) {
            state.stillSince = now;
        }
        var chameleon = bonuses.has(PowerArmorEffectType.CHAMELEON) && now - state.stillSince >= CHAMELEON_STILL_TICKS;
        var cloaked = now < state.cloakUntil;

        var invisible = chameleon || cloaked || MutationEvents.chameleonActive(player);
        if (armor.isInvisible() != invisible) {
            armor.setInvisible(invisible);
        }

        if (bonuses.has(PowerArmorEffectType.WARMING)) {
            player.setTicksFrozen(0);
        }
        var exhaustion = bonuses.get(PowerArmorEffectType.EXHAUSTION);
        var food = player.getFoodData();
        FoodDataAccessor accessor = (FoodDataAccessor) food;

        float currentExhaustion = accessor.atomicdawn$getExhaustionLevel();

        if (
            exhaustion > 0F
                && state.lastExhaustion >= 0F
                && currentExhaustion > state.lastExhaustion
        ) {

            float gained = currentExhaustion - state.lastExhaustion;

            float reduction = gained * Math.min(0.9F, exhaustion);

            accessor.atomicdawn$setExhaustionLevel(
                Math.max(0.0F, currentExhaustion - reduction)
            );
        }

        state.lastExhaustion = accessor.atomicdawn$getExhaustionLevel();

        var magnet = bonuses.get(PowerArmorEffectType.ITEM_MAGNET);
        if (magnet > 0F && now % 5 == 0) {
            pullItems(player, magnet);
        }

        if (now % 20 != 0) {
            return;
        }
        keepEffect(player, state, MobEffects.NIGHT_VISION, bonuses.has(PowerArmorEffectType.NIGHT_VISION), 0);
        var haste = Math.round(bonuses.get(PowerArmorEffectType.HASTE));
        keepEffect(player, state, MobEffects.HASTE, haste > 0, Math.max(0, haste - 1));
        for (var effect : bonuses.immunities()) {
            if (player.hasEffect(effect)) {
                player.removeEffect(effect);
            }
        }
        var scrub = bonuses.get(PowerArmorEffectType.RAD_SCRUB);
        if (scrub > 0F && RadiationService.rads(player) > 0F) {
            RadiationService.remove(player, scrub);
        }
        var regen = bonuses.get(PowerArmorEffectType.REGENERATING);
        if (regen > 0F && now - state.lastCombat > COMBAT_TICKS && player.getHealth() < player.getMaxHealth()) {
            player.heal(regen);
        }
        applyMaxHealth(player, bonuses.get(PowerArmorEffectType.MAX_HEALTH));
    }

    private static void pullItems(ServerPlayer player, float radius) {
        var center = player.position().add(0, 0.5, 0);
        for (
            var item : player.level()
                .getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius))
        ) {
            if (item.hasPickUpDelay()) {
                continue;
            }
            var pull = center.subtract(item.position());
            if (pull.lengthSqr() > 0.25) {
                item.setDeltaMovement(pull.normalize().scale(0.35).add(0, 0.04, 0));
            }
        }
    }

    private static void keepEffect(
        ServerPlayer player,
        PilotState state,
        Holder<MobEffect> effect,
        boolean active,
        int amplifier
    ) {
        var current = player.getEffect(effect);
        if (active) {
            if (current == null || (state.granted.contains(effect) && current.getDuration() < 260)) {
                player.addEffect(new MobEffectInstance(effect, 400, amplifier, true, false, true));
                state.granted.add(effect);
            }
        } else if (state.granted.remove(effect) && current != null && current.isAmbient()) {
            player.removeEffect(effect);
        }
    }

    private static void applyMaxHealth(ServerPlayer player, float amount) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        var existing = attribute.getModifier(MAX_HEALTH_MODIFIER);
        if (existing != null && Math.abs(existing.amount() - amount) < 1.0E-3) {
            return;
        }
        attribute.removeModifier(MAX_HEALTH_MODIFIER);
        if (amount > 0F) {
            attribute.addTransientModifier(
                new AttributeModifier(MAX_HEALTH_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE)
            );
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void cleanup(ServerPlayer player, PilotState state) {
        for (var effect : state.granted) {
            var current = player.getEffect(effect);
            if (current != null && current.isAmbient()) {
                player.removeEffect(effect);
            }
        }
        applyMaxHealth(player, 0F);
    }

    private static PilotState state(ServerPlayer player) {
        return PILOTS.computeIfAbsent(player.getUUID(), _ -> new PilotState());
    }
}

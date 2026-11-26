package mod.azure.atomicdawn.armor.upgrade;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.armor.ArmorPieceItem;
import mod.azure.atomicdawn.helper.ADEntityUtil;
import mod.azure.atomicdawn.mixin.FoodDataAccessor;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class ArmorEvents {

    private static final Identifier ARMOR_MODIFIER = AtomicDawn.id("armor_mods_armor");

    private static final Identifier TOUGHNESS_MODIFIER = AtomicDawn.id("armor_mods_toughness");

    private static final Identifier KNOCKBACK_MODIFIER = AtomicDawn.id("armor_mods_knockback");

    private static final Identifier HEALTH_MODIFIER = AtomicDawn.id("armor_mods_health");

    private static final Identifier SPEED_MODIFIER = AtomicDawn.id("armor_mods_speed");

    private static final int COMBAT_TICKS = 100;

    private static final int AUTO_STIM_COOLDOWN = 1200;

    private static final int CLOAK_COOLDOWN = 400;

    private static final int CHAMELEON_STILL_TICKS = 40;

    private static final int LIFE_SAVING_COOLDOWN = 6000;

    private static final float UNYIELDING_HEALTH = 0.2F;

    private static final class WearerState {

        long lastCombat = Long.MIN_VALUE / 2;

        long autoStimReady;

        long cloakReady;

        long cloakUntil;

        long stillSince;

        long lifeSavingReady;

        double lastX;

        double lastZ;

        boolean moving;

        float lastExhaustion = -1F;

        long snapshotTick = Long.MIN_VALUE;

        final ItemStack[] snapshotStacks = new ItemStack[ArmorPiece.COUNT];

        final int[] snapshotDamage = new int[ArmorPiece.COUNT];

        final Set<Holder<MobEffect>> granted = new HashSet<>();
    }

    private static final Map<UUID, WearerState> WEARERS = new HashMap<>();

    private ArmorEvents() {}

    public static int specialBonus(Player player, SpecialAttribute attribute) {
        var bonuses = ArmorBonuses.of(player);
        if (bonuses == ArmorBonuses.NONE) {
            return 0;
        }
        var total = 0F;
        for (var type : ArmorEffectType.values()) {
            if (type.special() == attribute) {
                total += bonuses.get(type);
            }
        }
        if (
            attribute != SpecialAttribute.ENDURANCE && bonuses.has(ArmorEffectType.UNYIELDING)
                && player.getHealth() < player.getMaxHealth() * UNYIELDING_HEALTH
        ) {
            total += bonuses.get(ArmorEffectType.UNYIELDING);
        }
        return Math.round(total);
    }

    public static float reductionMultiplier(Player player, ArmorEffectType type) {
        return 1F - Math.clamp(ArmorBonuses.of(player).get(type), 0F, 0.9F);
    }

    public static float apCostMultiplier(Player player) {
        return 1F - Math.clamp(ArmorBonuses.of(player).get(ArmorEffectType.AP_COST), 0F, 0.6F);
    }

    public static float maxApBonus(Player player) {
        return ArmorBonuses.of(player).get(ArmorEffectType.MAX_AP);
    }

    public static float apRegenBonus(Player player) {
        return ArmorBonuses.of(player).get(ArmorEffectType.AP_REGEN);
    }

    public static float criticalDamageBonus(Player player) {
        return ArmorBonuses.of(player).get(ArmorEffectType.CRITICAL_DAMAGE);
    }

    public static boolean isBigGun(WeaponCategory category) {
        return category == WeaponCategory.HEAVY || category == WeaponCategory.EXPLOSIVE;
    }

    public static float spreadMultiplier(Player player, WeaponCategory category) {
        if (!isBigGun(category)) {
            return 1F;
        }
        var reduction = player.getVehicle() instanceof PowerArmorEntity
            ? player.getData(ADAttachments.STOWED_STABILIZED.get()) * 2F
            : ArmorBonuses.of(player).get(ArmorEffectType.SPREAD);
        return 1F - Math.clamp(reduction, 0F, 0.9F);
    }

    public static void onPowerArmorStow(ServerPlayer player, ItemStack chest) {
        var amount = 0F;
        if (!chest.isEmpty()) {
            var access = player.level().registryAccess();
            var upgrades = ArmorUpgrades.of(chest);
            if (chest.getItem() instanceof ArmorPieceItem item) {
                for (var id : upgrades.mods()) {
                    var mod = ArmorMod.get(access, id).orElse(null);
                    if (mod != null && mod.value().fits(item)) {
                        for (var effect : mod.value().effects()) {
                            if (effect.type() == ArmorEffectType.SPREAD) {
                                amount += effect.amount();
                            }
                        }
                    }
                }
            }
        }
        player.setData(ADAttachments.STOWED_STABILIZED.get(), amount);
    }

    public static void jetPack(ServerPlayer player) {
        var bonuses = ArmorBonuses.of(player);
        if (!bonuses.has(ArmorEffectType.JET_PACK) || player.onGround() || player.isInWater()) {
            return;
        }
        var cost = (float) AtomicDawn.CONFIG.common.equipment.armor.ARMOR_JETPACK_AP_COST;
        if (!player.hasInfiniteMaterials() && !VatsActions.spend(player, cost)) {
            return;
        }
        player.resetFallDistance();
    }

    private static boolean isMelee(DamageSource source, LivingEntity victim) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypes.THORNS)
            && attacker.distanceToSqr(victim) < 36.0;
    }

    private static boolean isNight(ServerPlayer player) {
        var level = player.level();
        var time = level.getDefaultClockTime() % 24000L;
        return level.dimensionType().hasSkyLight() && time > 13000L && time < 23000L;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        var victim = event.getEntity();
        var level = victim.level();
        if (level.isClientSide()) {
            return;
        }

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim && isMelee(source, victim)) {
            var bonuses = ArmorBonuses.of(attacker);
            if (!bonuses.isEmpty()) {
                var multiplier = 1F + bonuses.get(ArmorEffectType.MELEE_DAMAGE);
                if (attacker.getMainHandItem().isEmpty()) {
                    multiplier += bonuses.get(ArmorEffectType.UNARMED_DAMAGE);
                }
                event.setAmount(event.getAmount() * multiplier);
                state(attacker).lastCombat = level.getGameTime();
            }
        }

        if (!(victim instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        var bonuses = ArmorBonuses.of(player);
        if (bonuses.isEmpty()) {
            return;
        }
        var state = state(player);
        var now = level.getGameTime();
        snapshotDurability(player, state, now);
        if (source.is(DamageTypeTags.IS_FALL)) {
            var fall = bonuses.get(ArmorEffectType.REDUCE_FALL);
            if (fall > 0F) {
                event.setAmount(event.getAmount() * (1F - Math.min(fall, 0.9F)));
            }
            return;
        }
        state.lastCombat = now;
        var reduction = reduction(player, state, bonuses, source);
        if (reduction > 0F) {
            var cap = (float) AtomicDawn.CONFIG.common.equipment.armor.ARMOR_LEGENDARY_MAX_REDUCTION;
            event.setAmount(event.getAmount() * (1F - Math.min(reduction, cap)));
        }
        if (isMelee(source, player) && source.getEntity() instanceof LivingEntity attacker) {
            retaliate(player, attacker, bonuses);
        }
    }

    private static void snapshotDurability(ServerPlayer player, WearerState state, long now) {
        state.snapshotTick = now;
        for (var piece : ArmorPiece.all()) {
            var stack = player.getItemBySlot(piece.slot());
            state.snapshotStacks[piece.ordinal()] = stack;
            state.snapshotDamage[piece.ordinal()] = stack.isDamageableItem() ? stack.getDamageValue() : -1;
        }
    }

    private static float reduction(
        ServerPlayer player,
        WearerState state,
        ArmorBonuses bonuses,
        DamageSource source
    ) {
        var total = bonuses.reductionFrom(source.getEntity());
        var health = player.getHealth() / Math.max(1F, player.getMaxHealth());
        total += bonuses.get(ArmorEffectType.REDUCE_LOW_HEALTH) * (1F - health);
        total += bonuses.get(ArmorEffectType.REDUCE_HIGH_HEALTH) * Math.clamp((health - 0.5F) * 2F, 0F, 1F);
        if (isNight(player)) {
            total += bonuses.get(ArmorEffectType.REDUCE_AT_NIGHT);
        }
        if (player.getFoodData().getFoodLevel() >= 18) {
            total += bonuses.get(ArmorEffectType.REDUCE_WELL_FED);
        }
        total += bonuses.get(ArmorEffectType.REDUCE_EXPERIENCE) * Math.min(1F, player.experienceLevel / 30F);
        if (RadiationService.rads(player) >= RadiationService.maxRads() * 0.25F) {
            total += bonuses.get(ArmorEffectType.REDUCE_IRRADIATED);
        }
        if (player.isSprinting()) {
            total += bonuses.get(ArmorEffectType.REDUCE_SPRINTING);
        }
        if (!state.moving) {
            total += bonuses.get(ArmorEffectType.REDUCE_STILL);
        }
        if (player.isBlocking() || player.isCrouching()) {
            total += bonuses.get(ArmorEffectType.REDUCE_BLOCKING);
        }
        if (isMelee(source, player)) {
            total += bonuses.get(ArmorEffectType.REDUCE_MELEE);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            total += bonuses.get(ArmorEffectType.REDUCE_PROJECTILE);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            total += bonuses.get(ArmorEffectType.REDUCE_EXPLOSION);
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            total += bonuses.get(ArmorEffectType.REDUCE_FIRE);
        }
        if (source.is(ADDamageTypes.ENERGY) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            total += bonuses.get(ArmorEffectType.REDUCE_ENERGY);
        }
        return total;
    }

    private static void retaliate(ServerPlayer player, LivingEntity attacker, ArmorBonuses bonuses) {
        var fire = bonuses.get(ArmorEffectType.RETALIATE_FIRE);
        if (fire > 0F) {
            attacker.igniteForSeconds(fire);
        }
        var slow = bonuses.get(ArmorEffectType.RETALIATE_SLOW);
        if (slow > 0F) {
            attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.round(slow * 20F), 1), player);
        }
        var poison = bonuses.get(ArmorEffectType.RETALIATE_POISON);
        if (poison > 0F) {
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, Math.round(poison * 20F), 0), player);
        }
        var energy = bonuses.get(ArmorEffectType.RETALIATE_ENERGY);
        if (energy > 0F) {
            ADEntityUtil.hurt(attacker, player.level(), player.level().damageSources().thorns(player), energy);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var bonuses = ArmorBonuses.of(player);
        if (bonuses.isEmpty()) {
            return;
        }
        var state = state(player);
        var now = player.level().getGameTime();
        restoreDurability(player, state, bonuses, now);
        if (event.getInflictedDamage() <= 0F || !player.isAlive()) {
            return;
        }
        var dissipate = bonuses.get(ArmorEffectType.DISSIPATE);
        if (dissipate > 0F) {
            RadiationService.remove(player, dissipate);
        }
        var cloak = bonuses.get(ArmorEffectType.CLOAKING);
        if (cloak > 0F && now >= state.cloakReady) {
            state.cloakUntil = now + Math.round(cloak * 20F);
            state.cloakReady = now + CLOAK_COOLDOWN;
        }
        var stim = bonuses.get(ArmorEffectType.AUTO_STIM);
        if (stim > 0F && now >= state.autoStimReady && player.getHealth() < player.getMaxHealth() * 0.25F) {
            state.autoStimReady = now + AUTO_STIM_COOLDOWN;
            player.heal(player.getMaxHealth() * Math.min(1F, stim));
            player.level()
                .playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BREWING_STAND_BREW,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.6F
                );
        }
    }

    private static void restoreDurability(ServerPlayer player, WearerState state, ArmorBonuses bonuses, long now) {
        var keep = Math.min(bonuses.get(ArmorEffectType.ARMOR_DURABILITY), 0.9F);
        if (keep <= 0F || state.snapshotTick != now) {
            return;
        }
        var random = player.getRandom();
        for (var piece : ArmorPiece.all()) {
            var index = piece.ordinal();
            var stack = player.getItemBySlot(piece.slot());
            var before = state.snapshotDamage[index];
            var snapshot = state.snapshotStacks[index];
            state.snapshotStacks[index] = null;
            if (before < 0 || stack.isEmpty() || stack != snapshot || !stack.isDamageableItem()) {
                continue;
            }
            var lost = stack.getDamageValue() - before;
            if (lost <= 0) {
                continue;
            }
            var saved = lost * keep;
            var restore = (int) saved + (random.nextFloat() < saved - (int) saved ? 1 : 0);
            stack.setDamageValue(Math.max(before, stack.getDamageValue() - restore));
        }
        state.snapshotTick = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var bonus = ArmorBonuses.of(player).get(ArmorEffectType.HEALING_RECEIVED);
            if (bonus > 0F) {
                event.setAmount(event.getAmount() * (1F + bonus));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            var chance = ArmorBonuses.of(player).get(ArmorEffectType.LIFE_SAVING);
            var state = state(player);
            var now = player.level().getGameTime();
            if (
                chance > 0F && now >= state.lifeSavingReady && player.getRandom().nextFloat() < Math.min(1F, chance)
                    && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            ) {
                event.setCanceled(true);
                state.lifeSavingReady = now + LIFE_SAVING_COOLDOWN;
                player.setHealth(player.getMaxHealth() * 0.3F);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                player.level()
                    .playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
                return;
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity()) {
            var adrenal = ArmorBonuses.of(killer).get(ArmorEffectType.ADRENAL);
            if (adrenal > 0F) {
                killer.addEffect(new MobEffectInstance(MobEffects.SPEED, Math.round(adrenal * 20F), 0));
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (
            event.getEntity() instanceof Player player
                && ArmorBonuses.of(player).immuneTo(event.getEffectInstance().getEffect())
        ) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEffectAdded(MobEffectEvent.Added event) {
        var instance = event.getEffectInstance();
        if (
            !(event.getEntity() instanceof ServerPlayer player) || instance.isAmbient() || instance.isInfiniteDuration()
                || !instance.getEffect().value().isBeneficial()
        ) {
            return;
        }
        var bonus = ArmorBonuses.of(player).get(ArmorEffectType.CHEM_DURATION);
        if (bonus <= 0F) {
            return;
        }
        instance.update(
            new MobEffectInstance(
                instance.getEffect(),
                Math.round(instance.getDuration() * (1F + bonus)),
                instance.getAmplifier(),
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()
            )
        );
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var bonuses = ArmorBonuses.of(player);
        if (bonuses.isEmpty()) {
            return;
        }
        var stealth = 0F;
        if (player.isCrouching()) {
            stealth += bonuses.get(ArmorEffectType.STEALTH);
        }
        if (player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 7) {
            stealth += bonuses.get(ArmorEffectType.STEALTH_DARK);
        }
        if (isNight(player)) {
            stealth += bonuses.get(ArmorEffectType.STEALTH_NIGHT);
        }
        if (stealth > 0F) {
            event.modifyVisibility(1.0 - Math.clamp(stealth, 0F, 0.8F));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var inPowerArmor = player.getVehicle() instanceof PowerArmorEntity;
        if (!inPowerArmor && player.getData(ADAttachments.STOWED_STABILIZED.get()) != 0F) {
            player.setData(ADAttachments.STOWED_STABILIZED.get(), 0F);
        }
        var bonuses = ArmorBonuses.of(player);
        if (bonuses.isEmpty()) {
            var state = WEARERS.remove(player.getUUID());
            if (state != null) {
                cleanup(player, state);
            }
            return;
        }
        var state = state(player);
        var now = player.level().getGameTime();

        var moved = Math.abs(player.getX() - state.lastX) + Math.abs(player.getZ() - state.lastZ) > 0.01;
        state.lastX = player.getX();
        state.lastZ = player.getZ();
        state.moving = moved;
        if (moved || !player.isCrouching() || state.stillSince == 0) {
            state.stillSince = now;
        }

        applyAttributes(player, bonuses);

        if (bonuses.has(ArmorEffectType.WARMING)) {
            player.setTicksFrozen(0);
        }
        tickExhaustion(player, state, bonuses.get(ArmorEffectType.EXHAUSTION));

        var magnet = bonuses.get(ArmorEffectType.ITEM_MAGNET);
        if (magnet > 0F && now % 5 == 0) {
            pullItems(player, magnet);
        }

        if (now % 5 == 0) {
            var hidden = (bonuses.has(ArmorEffectType.CHAMELEON) && now - state.stillSince >= CHAMELEON_STILL_TICKS)
                || now < state.cloakUntil;
            keepEffect(player, state, MobEffects.INVISIBILITY, hidden, 0, 40);
        }

        if (now % 20 != 0) {
            return;
        }
        var haste = Math.round(bonuses.get(ArmorEffectType.HASTE));
        keepEffect(player, state, MobEffects.HASTE, haste > 0, Math.max(0, haste - 1), 400);
        keepEffect(
            player,
            state,
            MobEffects.WATER_BREATHING,
            bonuses.has(ArmorEffectType.WATER_BREATHING),
            0,
            400
        );
        for (var effect : bonuses.immunities()) {
            if (player.hasEffect(effect)) {
                player.removeEffect(effect);
            }
        }
        var regen = bonuses.get(ArmorEffectType.REGENERATING);
        if (regen > 0F && now - state.lastCombat > COMBAT_TICKS && player.getHealth() < player.getMaxHealth()) {
            player.heal(regen);
        }
    }

    private static void tickExhaustion(ServerPlayer player, WearerState state, float exhaustion) {
        FoodDataAccessor accessor = (FoodDataAccessor) player.getFoodData();
        var current = accessor.atomicdawn$getExhaustionLevel();
        if (exhaustion > 0F && state.lastExhaustion >= 0F && current > state.lastExhaustion) {
            var gained = current - state.lastExhaustion;
            accessor.atomicdawn$setExhaustionLevel(Math.max(0F, current - gained * Math.min(0.9F, exhaustion)));
        }
        state.lastExhaustion = accessor.atomicdawn$getExhaustionLevel();
    }

    private static void applyAttributes(ServerPlayer player, ArmorBonuses bonuses) {
        setModifier(player, Attributes.ARMOR, ARMOR_MODIFIER, bonuses.get(ArmorEffectType.ARMOR), false);
        setModifier(
            player,
            Attributes.ARMOR_TOUGHNESS,
            TOUGHNESS_MODIFIER,
            bonuses.get(ArmorEffectType.TOUGHNESS),
            false
        );
        setModifier(
            player,
            Attributes.KNOCKBACK_RESISTANCE,
            KNOCKBACK_MODIFIER,
            Math.min(1F, bonuses.get(ArmorEffectType.KNOCKBACK_RESISTANCE)),
            false
        );
        setModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER, bonuses.get(ArmorEffectType.MAX_HEALTH), false);
        var weight = bonuses.weight() * (float) AtomicDawn.CONFIG.common.equipment.armor.ARMOR_WEIGHT_PENALTY_MULTIPLIER
            * PerkHooks.armorWeightMultiplier(player);
        var speed = Math.clamp(bonuses.get(ArmorEffectType.SPEED) - weight, -0.5F, 1F);
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, speed, true);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void setModifier(
        ServerPlayer player,
        Holder<Attribute> attribute,
        Identifier id,
        float amount,
        boolean multiplied
    ) {
        var instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        var existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 1.0E-4) {
            return;
        }
        if (existing == null && Math.abs(amount) < 1.0E-4) {
            return;
        }
        instance.removeModifier(id);
        if (Math.abs(amount) >= 1.0E-4) {
            instance.addTransientModifier(
                new AttributeModifier(
                    id,
                    amount,
                    multiplied
                        ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        : AttributeModifier.Operation.ADD_VALUE
                )
            );
        }
    }

    private static void pullItems(ServerPlayer player, float radius) {
        var center = player.position().add(0, 0.5, 0);
        for (var item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius))) {
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
        WearerState state,
        Holder<MobEffect> effect,
        boolean active,
        int amplifier,
        int duration
    ) {
        var current = player.getEffect(effect);
        if (active) {
            if (current == null || (state.granted.contains(effect) && current.getDuration() < duration * 2 / 3)) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
                state.granted.add(effect);
            }
        } else if (state.granted.remove(effect) && current != null && current.isAmbient()) {
            player.removeEffect(effect);
        }
    }

    private static void cleanup(ServerPlayer player, WearerState state) {
        for (var effect : state.granted) {
            var current = player.getEffect(effect);
            if (current != null && current.isAmbient()) {
                player.removeEffect(effect);
            }
        }
        applyAttributes(player, ArmorBonuses.NONE);
    }

    private static WearerState state(ServerPlayer player) {
        return WEARERS.computeIfAbsent(player.getUUID(), ignored -> new WearerState());
    }
}

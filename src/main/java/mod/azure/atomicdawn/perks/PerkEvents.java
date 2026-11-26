package mod.azure.atomicdawn.perks;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.helper.ADEntityUtil;
import mod.azure.atomicdawn.mixin.FoodDataAccessor;
import mod.azure.atomicdawn.party.PartyService;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.special.SpecialStats;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;
import mod.azure.atomicdawn.weapon.instance.Weapons;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class PerkEvents {

    private static final Identifier ARMOR_MODIFIER = AtomicDawn.id("perk_armor");

    private static final Identifier KNOCKBACK_MODIFIER = AtomicDawn.id("perk_knockback");

    private static final Identifier SPEED_MODIFIER = AtomicDawn.id("perk_speed");

    private static final Identifier ATTACK_SPEED_MODIFIER = AtomicDawn.id("perk_attack_speed");

    private static final float DODGE_AP_COST = 30F;

    private static final int PACIFY_CACHE_LIMIT = 4096;

    private static final Set<Holder<MobEffect>> DISEASES = Set.of(
        MobEffects.POISON,
        MobEffects.HUNGER,
        MobEffects.NAUSEA,
        MobEffects.WEAKNESS
    );

    private static final class PlayerState {

        long autoHealReady;

        long cheatDeathReady;

        long survivalAidNext = -1L;

        float lastExhaustion = -1F;

        long snapshotTick = Long.MIN_VALUE;

        final ItemStack[] snapshotStacks = new ItemStack[ArmorPiece.COUNT];

        final int[] snapshotDamage = new int[ArmorPiece.COUNT];

        final Set<Holder<MobEffect>> granted = new HashSet<>();
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private static final Map<UUID, Boolean> PACIFIED = new HashMap<>();

    private static boolean sharingHeal;

    private PerkEvents() {}

    private static PlayerState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
    }

    private static long cooldownTicks() {
        return AtomicDawn.CONFIG.common.progression.perks.cards.PERKS_PROC_COOLDOWN_SECONDS * 20L;
    }

    private static boolean isMelee(DamageSource source, LivingEntity victim) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypes.THORNS)
            && attacker.distanceToSqr(victim) < 36.0;
    }

    private static boolean isEnergy(DamageSource source) {
        return source.is(ADDamageTypes.ENERGY) || source.is(DamageTypeTags.WITCH_RESISTANT_TO);
    }

    private static WeaponCategory vanillaCategory(ServerPlayer attacker, DamageSource source) {
        if (source.getDirectEntity() instanceof Projectile) {
            return WeaponCategory.BOW;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return WeaponCategory.EXPLOSIVE;
        }
        if (source.getDirectEntity() == attacker && source.is(DamageTypes.PLAYER_ATTACK)) {
            var held = attacker.getMainHandItem();
            if (Weapons.definition(attacker.level(), held) != null) {
                return null;
            }
            return held.isEmpty() ? WeaponCategory.UNARMED : WeaponCategory.MELEE;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        var source = event.getSource();
        var now = level.getGameTime();

        var markBonus = PerkCombat.markBonus(victim, now);
        if (markBonus > 0F) {
            event.setAmount(event.getAmount() * (1F + markBonus));
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            var weaken = PerkCombat.markWeaken(attacker, now);
            if (weaken > 0F) {
                event.setAmount(event.getAmount() * (1F - Math.min(weaken, 0.9F)));
            }
        }

        if (
            source.getEntity() instanceof ServerPlayer attacker && attacker != victim && !PerkCombat.inWeaponHit()
                && !PerkCombat.inProc()
        ) {
            var category = vanillaCategory(attacker, source);
            if (category != null) {
                vanillaHit(event, level, attacker, victim, category);
            }
        }

        if (victim instanceof ServerPlayer player && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            defend(event, level, player, source, now);
        }
    }

    private static void vanillaHit(
        LivingIncomingDamageEvent event,
        ServerLevel level,
        ServerPlayer attacker,
        LivingEntity victim,
        WeaponCategory category
    ) {
        var stats = PerkHooks.stats(attacker);
        var multiplier = PerkCombat.outgoingMultiplier(attacker, victim, category, false, false);
        if (category == WeaponCategory.BOW || category == WeaponCategory.EXPLOSIVE) {
            multiplier *= 1F + stats.damageBonus(category, false, false);
        }
        var penetration = PerkCombat.armorPenetration(attacker, category, false);
        if (penetration > 0F) {
            multiplier *= 1F + penetration * Math.min(1F, victim.getArmorValue() / 20F);
        }
        event.setAmount(event.getAmount() * multiplier);
        PerkCombat.onHit(level, attacker, victim, category, false, false, event.getAmount(), victim.position());
    }

    private static void defend(
        LivingIncomingDamageEvent event,
        ServerLevel level,
        ServerPlayer player,
        DamageSource source,
        long now
    ) {
        var stats = PerkHooks.stats(player);
        snapshotDurability(player, now);
        if (source.is(DamageTypeTags.IS_FALL)) {
            var fall = stats.extra(PerkEffectType.REDUCE_FALL);
            if (fall > 0F) {
                event.setAmount(event.getAmount() * (1F - Math.min(fall, 0.95F)));
            }
            return;
        }
        var random = player.getRandom();
        if (PerkCombat.lowHealth(player) && PerkCombat.roll(random, stats.extra(PerkEffectType.DODGE_LOW_HEALTH))) {
            event.setCanceled(true);
            level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS,
                0.8F,
                1.2F
            );
            return;
        }
        var buffs = PerkCombat.buffs(player);
        if (player.isBlocking() && stats.extra(PerkEffectType.RETRIBUTION) > 0F) {
            buffs.retributionUntil = now + PerkCombat.RETRIBUTION_TICKS;
        }

        var reduction = stats.extra(PerkEffectType.DAMAGE_REDUCTION);
        var melee = isMelee(source, player);
        if (melee) {
            reduction += stats.extra(PerkEffectType.REDUCE_MELEE);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            reduction += stats.extra(PerkEffectType.REDUCE_PROJECTILE);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            reduction += stats.extra(PerkEffectType.REDUCE_EXPLOSION);
        }
        var fullSet = PerkHooks.fullArmorSet(player);
        if (source.is(DamageTypeTags.IS_FIRE)) {
            reduction += stats.extra(PerkEffectType.REDUCE_FIRE);
            if (fullSet) {
                reduction += stats.extra(PerkEffectType.SET_FIRE_RESISTANCE);
            }
        }
        var energy = isEnergy(source);
        if (energy) {
            reduction += stats.extra(PerkEffectType.REDUCE_ENERGY);
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER)) {
            reduction += stats.extra(PerkEffectType.REDUCE_POISON);
            if (fullSet) {
                reduction += stats.extra(PerkEffectType.SET_POISON_RESISTANCE);
            }
        }
        if (PerkCombat.lowHealth(player)) {
            reduction += stats.extra(PerkEffectType.LOW_HEALTH_REDUCTION);
        }
        if (PerkCombat.isNight(player)) {
            reduction += stats.extra(PerkEffectType.NIGHT_REDUCTION);
        }
        if (player.isSprinting()) {
            reduction += stats.extra(PerkEffectType.SPRINT_REDUCTION);
        }
        var mates = 0;
        for (var mate : PartyService.onlineMates(player)) {
            if (mate.level() == player.level() && mate.distanceToSqr(player) <= 32 * 32) {
                mates++;
            }
        }
        reduction += mates == 0
            ? stats.extra(PerkEffectType.SOLO_REDUCTION)
            : stats.extra(PerkEffectType.PARTY_REDUCTION) * Math.min(mates, 4);
        if (now < buffs.retributionUntil) {
            reduction += stats.extra(PerkEffectType.RETRIBUTION);
        }
        var dodge = stats.extra(PerkEffectType.DODGE_AP);
        if (dodge > 0F && VatsActions.actionPoints(player).points() >= DODGE_AP_COST) {
            VatsActions.spend(player, DODGE_AP_COST);
            reduction += dodge;
        }
        if (reduction > 0F) {
            var cap = (float) AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MAX_DAMAGE_REDUCTION;
            event.setAmount(event.getAmount() * (1F - Math.min(reduction, cap)));
        }

        if (energy) {
            var absorb = stats.extra(PerkEffectType.ENERGY_ABSORB);
            if (absorb > 0F) {
                if (player.getVehicle() instanceof PowerArmorEntity armor) {
                    armor.recharge(event.getAmount() * absorb * 10F);
                } else {
                    player.heal(event.getAmount() * absorb);
                }
            }
        }
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
            var mark = stats.extra(PerkEffectType.MARK_ATTACKER);
            if (mark > 0F) {
                PerkCombat.mark(attacker, mark, 0F, now);
            }
            if (
                source.is(DamageTypeTags.IS_PROJECTILE) && !PerkCombat.inProc()
                    && PerkCombat.roll(random, stats.extra(PerkEffectType.RICOCHET))
            ) {
                attacker.setInvulnerableTime(0);
                ADEntityUtil.hurt(attacker, level, level.damageSources().thorns(player), event.getAmount());
            }
        }
    }

    private static void snapshotDurability(ServerPlayer player, long now) {
        if (PerkHooks.armorConditionKeep(player) <= 0F) {
            return;
        }
        var state = state(player);
        state.snapshotTick = now;
        for (var piece : ArmorPiece.all()) {
            var stack = player.getItemBySlot(piece.slot());
            state.snapshotStacks[piece.ordinal()] = stack;
            state.snapshotDamage[piece.ordinal()] = stack.isDamageableItem() ? stack.getDamageValue() : -1;
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var state = STATES.get(player.getUUID());
        var now = player.level().getGameTime();
        if (state != null && state.snapshotTick == now) {
            restoreDurability(player, state);
        }
        if (event.getInflictedDamage() <= 0F || !player.isAlive()) {
            return;
        }
        var heal = PerkHooks.value(player, PerkEffectType.AUTO_HEAL);
        if (heal > 0F && PerkCombat.lowHealth(player)) {
            var current = state(player);
            if (now >= current.autoHealReady) {
                current.autoHealReady = now + cooldownTicks();
                player.heal(player.getMaxHealth() * Math.min(1F, heal));
                player.level()
                    .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.BREWING_STAND_BREW,
                        SoundSource.PLAYERS,
                        0.8F,
                        1.4F
                    );
            }
        }
    }

    private static void restoreDurability(ServerPlayer player, PlayerState state) {
        var keep = PerkHooks.armorConditionKeep(player);
        var random = player.getRandom();
        for (var piece : ArmorPiece.all()) {
            var index = piece.ordinal();
            var stack = player.getItemBySlot(piece.slot());
            var before = state.snapshotDamage[index];
            var snapshot = state.snapshotStacks[index];
            state.snapshotStacks[index] = null;
            if (keep <= 0F || before < 0 || stack.isEmpty() || stack != snapshot || !stack.isDamageableItem()) {
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        var stats = PerkHooks.stats(player);
        var state = state(player);
        var now = player.level().getGameTime();
        var random = player.getRandom();
        var reboot = player.getVehicle() instanceof PowerArmorEntity
            && PerkCombat.roll(random, stats.extra(PerkEffectType.PA_REBOOT));
        var cheat = now >= state.cheatDeathReady && PerkCombat.roll(random, stats.extra(PerkEffectType.CHEAT_DEATH));
        if (reboot || cheat) {
            event.setCanceled(true);
            if (cheat) {
                state.cheatDeathReady = now + cooldownTicks();
            }
            player.setHealth(player.getMaxHealth() * (reboot ? 0.5F : 0.25F));
            player.level()
                .playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.TOTEM_USE,
                    SoundSource.PLAYERS,
                    0.6F,
                    reboot ? 0.7F : 1.4F
                );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        var victim = event.getEntity();
        if (victim instanceof ServerPlayer player) {
            var laugh = PerkHooks.value(player, PerkEffectType.LAST_LAUGH);
            if (laugh > 0F) {
                PerkCombat.explode(level, player, player.position(), laugh, 4F);
            }
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim) {
            return;
        }
        var stats = PerkHooks.stats(killer);
        var now = level.getGameTime();
        var kh = stats.extra(PerkEffectType.KILL_HEAL);
        if (kh > 0F) {
            killer.heal(kh);
        }
        var kap = stats.extra(PerkEffectType.KILL_AP);
        if (kap > 0F) {
            var ap = VatsActions.actionPoints(killer);
            var max = VatsActions.maxActionPoints(killer);
            killer.setData(ADAttachments.ACTION_POINTS.get(), ap.withPoints(Math.min(max, ap.points() + kap), max));
        }
        var food = stats.extra(PerkEffectType.KILL_FOOD);
        if (food > 0F && !GhoulService.isGhoul(killer)) {
            killer.getFoodData().eat(Math.round(food), 0.3F);
        }
        var stability = stats.extra(PerkEffectType.KILL_STABILITY);
        if (stability > 0F) {
            GhoulService.restoreStability(killer, stability);
        }
        if (stats.has(PerkEffectType.KILL_DAMAGE)) {
            PerkCombat.buffs(killer).killDamageUntil = now + PerkCombat.BUFF_TICKS;
        }

        var category = killCategory(killer, victim, event.getSource(), now);
        if (category == null) {
            return;
        }
        var random = killer.getRandom();
        var power = Math.min(30F, 6F + victim.getMaxHealth() * 0.25F);
        var at = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        var chance = category.melee()
            ? stats.extra(PerkEffectType.MELEE_KILL_EXPLOSION)
            : category == WeaponCategory.EXPLOSIVE
                ? stats.extra(PerkEffectType.EXPLOSIVE_KILL_EXPLOSION)
                : stats.extra(PerkEffectType.RANGED_KILL_EXPLOSION);
        if (PerkCombat.roll(random, chance)) {
            PerkCombat.explode(level, killer, at, power, 3F);
        }
    }

    private static WeaponCategory killCategory(
        ServerPlayer killer,
        LivingEntity victim,
        DamageSource source,
        long now
    ) {
        if (source.is(ADDamageTypes.LEGENDARY_EXPLOSION)) {
            return WeaponCategory.EXPLOSIVE;
        }
        var hit = PerkCombat.LAST_HIT.get(killer.getUUID());
        if (hit != null && hit.targetId() == victim.getId() && now - hit.tick() <= 2) {
            return hit.category();
        }
        return vanillaCategory(killer, source);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isEndConquered()) {
            var minutes = PerkHooks.value(player, PerkEffectType.REVENANT);
            if (minutes > 0F) {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, Math.round(minutes * 1200F), 0));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
        PerkCombat.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0F) {
            return;
        }
        var stats = PerkHooks.stats(player);
        var bonus = stats.extra(PerkEffectType.HEALING_RECEIVED);
        if (bonus > 0F) {
            event.setAmount(event.getAmount() * (1F + bonus));
        }
        var share = stats.extra(PerkEffectType.TEAM_HEALING);
        if (share > 0F && !sharingHeal) {
            sharingHeal = true;
            try {
                for (var mate : PartyService.onlineMates(player)) {
                    if (mate.level() == player.level() && mate.distanceToSqr(player) <= 16 * 16) {
                        mate.heal(event.getAmount() * share);
                    }
                }
            } finally {
                sharingHeal = false;
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var effect = event.getEffectInstance().getEffect();
        if (!DISEASES.contains(effect)) {
            return;
        }
        var stats = PerkHooks.stats(player);
        var chance = stats.extra(PerkEffectType.DISEASE_RESISTANCE);
        if (effect.equals(MobEffects.POISON) && PerkHooks.fullArmorSet(player)) {
            chance += stats.extra(PerkEffectType.SET_POISON_RESISTANCE);
        }
        if (PerkCombat.roll(player.getRandom(), chance)) {
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
        var bonus = PerkHooks.value(player, PerkEffectType.CHEM_DURATION);
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
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var food = event.getItem().get(net.minecraft.core.component.DataComponents.FOOD);
        if (food == null) {
            return;
        }
        var bonus = PerkHooks.value(player, PerkEffectType.FOOD_BONUS);
        if (bonus > 0F) {
            player.getFoodData().eat(Math.max(1, Math.round(food.nutrition() * bonus)), food.saturation() * bonus);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var victim = event.getEntity();
        if (
            victim instanceof Player || !(event.getSource().getEntity() instanceof ServerPlayer killer)
                || !(victim.level() instanceof ServerLevel level)
        ) {
            return;
        }
        var chance = PerkHooks.value(killer, PerkEffectType.LOOT_BONUS);
        if (chance <= 0F) {
            return;
        }
        var random = killer.getRandom();
        var extra = new java.util.ArrayList<ItemEntity>();
        for (var drop : event.getDrops()) {
            if (PerkCombat.roll(random, chance)) {
                extra.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
        }
        event.getDrops().addAll(extra);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        var state = event.getState();
        if (!state.is(BlockTags.CROPS) && !state.is(BlockTags.LOGS)) {
            return;
        }
        var chance = PerkHooks.value(player, PerkEffectType.HARVEST_BONUS);
        if (chance <= 0F) {
            return;
        }
        var random = player.getRandom();
        var extra = new java.util.ArrayList<ItemEntity>();
        for (var drop : event.getDrops()) {
            if (PerkCombat.roll(random, chance)) {
                extra.add(
                    new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy())
                );
            }
        }
        event.getDrops().addAll(extra);
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PerkHooks.onCrafted(player, event.getCrafting());
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var type = event.getTarget().getType();
        if (type != EntityTypes.VILLAGER && type != EntityTypes.WANDERING_TRADER) {
            return;
        }
        var barter = Math.round(PerkHooks.value(player, PerkEffectType.BARTER));
        if (barter > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 600, barter - 1, true, false, true));
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (
            !(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) || !(event
                .getEntity() instanceof Mob mob)
                || mob.getMaxHealth() > 100F || mob.getLastHurtByMob() == player
        ) {
            return;
        }
        var chance = PerkHooks.value(player, PerkEffectType.PACIFY);
        if (chance <= 0F) {
            return;
        }
        if (PACIFIED.size() > PACIFY_CACHE_LIMIT) {
            PACIFIED.clear();
        }
        var pacified = PACIFIED.computeIfAbsent(mob.getUUID(), ignored -> PerkCombat.roll(player.getRandom(), chance));
        if (pacified) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isCrouching()) {
            return;
        }
        var stealth = PerkHooks.value(player, PerkEffectType.STEALTH);
        if (stealth > 0F) {
            event.modifyVisibility(1.0 - Math.clamp(stealth, 0F, 0.8F));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var tick = event.getServer().getTickCount();
        if (tick % 20 == 0) {
            PerkCombat.tickBleeds();
        }
        if (tick % 200 == 0) {
            PerkCombat.prune(event.getServer().overworld().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        var stats = PerkHooks.stats(player);
        var state = state(player);
        var now = player.level().getGameTime();

        tickExhaustion(player, state, stats.extra(PerkEffectType.HUNGER_REDUCTION));

        var magnet = stats.extra(PerkEffectType.ITEM_MAGNET);
        if (magnet > 0F && now % 5 == 0) {
            pullItems(player, magnet);
        }
        var painTrain = stats.extra(PerkEffectType.PAIN_TRAIN);
        if (
            painTrain > 0F && now % 10 == 0 && player.isSprinting() && player.getVehicle() instanceof PowerArmorEntity
        ) {
            painTrain(player, painTrain);
        }
        if (now % 10 == 0) {
            applyAttributes(player, stats);
        }
        if (now % 20 != 0) {
            return;
        }
        keepEffect(player, state, MobEffects.WATER_BREATHING, stats.extra(PerkEffectType.WATER_BREATHING) > 0F, 0);
        keepEffect(player, state, MobEffects.NIGHT_VISION, stats.extra(PerkEffectType.NIGHT_VISION) > 0F, 0);
        regenerate(player, stats);
        var teamSpeed = Math.round(stats.extra(PerkEffectType.TEAM_SPEED));
        if (teamSpeed > 0 && now % 40 == 0) {
            for (var mate : PartyService.onlineMates(player)) {
                if (mate.level() == player.level() && mate.distanceToSqr(player) <= 32 * 32) {
                    mate.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, teamSpeed - 1, true, false, true));
                }
            }
        }
        var aidMinutes = stats.extra(PerkEffectType.SURVIVAL_AID);
        if (aidMinutes > 0F) {
            var interval = Math.max(200L, Math.round(aidMinutes * 1200F));
            if (state.survivalAidNext < 0L) {
                state.survivalAidNext = now + interval;
            } else if (now >= state.survivalAidNext) {
                state.survivalAidNext = now + interval;
                if (countItem(player, Items.BREAD) < 16) {
                    PlayerItems.give(player, new ItemStack(Items.BREAD));
                }
            }
        } else {
            state.survivalAidNext = -1L;
        }
        var soloAp = stats.extra(PerkEffectType.SOLO_AP_REGEN);
        if (soloAp > 0F && PartyService.onlineMates(player).isEmpty()) {
            var ap = VatsActions.actionPoints(player);
            var max = VatsActions.maxActionPoints(player);
            if (ap.points() < max) {
                var gain = (float) AtomicDawn.CONFIG.common.combat.vats.AP_REGEN_PER_SECOND * soloAp;
                player.setData(
                    ADAttachments.ACTION_POINTS.get(),
                    ap.withPoints(Math.min(max, ap.points() + gain), max)
                );
            }
        }
    }

    private static int countItem(ServerPlayer player, Item item) {
        var total = 0;
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void regenerate(ServerPlayer player, SpecialStats stats) {
        if (player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        var level = player.level();
        var amount = stats.extra(PerkEffectType.REGEN);
        var pos = player.blockPosition();
        if (!PerkCombat.isNight(player) && level.dimensionType().hasSkyLight() && level.canSeeSky(pos)) {
            amount += stats.extra(PerkEffectType.REGEN_DAY);
        }
        if (level.isRainingAt(pos.above())) {
            amount += stats.extra(PerkEffectType.REGEN_RAIN);
        }
        if (RadiationService.rads(player) > 0F) {
            amount += stats.extra(PerkEffectType.REGEN_IRRADIATED);
        }
        if (amount > 0F) {
            player.heal(amount);
        }
    }

    private static void tickExhaustion(ServerPlayer player, PlayerState state, float reduction) {
        FoodDataAccessor accessor = (FoodDataAccessor) player.getFoodData();
        var current = accessor.atomicdawn$getExhaustionLevel();
        if (reduction > 0F && state.lastExhaustion >= 0F && current > state.lastExhaustion) {
            var gained = current - state.lastExhaustion;
            accessor.atomicdawn$setExhaustionLevel(Math.max(0F, current - gained * Math.min(0.9F, reduction)));
        }
        state.lastExhaustion = accessor.atomicdawn$getExhaustionLevel();
    }

    private static void painTrain(ServerPlayer player, float damage) {
        var level = player.level();
        var look = player.getLookAngle();
        var center = player.position().add(look.x * 1.5, 0.5, look.z * 1.5);
        var area = new net.minecraft.world.phys.AABB(center, center).inflate(1.5);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, area, e -> PerkCombat.isFoe(player, e))) {
            if (target.getVehicle() == player || target == player.getVehicle()) {
                continue;
            }
            ADEntityUtil.hurt(target, level, level.damageSources().playerAttack(player), damage);
            ADEntityUtil.knockback(target, 1.2, -look.x, -look.z, target.damageSources().generic(), 0.0F);
        }
    }

    private static void applyAttributes(ServerPlayer player, mod.azure.atomicdawn.special.SpecialStats stats) {
        setModifier(player, Attributes.ARMOR, ARMOR_MODIFIER, stats.extra(PerkEffectType.ARMOR), false);
        setModifier(
            player,
            Attributes.KNOCKBACK_RESISTANCE,
            KNOCKBACK_MODIFIER,
            Math.min(1F, stats.extra(PerkEffectType.KNOCKBACK_RESISTANCE)),
            false
        );
        var speed = stats.extra(PerkEffectType.MOVEMENT_SPEED);
        if (PerkCombat.lowHealth(player)) {
            speed += stats.extra(PerkEffectType.LOW_HEALTH_SPEED);
        }
        speed = Math.clamp(speed, 0F, (float) AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MAX_SPEED_BONUS);
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, speed, true);
        var attackSpeed = Math.clamp(stats.value(PerkEffectType.ATTACK_SPEED, meleeCategory(player)), 0F, 1F);
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, attackSpeed, true);
    }

    private static WeaponCategory meleeCategory(ServerPlayer player) {
        return player.getMainHandItem().isEmpty() ? WeaponCategory.UNARMED : WeaponCategory.MELEE;
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
            Vec3 pull = center.subtract(item.position());
            if (pull.lengthSqr() > 0.25) {
                item.setDeltaMovement(pull.normalize().scale(0.35).add(0, 0.04, 0));
            }
        }
    }

    private static void keepEffect(
        ServerPlayer player,
        PlayerState state,
        Holder<MobEffect> effect,
        boolean active,
        int amplifier
    ) {
        var current = player.getEffect(effect);
        var duration = effect.equals(MobEffects.NIGHT_VISION) ? 400 : 200;
        if (active) {
            if (current == null || (state.granted.contains(effect) && current.getDuration() < duration * 2 / 3)) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
                state.granted.add(effect);
            }
        } else if (state.granted.remove(effect) && current != null && current.isAmbient()) {
            player.removeEffect(effect);
        }
    }
}

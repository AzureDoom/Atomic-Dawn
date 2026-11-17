package mod.azure.atomicdawn.special;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulPerks;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.ghoul.PlayerForm;
import mod.azure.atomicdawn.item.NukaColaEffects;
import mod.azure.atomicdawn.network.SpecialActionPayload;
import mod.azure.atomicdawn.party.PartyService;
import mod.azure.atomicdawn.perks.LegendaryPerkCatalog;
import mod.azure.atomicdawn.perks.PerkCatalog;
import mod.azure.atomicdawn.perks.PerkDefinition;
import mod.azure.atomicdawn.pipboy.PipBoyNotifications;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.util.HealthClamp;
import mod.azure.atomicdawn.weapon.action.VatsActions;

public final class SpecialService {

    public static final TagKey<Block> LOADOUT_STATION = TagKey.create(
        Registries.BLOCK,
        AtomicDawn.id("loadout_station")
    );

    private static final Identifier HEALTH_MODIFIER = AtomicDawn.id("special_max_health");

    private static final int ACTIONS_PER_SECOND = 12;

    private static final Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();

    private static final Map<UUID, long[]> RATE = new ConcurrentHashMap<>();

    private static final Map<UUID, long[]> KILL_XP_BUDGET = new ConcurrentHashMap<>();

    private static final Set<Identifier> MOVED_TO_LEGENDARY = Set.of(AtomicDawn.id("what_rads"));

    private SpecialService() {}

    public static SpecialProgress get(ServerPlayer player) {
        return player.getData(ADAttachments.SPECIAL_PROGRESS.get());
    }

    public static void set(ServerPlayer player, SpecialProgress progress) {
        player.setData(ADAttachments.SPECIAL_PROGRESS.get(), progress);
        refresh(player);
    }

    public static void refresh(ServerPlayer player) {
        var stats = SpecialStatResolver.resolve(player);
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(HEALTH_MODIFIER);
            if (stats.maxHealthBonus > 0F) {
                attribute.addPermanentModifier(
                    new AttributeModifier(HEALTH_MODIFIER, stats.maxHealthBonus, AttributeModifier.Operation.ADD_VALUE)
                );
            }
        }

        RadiationService.updateMaxHealth(player);
        HealthClamp.clampToMax(player);
        var ap = VatsActions.actionPoints(player);
        var maxAp = VatsActions.maxActionPoints(player);
        if (ap.points() > maxAp) {
            player.setData(ADAttachments.ACTION_POINTS.get(), ap.withPoints(ap.points(), maxAp));
        }
    }

    public static int unlockedLegendarySlots(int level) {
        if (!AtomicDawn.CONFIG.perks.LEGENDARY_ENABLED) {
            return 0;
        }
        var count = 0;
        var thresholds = AtomicDawn.CONFIG.perks.LEGENDARY_SLOT_LEVELS;
        if (thresholds == null) {
            return 0;
        }
        for (var threshold : thresholds) {
            if (threshold <= level) {
                count++;
            }
        }
        return Math.min(count, LegendaryProgress.MAX_SLOTS);
    }

    public static int nextLegendarySlotLevel(int level) {
        var next = Integer.MAX_VALUE;
        var thresholds = AtomicDawn.CONFIG.perks.LEGENDARY_SLOT_LEVELS;
        if (thresholds != null) {
            for (var threshold : thresholds) {
                if (threshold > level) {
                    next = Math.min(next, threshold);
                }
            }
        }
        return next == Integer.MAX_VALUE ? -1 : next;
    }

    private static SpecialProgress migrate(RegistryAccess access, SpecialProgress progress) {
        if (progress.version() >= SpecialProgress.CURRENT_VERSION) {
            return progress;
        }
        var owned = new HashMap<>(progress.owned());
        var legendary = progress.legendary();
        var loadouts = new ArrayList<SpecialLoadout>();
        for (var loadout : progress.loadouts()) {
            var equipped = new HashMap<>(loadout.equipped());
            equipped.keySet().removeAll(MOVED_TO_LEGENDARY);
            loadouts.add(loadout.withEquipped(equipped));
        }
        for (var id : MOVED_TO_LEGENDARY) {
            var rank = owned.getOrDefault(id, 0);
            if (rank <= 0 || PerkCatalog.get(access, id).isPresent()) {
                continue;
            }
            owned.remove(id);
            if (LegendaryPerkCatalog.get(access, id).isPresent() && legendary.ownedRank(id) <= 0) {
                legendary = legendary.withOwned(id, 1);
            }
            legendary = legendary.addCoins(rank * AtomicDawn.CONFIG.perks.LEGENDARY_COINS_PER_SCRAPPED_RANK);
        }
        return progress.withOwnedAndLegendary(owned, legendary)
            .withLoadouts(loadouts, progress.active())
            .withVersion(SpecialProgress.CURRENT_VERSION);
    }

    public static void onFormChanged(ServerPlayer player) {
        var progress = get(player);
        var sanitized = sanitizeAll(player.level().registryAccess(), progress, GhoulService.form(player));
        if (sanitized.removed() > 0) {
            PipBoyNotifications.send(player, PipBoyNotifications.Kind.CARDS_UNEQUIPPED, sanitized.removed());
        }
        set(player, sanitized.progress());
    }

    public static void ensureInitialized(ServerPlayer player) {
        var access = player.level().registryAccess();
        var progress = migrate(access, get(player));
        var start = AtomicDawn.CONFIG.special.SPECIAL_STARTING_POINTS;
        if (!progress.initialized()) {
            var owned = new HashMap<Identifier, Integer>();
            for (var entry : AtomicDawn.CONFIG.perks.PERKS_STARTER_CARDS) {
                parseStarter(entry).ifPresent(starter -> {
                    var definition = PerkCatalog.get(access, starter.id()).orElse(null);
                    if (definition != null) {
                        owned.merge(starter.id(), Math.min(starter.rank(), definition.maxRank()), Math::max);
                    } else {
                        AtomicDawn.LOGGER.warn("Starter perk '{}' is not a loaded perk card; skipping", entry);
                    }
                });
            }
            var loadouts = new ArrayList<SpecialLoadout>();
            for (var i = 0; i < AtomicDawn.CONFIG.special.SPECIAL_LOADOUT_SLOTS; i++) {
                loadouts.add(SpecialLoadout.fresh(start));
            }
            progress = progress.withInitialized(owned, loadouts, AtomicDawn.CONFIG.perks.PERKS_STARTING_PICKS);
        }
        if (progress.loadouts().size() < AtomicDawn.CONFIG.special.SPECIAL_LOADOUT_SLOTS) {
            var loadouts = new ArrayList<>(progress.loadouts());
            while (
                loadouts.size() < Math.min(
                    AtomicDawn.CONFIG.special.SPECIAL_LOADOUT_SLOTS,
                    SpecialProgress.MAX_LOADOUTS
                )
            ) {
                loadouts.add(SpecialLoadout.fresh(start));
            }
            progress = progress.withLoadouts(loadouts, progress.active());
        }
        var sanitized = sanitizeAll(access, progress, GhoulService.form(player));
        if (sanitized.removed() > 0) {
            PipBoyNotifications.send(player, PipBoyNotifications.Kind.CARDS_UNEQUIPPED, sanitized.removed());
        }
        set(player, sanitized.progress());
    }

    private record Starter(
        Identifier id,
        int rank
    ) {}

    private static Optional<Starter> parseStarter(String raw) {
        var at = raw.indexOf('@');
        var idText = at >= 0 ? raw.substring(0, at) : raw;
        var rank = 1;
        if (at >= 0) {
            try {
                rank = Math.max(1, Integer.parseInt(raw.substring(at + 1).trim()));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        var id = Identifier.tryParse(idText.trim());
        return id == null ? Optional.empty() : Optional.of(new Starter(id, rank));
    }

    public record Sanitized(
        SpecialProgress progress,
        int removed
    ) {}

    public static Sanitized sanitizeAll(RegistryAccess access, SpecialProgress progress, PlayerForm form) {
        var legendaryFix = sanitizeLegendary(access, progress, form);
        if (legendaryFix.legendary() != progress.legendary()) {
            progress = progress.withLegendary(legendaryFix.legendary());
        }
        var removed = legendaryFix.removed();
        var legendarySpecial = SpecialStatResolver.legendarySpecial(access, progress, form);
        var loadouts = new ArrayList<SpecialLoadout>();
        var changed = false;
        for (var loadout : progress.loadouts()) {
            var result = sanitize(access, progress, loadout, form, legendarySpecial);
            removed += result.removed;
            changed |= result.loadout != loadout;
            loadouts.add(result.loadout);
        }
        return new Sanitized(changed ? progress.withLoadouts(loadouts, progress.active()) : progress, removed);
    }

    private record LegendaryFix(
        LegendaryProgress legendary,
        int removed
    ) {}

    private static LegendaryFix sanitizeLegendary(RegistryAccess access, SpecialProgress progress, PlayerForm form) {
        var legendary = progress.legendary();
        var original = legendary;
        var unlocked = unlockedLegendarySlots(progress.level());
        var removed = 0;
        for (var i = 0; i < legendary.slots().size(); i++) {
            var id = legendary.slot(i).orElse(null);
            if (id == null) {
                continue;
            }
            var definition = LegendaryPerkCatalog.get(access, id).orElse(null);
            var owned = legendary.ownedRank(id);
            var invalid = i >= unlocked || definition == null || owned <= 0
                || !GhoulPerks.eligible(definition.tags(), form) || definition.rank(1).minLevel() > progress.level();
            if (invalid) {
                legendary = legendary.clearSlot(i);
                removed++;
            }
        }
        for (var entry : legendary.owned().entrySet()) {
            var definition = LegendaryPerkCatalog.get(access, entry.getKey()).orElse(null);
            if (definition != null && entry.getValue() > definition.maxRank()) {
                legendary = legendary.withOwned(entry.getKey(), definition.maxRank());
            }
        }
        return new LegendaryFix(legendary.equals(original) ? original : legendary, removed);
    }

    private record LoadoutFix(
        SpecialLoadout loadout,
        int removed
    ) {}

    private static LoadoutFix sanitize(
        RegistryAccess access,
        SpecialProgress progress,
        SpecialLoadout loadout,
        PlayerForm form,
        int[] legendarySpecial
    ) {
        var start = AtomicDawn.CONFIG.special.SPECIAL_STARTING_POINTS;
        var maxBase = AtomicDawn.CONFIG.special.SPECIAL_MAX_BASE;
        var budget = start * SpecialAttribute.COUNT + progress.earnedPoints();
        var values = new ArrayList<>(loadout.special());
        var distributionValid = loadout.allocated() <= budget;
        for (var v : values) {
            distributionValid &= v >= 1 && v <= maxBase;
        }
        var result = distributionValid
            ? loadout
            : SpecialLoadout.fresh(start).withName(loadout.name()).withEquipped(loadout.equipped());

        var equipped = new TreeMap<>(result.equipped());
        var removed = 0;
        var groups = new HashMap<String, Identifier>();
        for (var it = equipped.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            var definition = PerkCatalog.get(access, entry.getKey()).orElse(null);
            var owned = progress.ownedRank(entry.getKey());
            if (
                definition == null || owned <= 0 || entry.getValue() <= 0 || !GhoulPerks.eligible(definition, form)
            ) {
                it.remove();
                removed++;
                continue;
            }
            var rank = Math.min(Math.min(entry.getValue(), owned), definition.maxRank());
            if (definition.rank(rank).minLevel() > progress.level()) {
                it.remove();
                removed++;
                continue;
            }
            if (
                definition.exclusiveGroup().isPresent() && groups.putIfAbsent(
                    definition.exclusiveGroup().get(),
                    entry.getKey()
                ) != null
            ) {
                it.remove();
                removed++;
                continue;
            }
            entry.setValue(rank);
        }
        for (var special : SpecialAttribute.all()) {
            while (true) {
                var probe = result.withEquipped(equipped);
                var used = SpecialStatResolver.capacityUsed(access, progress, probe, special);
                if (used <= SpecialStatResolver.capacity(probe, special, legendarySpecial)) {
                    break;
                }
                Identifier victim = null;
                var victimCost = -1;
                for (var entry : equipped.entrySet()) {
                    var definition = PerkCatalog.get(access, entry.getKey()).orElseThrow();
                    if (definition.special() != special) {
                        continue;
                    }
                    var cost = definition.cost(entry.getValue());
                    if (cost > victimCost) {
                        victim = entry.getKey();
                        victimCost = cost;
                    }
                }
                if (victim == null) {
                    break;
                }
                equipped.remove(victim);
                removed++;
            }
        }
        var fixed = result.withEquipped(equipped);
        var same = fixed.equals(loadout);
        return new LoadoutFix(same ? loadout : fixed, removed);
    }

    public static void markCombat(ServerPlayer player) {
        LAST_COMBAT.put(player.getUUID(), player.level().getGameTime());
    }

    public static boolean inCombat(ServerPlayer player, int seconds) {
        if (seconds <= 0) {
            return false;
        }
        var last = LAST_COMBAT.get(player.getUUID());
        return last != null && player.level().getGameTime() - last < seconds * 20L;
    }

    public static boolean atStation(ServerPlayer player) {
        var radius = AtomicDawn.CONFIG.special.SPECIAL_STATION_RADIUS;
        if (radius <= 0) {
            return false;
        }
        var level = player.level();
        var origin = player.blockPosition();
        for (
            var pos : BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius)
            )
        ) {
            if (level.getBlockState(pos).is(LOADOUT_STATION)) {
                return true;
            }
        }
        return false;
    }

    private static SpecialResult canRebuild(ServerPlayer player) {
        if (player.isCreative() || atStation(player)) {
            return SpecialResult.OK;
        }
        if (!AtomicDawn.CONFIG.special.SPECIAL_ALLOW_ANYWHERE_SWITCH) {
            return SpecialResult.NEED_STATION;
        }
        return inCombat(player, AtomicDawn.CONFIG.special.SPECIAL_OUT_OF_COMBAT_SWITCH_SECONDS)
            ? SpecialResult.IN_COMBAT
            : SpecialResult.OK;
    }

    private static SpecialResult canEdit(ServerPlayer player) {
        if (player.isCreative()) {
            return SpecialResult.OK;
        }
        return inCombat(player, AtomicDawn.CONFIG.special.SPECIAL_BUILD_EDIT_COMBAT_LOCK_SECONDS)
            ? SpecialResult.IN_COMBAT
            : SpecialResult.OK;
    }

    private static boolean rateLimited(ServerPlayer player) {
        var now = player.level().getGameTime();
        var window = RATE.computeIfAbsent(player.getUUID(), _ -> new long[] { now, 0 });
        if (now - window[0] >= 20) {
            window[0] = now;
            window[1] = 0;
        }
        return ++window[1] > ACTIONS_PER_SECOND;
    }

    public static void forget(ServerPlayer player) {
        LAST_COMBAT.remove(player.getUUID());
        RATE.remove(player.getUUID());
        KILL_XP_BUDGET.remove(player.getUUID());
    }

    public static void awardXp(ServerPlayer player, long amount) {
        if (amount <= 0 || !AtomicDawn.CONFIG.special.SPECIAL_ENABLED) {
            return;
        }
        var stats = SpecialStatResolver.resolve(player);
        var bonus = stats.xpBonus + teamAura(player) + NukaColaEffects.xpBonus(player);
        var total = Math.round(amount * (1.0 + Math.min(bonus, AtomicDawn.CONFIG.perks.PERKS_MAX_XP_BONUS)));
        addXp(player, total);
    }

    private static float teamAura(ServerPlayer player) {
        if (!AtomicDawn.CONFIG.perks.PERKS_TEAM_AURAS) {
            return 0F;
        }
        var radius = AtomicDawn.CONFIG.perks.PERKS_TEAM_AURA_RADIUS;
        var best = 0F;
        for (var other : player.level().players()) {
            var ally = player.isAlliedTo(other) || PartyService.sameParty(player, other);
            if (other == player || !other.isAlive() || !ally || other.distanceToSqr(player) > radius * radius) {
                continue;
            }

            best = Math.max(best, SpecialStatResolver.resolve(other).teamXpBonus);
        }
        return best;
    }

    public static void addXp(ServerPlayer player, long amount) {
        if (amount <= 0) {
            return;
        }
        var progress = get(player);
        var xp = progress.xp() > Long.MAX_VALUE - amount ? Long.MAX_VALUE : progress.xp() + amount;
        var newLevel = SpecialXp.levelFor(xp, AtomicDawn.CONFIG.special.SPECIAL_MAX_LEVEL);
        var oldLevel = progress.level();
        var earned = progress.earnedPoints();
        var picks = progress.perkPicks();
        var pointCap = Math.max(0, AtomicDawn.CONFIG.special.SPECIAL_ATTRIBUTE_LEVEL_CAP - 1)
            * AtomicDawn.CONFIG.special.SPECIAL_POINTS_PER_LEVEL;
        for (var level = oldLevel + 1; level <= newLevel; level++) {
            if (level <= AtomicDawn.CONFIG.special.SPECIAL_ATTRIBUTE_LEVEL_CAP) {
                earned = Math.min(pointCap, earned + AtomicDawn.CONFIG.special.SPECIAL_POINTS_PER_LEVEL);
            }
            picks++;
        }
        set(player, progress.withXp(xp, Math.max(oldLevel, newLevel), earned, picks));
        if (newLevel > oldLevel) {
            PipBoyNotifications.send(player, PipBoyNotifications.Kind.LEVEL_UP, newLevel);
            PipBoyNotifications.send(player, PipBoyNotifications.Kind.PERK_PICKS, picks);
            var slots = unlockedLegendarySlots(newLevel);
            if (slots > unlockedLegendarySlots(oldLevel)) {
                PipBoyNotifications.send(player, PipBoyNotifications.Kind.LEGENDARY_SLOT, slots);
            }
        }
    }

    public static void awardKillXp(ServerPlayer killer, LivingEntity victim) {
        var amount = AtomicDawn.CONFIG.special.XP_KILL_BASE + victim.getMaxHealth()
            * AtomicDawn.CONFIG.special.XP_KILL_PER_HEALTH;
        var cap = AtomicDawn.CONFIG.special.XP_KILL_PER_MINUTE_CAP;
        var granted = Math.round(amount);
        if (cap > 0) {
            var now = killer.level().getGameTime();
            var budget = KILL_XP_BUDGET.computeIfAbsent(killer.getUUID(), _ -> new long[] { now, 0 });
            if (now - budget[0] >= 1200) {
                budget[0] = now;
                budget[1] = 0;
            }
            granted = Math.clamp(granted, 0, cap - budget[1]);
            budget[1] += granted;
        }
        awardXp(killer, granted);
    }

    public static SpecialResult handle(ServerPlayer player, SpecialActionPayload payload) {
        if (!AtomicDawn.CONFIG.special.SPECIAL_ENABLED) {
            return SpecialResult.DISABLED;
        }
        if (rateLimited(player)) {
            return SpecialResult.RATE_LIMITED;
        }
        var progress = get(player);
        if (payload.revision() != progress.revision()) {
            return SpecialResult.STALE;
        }
        return switch (payload.action()) {
            case ALLOCATE -> allocate(player, progress, payload.special());
            case LEARN_PERK -> learn(player, progress, payload.perk());
            case EQUIP_PERK -> equip(player, progress, payload.perk(), payload.rank());
            case UNEQUIP_PERK -> unequip(player, progress, payload.perk());
            case SWITCH_LOADOUT -> switchLoadout(player, progress, payload.loadout());
            case RENAME_LOADOUT -> rename(player, progress, payload.loadout(), payload.name());
            case SCRAP_PERK -> scrap(player, progress, payload.perk(), payload.rank());
            case LEGENDARY_EQUIP -> legendaryEquip(player, progress, payload.perk(), payload.rank());
            case LEGENDARY_UNEQUIP -> legendaryUnequip(player, progress, payload.perk());
            case LEGENDARY_UPGRADE -> legendaryUpgrade(player, progress, payload.perk(), payload.rank());
        };
    }

    private static SpecialResult allocate(ServerPlayer player, SpecialProgress progress, List<Integer> requested) {
        if (requested.size() != SpecialAttribute.COUNT) {
            return SpecialResult.INVALID;
        }
        var current = progress.activeLoadout();
        var maxBase = AtomicDawn.CONFIG.special.SPECIAL_MAX_BASE;
        var budget = AtomicDawn.CONFIG.special.SPECIAL_STARTING_POINTS * SpecialAttribute.COUNT + progress
            .earnedPoints();
        var total = 0;
        var decreased = false;
        var changed = false;
        for (var attribute : SpecialAttribute.all()) {
            var value = requested.get(attribute.ordinal());
            if (value < 1) {
                return SpecialResult.INVALID;
            }
            if (value > maxBase) {
                return SpecialResult.ATTRIBUTE_CAP;
            }
            total += value;
            decreased |= value < current.base(attribute);
            changed |= value != current.base(attribute);
        }
        if (!changed) {
            return SpecialResult.NO_CHANGE;
        }
        if (total > budget) {
            return SpecialResult.NO_POINTS;
        }

        var gate = decreased ? canRebuild(player) : canEdit(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        var updated = progress.withActiveLoadout(current.withSpecial(requested));
        var sanitized = sanitizeAll(player.level().registryAccess(), updated, GhoulService.form(player));
        set(player, sanitized.progress());
        return sanitized.removed() > 0 ? SpecialResult.CARDS_UNEQUIPPED : SpecialResult.OK;
    }

    private static SpecialResult learn(ServerPlayer player, SpecialProgress progress, Identifier perk) {
        if (!AtomicDawn.CONFIG.perks.PERKS_ENABLED) {
            return SpecialResult.DISABLED;
        }
        if (progress.perkPicks() <= 0) {
            return SpecialResult.NO_PERK_PICKS;
        }
        var definition = PerkCatalog.get(player.level().registryAccess(), perk).orElse(null);
        if (definition == null) {
            return SpecialResult.UNKNOWN_PERK;
        }
        var owned = progress.ownedRank(perk);
        if (owned >= definition.maxRank()) {
            return SpecialResult.MAX_RANK;
        }
        if (definition.rank(owned + 1).minLevel() > progress.level()) {
            return SpecialResult.LEVEL_TOO_LOW;
        }
        if (progress.owned().size() >= SpecialProgress.MAX_OWNED && owned == 0) {
            return SpecialResult.INVALID;
        }
        var copy = new HashMap<>(progress.owned());
        copy.put(perk, owned + 1);
        set(player, progress.withOwned(copy, progress.perkPicks() - 1));
        return SpecialResult.OK;
    }

    private static SpecialResult equip(ServerPlayer player, SpecialProgress progress, Identifier perk, int rank) {
        if (!AtomicDawn.CONFIG.perks.PERKS_ENABLED) {
            return SpecialResult.DISABLED;
        }
        var access = player.level().registryAccess();
        var definition = PerkCatalog.get(access, perk).orElse(null);
        if (definition == null) {
            return SpecialResult.UNKNOWN_PERK;
        }
        if (rank < 1 || rank > definition.maxRank()) {
            return SpecialResult.INVALID;
        }
        if (!GhoulPerks.eligible(definition, GhoulService.form(player))) {
            return SpecialResult.WRONG_FORM;
        }
        if (progress.ownedRank(perk) < rank) {
            return SpecialResult.NOT_OWNED;
        }
        if (definition.rank(rank).minLevel() > progress.level()) {
            return SpecialResult.LEVEL_TOO_LOW;
        }
        var loadout = progress.activeLoadout();
        if (loadout.equipped().getOrDefault(perk, 0) == rank) {
            return SpecialResult.NO_CHANGE;
        }
        if (loadout.equipped().size() >= SpecialLoadout.MAX_EQUIPPED && !loadout.equipped().containsKey(perk)) {
            return SpecialResult.INVALID;
        }
        if (definition.exclusiveGroup().isPresent()) {
            var group = definition.exclusiveGroup().get();
            for (var other : loadout.equipped().keySet()) {
                if (other.equals(perk)) {
                    continue;
                }
                var otherDefinition = PerkCatalog.get(access, other);
                if (otherDefinition.flatMap(PerkDefinition::exclusiveGroup).map(group::equals).orElse(false)) {
                    return SpecialResult.EXCLUSIVE;
                }
            }
        }
        var candidate = loadout.equip(perk, rank);
        var used = SpecialStatResolver.capacityUsed(access, progress, candidate, definition.special());
        var capacity = SpecialStatResolver.capacity(
            access,
            progress,
            candidate,
            definition.special(),
            GhoulService.form(player)
        );
        if (used > capacity) {
            return SpecialResult.NO_CAPACITY;
        }
        var gate = canEdit(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        set(player, progress.withActiveLoadout(candidate));
        return SpecialResult.OK;
    }

    private static SpecialResult unequip(ServerPlayer player, SpecialProgress progress, Identifier perk) {
        var loadout = progress.activeLoadout();
        if (!loadout.equipped().containsKey(perk)) {
            return SpecialResult.NO_CHANGE;
        }
        var gate = canEdit(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        set(player, progress.withActiveLoadout(loadout.unequip(perk)));
        return SpecialResult.OK;
    }

    private static SpecialResult switchLoadout(ServerPlayer player, SpecialProgress progress, int index) {
        if (index < 0 || index >= progress.loadouts().size()) {
            return SpecialResult.INVALID;
        }
        if (index == progress.active()) {
            return SpecialResult.NO_CHANGE;
        }
        var gate = canRebuild(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        var sanitized = sanitizeAll(
            player.level().registryAccess(),
            progress.withActive(index),
            GhoulService.form(player)
        );
        set(player, sanitized.progress());
        return sanitized.removed() > 0 ? SpecialResult.CARDS_UNEQUIPPED : SpecialResult.OK;
    }

    private static SpecialResult rename(ServerPlayer player, SpecialProgress progress, int index, String name) {
        if (index < 0 || index >= progress.loadouts().size()) {
            return SpecialResult.INVALID;
        }
        var clean = SpecialLoadout.sanitizeName(name);
        var loadout = progress.loadouts().get(index);
        if (clean.equals(loadout.name())) {
            return SpecialResult.NO_CHANGE;
        }
        set(player, progress.withLoadout(index, loadout.withName(clean)));
        return SpecialResult.OK;
    }

    public static void reset(ServerPlayer player) {
        player.setData(ADAttachments.SPECIAL_PROGRESS.get(), SpecialProgress.EMPTY);
        ensureInitialized(player);
    }

    public static void respec(ServerPlayer player) {
        var progress = get(player);
        var start = AtomicDawn.CONFIG.special.SPECIAL_STARTING_POINTS;
        var fresh = SpecialLoadout.fresh(start).withName(progress.activeLoadout().name());
        set(player, progress.withActiveLoadout(fresh));
    }

    private static void commitSanitized(ServerPlayer player, SpecialProgress progress) {
        var sanitized = sanitizeAll(player.level().registryAccess(), progress, GhoulService.form(player));
        if (sanitized.removed() > 0) {
            PipBoyNotifications.send(player, PipBoyNotifications.Kind.CARDS_UNEQUIPPED, sanitized.removed());
        }
        set(player, sanitized.progress());
    }

    public static void grantPerkPicks(ServerPlayer player, int amount) {
        var progress = get(player);
        set(player, progress.withPerkPicks((int) Math.min(Integer.MAX_VALUE, (long) progress.perkPicks() + amount)));
    }

    public static boolean grantPerk(ServerPlayer player, Identifier perk, int rank) {
        var definition = PerkCatalog.get(player.level().registryAccess(), perk).orElse(null);
        if (definition == null) {
            return false;
        }
        var progress = get(player);
        if (progress.owned().size() >= SpecialProgress.MAX_OWNED && progress.ownedRank(perk) == 0) {
            return false;
        }
        var copy = new HashMap<>(progress.owned());
        copy.put(perk, Math.clamp(rank, 1, definition.maxRank()));
        commitSanitized(player, progress.withOwned(copy, progress.perkPicks()));
        return true;
    }

    public static boolean grantLegendary(ServerPlayer player, Identifier perk, int rank) {
        var definition = LegendaryPerkCatalog.get(player.level().registryAccess(), perk).orElse(null);
        if (definition == null) {
            return false;
        }
        var progress = get(player);
        var legendary = progress.legendary();
        if (legendary.owned().size() >= LegendaryProgress.MAX_OWNED && legendary.ownedRank(perk) == 0) {
            return false;
        }
        commitSanitized(
            player,
            progress.withLegendary(legendary.withOwned(perk, Math.clamp(rank, 1, definition.maxRank())))
        );
        return true;
    }

    public static void grantLegendaryCoins(ServerPlayer player, int amount) {
        var progress = get(player);
        set(player, progress.withLegendary(progress.legendary().addCoins(amount)));
    }

    private static boolean equippedAnywhere(SpecialProgress progress, Identifier perk, int keptRank) {
        for (var loadout : progress.loadouts()) {
            if (loadout.equipped().getOrDefault(perk, 0) > keptRank) {
                return true;
            }
        }
        return false;
    }

    private static SpecialResult scrap(ServerPlayer player, SpecialProgress progress, Identifier perk, int ranks) {
        if (!AtomicDawn.CONFIG.perks.PERKS_ENABLED || !AtomicDawn.CONFIG.perks.LEGENDARY_ENABLED) {
            return SpecialResult.DISABLED;
        }
        var owned = progress.ownedRank(perk);
        if (owned <= 0) {
            return SpecialResult.NOT_OWNED;
        }
        if (ranks < 1 || ranks > owned) {
            return SpecialResult.INVALID;
        }
        if (PerkCatalog.get(player.level().registryAccess(), perk).isEmpty()) {
            return SpecialResult.UNKNOWN_PERK;
        }
        var remaining = owned - ranks;
        if (equippedAnywhere(progress, perk, remaining)) {
            return SpecialResult.PROTECTED;
        }
        var gate = canEdit(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        var copy = new HashMap<>(progress.owned());
        if (remaining > 0) {
            copy.put(perk, remaining);
        } else {
            copy.remove(perk);
        }
        var coins = ranks * AtomicDawn.CONFIG.perks.LEGENDARY_COINS_PER_SCRAPPED_RANK;
        set(player, progress.withOwnedAndLegendary(copy, progress.legendary().addCoins(coins)));
        return SpecialResult.OK;
    }

    private static SpecialResult legendaryEquip(
        ServerPlayer player,
        SpecialProgress progress,
        Identifier perk,
        int slot
    ) {
        if (!AtomicDawn.CONFIG.perks.PERKS_ENABLED || !AtomicDawn.CONFIG.perks.LEGENDARY_ENABLED) {
            return SpecialResult.DISABLED;
        }
        var definition = LegendaryPerkCatalog.get(player.level().registryAccess(), perk).orElse(null);
        if (definition == null) {
            return SpecialResult.UNKNOWN_PERK;
        }
        if (slot < 0 || slot >= LegendaryProgress.MAX_SLOTS) {
            return SpecialResult.INVALID;
        }
        if (slot >= unlockedLegendarySlots(progress.level())) {
            return SpecialResult.NO_SLOT;
        }
        if (!GhoulPerks.eligible(definition.tags(), GhoulService.form(player))) {
            return SpecialResult.WRONG_FORM;
        }
        if (definition.rank(1).minLevel() > progress.level()) {
            return SpecialResult.LEVEL_TOO_LOW;
        }
        var legendary = progress.legendary();
        if (legendary.slotOf(perk) == slot) {
            return SpecialResult.NO_CHANGE;
        }
        if (legendary.ownedRank(perk) <= 0 && legendary.owned().size() >= LegendaryProgress.MAX_OWNED) {
            return SpecialResult.INVALID;
        }
        var gate = canEdit(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        if (legendary.ownedRank(perk) <= 0) {
            legendary = legendary.withOwned(perk, 1);
        }
        commitSanitized(player, progress.withLegendary(legendary.withSlot(slot, perk)));
        return SpecialResult.OK;
    }

    private static SpecialResult legendaryUnequip(ServerPlayer player, SpecialProgress progress, Identifier perk) {
        var legendary = progress.legendary();
        var slot = legendary.slotOf(perk);
        if (slot < 0) {
            return SpecialResult.NO_CHANGE;
        }
        var gate = canEdit(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        commitSanitized(player, progress.withLegendary(legendary.clearSlot(slot)));
        return SpecialResult.OK;
    }

    private static SpecialResult legendaryUpgrade(
        ServerPlayer player,
        SpecialProgress progress,
        Identifier perk,
        int target
    ) {
        if (!AtomicDawn.CONFIG.perks.PERKS_ENABLED || !AtomicDawn.CONFIG.perks.LEGENDARY_ENABLED) {
            return SpecialResult.DISABLED;
        }
        var definition = LegendaryPerkCatalog.get(player.level().registryAccess(), perk).orElse(null);
        if (definition == null) {
            return SpecialResult.UNKNOWN_PERK;
        }
        var legendary = progress.legendary();
        var owned = legendary.ownedRank(perk);
        if (owned <= 0) {
            return SpecialResult.NOT_OWNED;
        }
        if (owned >= definition.maxRank()) {
            return SpecialResult.MAX_RANK;
        }
        if (target != owned + 1) {
            return SpecialResult.STALE;
        }
        if (definition.rank(target).minLevel() > progress.level()) {
            return SpecialResult.LEVEL_TOO_LOW;
        }
        var cost = definition.upgradeCost(target);
        if (legendary.coins() < cost) {
            return SpecialResult.NO_COINS;
        }
        var gate = canEdit(player);
        if (gate != SpecialResult.OK) {
            return gate;
        }
        commitSanitized(player, progress.withLegendary(legendary.addCoins(-cost).withOwned(perk, target)));
        return SpecialResult.OK;
    }
}

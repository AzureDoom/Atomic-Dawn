package mod.azure.atomicdawn.special;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulPerks;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.ghoul.PlayerForm;
import mod.azure.atomicdawn.perks.LegendaryPerkCatalog;
import mod.azure.atomicdawn.perks.PerkCatalog;
import mod.azure.atomicdawn.perks.PerkEffect;
import mod.azure.atomicdawn.perks.PerkEffectType;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADRegistries;

public final class SpecialStatResolver {

    @FunctionalInterface
    public interface TemporarySource {

        int bonus(Player player, SpecialAttribute attribute);
    }

    private record CacheEntry(
        SpecialProgress progress,
        Object catalog,
        Object legendaryCatalog,
        int[] temporary,
        int configGeneration,
        PlayerForm form,
        SpecialStats stats
    ) {}

    private static final List<TemporarySource> TEMPORARY_SOURCES = new CopyOnWriteArrayList<>();

    private static final Map<Player, CacheEntry> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile int configGeneration;

    private SpecialStatResolver() {}

    public static void registerTemporarySource(TemporarySource source) {
        TEMPORARY_SOURCES.add(source);
    }

    public static void invalidateAll() {
        configGeneration++;
    }

    public static SpecialProgress progress(Player player) {
        return player.getData(ADAttachments.SPECIAL_PROGRESS.get());
    }

    public static SpecialStats resolve(Player player) {
        if (!AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_ENABLED) {
            return SpecialStats.NEUTRAL;
        }
        var progress = progress(player);
        var access = player.level().registryAccess();
        var catalog = access.lookup(ADRegistries.PERK).orElse(null);
        var legendaryCatalog = access.lookup(ADRegistries.LEGENDARY_PERK).orElse(null);
        var temporary = temporary(player);
        var generation = configGeneration;
        var form = GhoulService.form(player);
        var cached = CACHE.get(player);
        if (
            cached != null && cached.progress == progress && cached.catalog == catalog
                && cached.legendaryCatalog == legendaryCatalog && cached.configGeneration == generation
                && cached.form == form && Arrays.equals(cached.temporary, temporary)
        ) {
            return cached.stats;
        }
        var stats = compute(access, progress, temporary, form);
        CACHE.put(player, new CacheEntry(progress, catalog, legendaryCatalog, temporary, generation, form, stats));
        return stats;
    }

    private static int[] temporary(Player player) {
        var result = new int[SpecialAttribute.COUNT];
        if (TEMPORARY_SOURCES.isEmpty()) {
            return result;
        }
        for (var attribute : SpecialAttribute.all()) {
            var total = 0;
            for (var source : TEMPORARY_SOURCES) {
                total += source.bonus(player, attribute);
            }
            result[attribute.ordinal()] = total;
        }
        return result;
    }

    public static void forEachEffect(
        RegistryAccess access,
        SpecialProgress progress,
        SpecialLoadout loadout,
        PlayerForm form,
        Consumer<PerkEffect> consumer
    ) {
        if (!AtomicDawn.CONFIG.common.progression.perks.cards.PERKS_ENABLED) {
            return;
        }
        for (var entry : loadout.equipped().entrySet()) {
            var definition = PerkCatalog.get(access, entry.getKey()).orElse(null);
            if (definition == null || !GhoulPerks.eligible(definition.tags(), form)) {
                continue;
            }
            var rank = Math.min(Math.min(entry.getValue(), progress.ownedRank(entry.getKey())), definition.maxRank());
            if (rank > 0) {
                definition.rank(rank).effects().forEach(consumer);
            }
        }
        forEachLegendaryEffect(access, progress, form, consumer);
    }

    public static void forEachLegendaryEffect(
        RegistryAccess access,
        SpecialProgress progress,
        PlayerForm form,
        Consumer<PerkEffect> consumer
    ) {
        if (
            !AtomicDawn.CONFIG.common.progression.perks.cards.PERKS_ENABLED
                || !AtomicDawn.CONFIG.common.progression.perks.legendary.LEGENDARY_ENABLED
        ) {
            return;
        }
        var legendary = progress.legendary();
        var unlocked = SpecialService.unlockedLegendarySlots(progress.level());
        for (var i = 0; i < Math.min(unlocked, legendary.slots().size()); i++) {
            var id = legendary.slot(i).orElse(null);
            if (id == null) {
                continue;
            }
            var definition = LegendaryPerkCatalog.get(access, id).orElse(null);
            if (definition == null || !GhoulPerks.eligible(definition.tags(), form)) {
                continue;
            }
            var rank = Math.min(legendary.ownedRank(id), definition.maxRank());
            if (rank > 0 && definition.rank(rank).minLevel() <= progress.level()) {
                definition.rank(rank).effects().forEach(consumer);
            }
        }
    }

    public static int[] legendarySpecial(RegistryAccess access, SpecialProgress progress, PlayerForm form) {
        var result = new int[SpecialAttribute.COUNT];
        forEachLegendaryEffect(access, progress, form, effect -> {
            var attribute = effect.type().special();
            if (attribute != null) {
                result[attribute.ordinal()] += Math.round(effect.value());
            }
        });
        return result;
    }

    public static int capacity(
        RegistryAccess access,
        SpecialProgress progress,
        SpecialLoadout loadout,
        SpecialAttribute attribute,
        PlayerForm form
    ) {
        return capacity(loadout, attribute, legendarySpecial(access, progress, form));
    }

    public static int capacity(SpecialLoadout loadout, SpecialAttribute attribute, int[] legendary) {
        var base = loadout.base(attribute);
        var boosted = base + Math.max(0, legendary[attribute.ordinal()]);
        return Math.max(
            base,
            Math.min(AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_MAX_BASE, boosted)
        );
    }

    public static float equippedValue(Player player, PerkEffectType type) {
        if (!AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_ENABLED) {
            return 0F;
        }
        var progress = progress(player);
        var total = new float[1];
        forEachEffect(
            player.level().registryAccess(),
            progress,
            progress.activeLoadout(),
            GhoulService.form(player),
            effect -> {
                if (effect.type() == type) {
                    total[0] += effect.value();
                }
            }
        );
        return total[0];
    }

    public static SpecialStats compute(RegistryAccess access, SpecialProgress progress, int[] temporary) {
        return compute(access, progress, temporary, PlayerForm.HUMAN);
    }

    public static SpecialStats compute(
        RegistryAccess access,
        SpecialProgress progress,
        int[] temporary,
        PlayerForm form
    ) {
        var loadout = progress.activeLoadout();
        var start = AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_STARTING_POINTS;
        var maxEffective = AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_MAX_EFFECTIVE;

        var acc = new Accumulator();
        var used = new int[SpecialAttribute.COUNT];
        if (AtomicDawn.CONFIG.common.progression.perks.cards.PERKS_ENABLED) {
            for (var entry : loadout.equipped().entrySet()) {
                var definition = PerkCatalog.get(access, entry.getKey()).orElse(null);
                if (definition == null) {
                    continue;
                }
                var rank = Math.min(
                    Math.min(entry.getValue(), progress.ownedRank(entry.getKey())),
                    definition.maxRank()
                );
                if (rank <= 0) {
                    continue;
                }
                used[definition.special().ordinal()] += definition.cost(rank);
                if (GhoulPerks.eligible(definition.tags(), form)) {
                    definition.rank(rank).effects().forEach(acc::accept);
                }
            }
        }
        var legendary = new int[SpecialAttribute.COUNT];
        forEachLegendaryEffect(access, progress, form, effect -> {
            var attribute = effect.type().special();
            if (attribute != null) {
                legendary[attribute.ordinal()] += Math.round(effect.value());
            } else {
                acc.accept(effect);
            }
        });

        var base = new int[SpecialAttribute.COUNT];
        var effective = new int[SpecialAttribute.COUNT];
        var capacity = new int[SpecialAttribute.COUNT];
        for (var attribute : SpecialAttribute.all()) {
            var i = attribute.ordinal();
            base[i] = loadout.base(attribute);
            capacity[i] = capacity(loadout, attribute, legendary);
            effective[i] = Math.clamp(base[i] + temporary[i] + legendary[i] + acc.special[i], 1, maxEffective);
        }

        float melee = (float) (above(effective, SpecialAttribute.STRENGTH, start)
            * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_STRENGTH_MELEE_PER_POINT);
        float hitBonus = (float) (above(effective, SpecialAttribute.PERCEPTION, start)
            * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_PERCEPTION_HIT_CHANCE_PER_POINT)
            + acc.hitBonus;
        float maxHealth = (float) (above(effective, SpecialAttribute.ENDURANCE, start)
            * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_ENDURANCE_HEALTH_PER_POINT)
            + acc.maxHealth;
        float condition = (float) (above(effective, SpecialAttribute.INTELLIGENCE, start)
            * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_INTELLIGENCE_CONDITION_PER_POINT)
            + acc.condition;
        float maxAp = (float) (above(effective, SpecialAttribute.AGILITY, start)
            * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_AGILITY_AP_PER_POINT) + acc.maxAp;
        float critGain = (float) (above(effective, SpecialAttribute.LUCK, start)
            * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_LUCK_CRIT_GAIN_PER_POINT)
            + acc.critGain;

        return new SpecialStats(
            base,
            effective,
            capacity,
            used,
            Math.max(0F, maxAp),
            Math.max(0F, 1F + acc.apRegen),
            (float) Math.clamp(
                1F - acc.apCost,
                AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MIN_AP_COST_MULTIPLIER,
                1F
            ),
            (float) Math.clamp(hitBonus, 0F, AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MAX_VATS_HIT_BONUS),
            Math.max(0F, 1F + critGain),
            (float) Math.clamp(
                acc.critDamage,
                0F,
                AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MAX_CRITICAL_DAMAGE_BONUS
            ),
            Math.clamp(acc.critProc, 0F, 1F),
            Math.max(0F, maxHealth),
            (float) Math.clamp(
                acc.radiation,
                0F,
                AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MAX_RADIATION_RESISTANCE
            ),
            (float) Math.clamp(
                condition,
                0F,
                AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MAX_CONDITION_REDUCTION
            ),
            Math.max(0F, acc.xp),
            Math.max(0F, acc.teamXp),
            Math.max(0F, melee),
            acc.damage,
            acc.filtered,
            acc.extra
        );
    }

    private static final class Accumulator {

        float apRegen;

        float apCost;

        float critDamage;

        float critProc;

        float radiation;

        float xp;

        float teamXp;

        float hitBonus;

        float maxHealth;

        float condition;

        float maxAp;

        float critGain;

        final int[] special = new int[SpecialAttribute.COUNT];

        final List<PerkEffect> damage = new ArrayList<>();

        final List<PerkEffect> filtered = new ArrayList<>();

        final EnumMap<PerkEffectType, Float> extra = new EnumMap<>(PerkEffectType.class);

        void accept(PerkEffect effect) {
            var v = effect.value();
            var attribute = effect.type().special();
            if (attribute != null) {
                special[attribute.ordinal()] += Math.round(v);
                return;
            }
            switch (effect.type()) {
                case DAMAGE -> damage.add(effect);
                case CRITICAL_DAMAGE -> critDamage += v;
                case MAX_HEALTH -> maxHealth += v;
                case MAX_AP -> maxAp += v;
                case AP_REGEN -> apRegen += v;
                case VATS_AP_COST -> apCost += v;
                case VATS_HIT_CHANCE -> hitBonus += v;
                case CRITICAL_GAIN -> critGain += v;
                case CRITICAL_PROC -> critProc += v;
                case RADIATION_RESISTANCE -> radiation += v;
                case CONDITION_LOSS -> condition += v;
                case XP_BONUS -> xp += v;
                case TEAM_XP_BONUS -> teamXp = Math.max(teamXp, v);
                default -> {
                    if (effect.categories().isEmpty() && effect.automatic().isEmpty()) {
                        extra.merge(effect.type(), v, Float::sum);
                    } else {
                        filtered.add(effect);
                    }
                }
            }
        }
    }

    private static int above(int[] values, SpecialAttribute attribute, int start) {
        return Math.max(0, values[attribute.ordinal()] - start);
    }

    public static int capacityUsed(
        RegistryAccess access,
        SpecialProgress progress,
        SpecialLoadout loadout,
        SpecialAttribute special
    ) {
        var used = 0;
        for (var entry : loadout.equipped().entrySet()) {
            var definition = PerkCatalog.get(access, entry.getKey()).orElse(null);
            if (definition == null || definition.special() != special) {
                continue;
            }
            var rank = Math.min(Math.min(entry.getValue(), progress.ownedRank(entry.getKey())), definition.maxRank());
            if (rank > 0) {
                used += definition.cost(rank);
            }
        }
        return used;
    }
}

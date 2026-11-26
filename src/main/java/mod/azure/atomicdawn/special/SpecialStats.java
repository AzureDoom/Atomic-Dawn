package mod.azure.atomicdawn.special;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.perks.PerkEffect;
import mod.azure.atomicdawn.perks.PerkEffectType;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;

@SuppressWarnings("unused")
public final class SpecialStats {

    public static final SpecialStats NEUTRAL = new SpecialStats(
        new int[SpecialAttribute.COUNT],
        new int[SpecialAttribute.COUNT],
        new int[SpecialAttribute.COUNT],
        new int[SpecialAttribute.COUNT],
        0F,
        1F,
        1F,
        0F,
        1F,
        0F,
        0F,
        0F,
        0F,
        0F,
        0F,
        0F,
        0F,
        List.of(),
        List.of(),
        Map.of()
    );

    private final int[] base;

    private final int[] effective;

    private final int[] capacity;

    private final int[] capacityUsed;

    public final float maxApBonus;

    public final float apRegenMultiplier;

    public final float apCostMultiplier;

    public final float vatsHitBonus;

    public final float criticalGainMultiplier;

    public final float criticalDamageBonus;

    public final float criticalProcChance;

    public final float maxHealthBonus;

    public final float radiationResistance;

    public final float conditionReduction;

    public final float xpBonus;

    public final float teamXpBonus;

    public final float meleeBonus;

    private final List<PerkEffect> damageEffects;

    private final List<PerkEffect> filteredEffects;

    private final Map<PerkEffectType, Float> extra;

    SpecialStats(
        int[] base,
        int[] effective,
        int[] capacity,
        int[] capacityUsed,
        float maxApBonus,
        float apRegenMultiplier,
        float apCostMultiplier,
        float vatsHitBonus,
        float criticalGainMultiplier,
        float criticalDamageBonus,
        float criticalProcChance,
        float maxHealthBonus,
        float radiationResistance,
        float conditionReduction,
        float xpBonus,
        float teamXpBonus,
        float meleeBonus,
        List<PerkEffect> damageEffects,
        List<PerkEffect> filteredEffects,
        Map<PerkEffectType, Float> extra
    ) {
        this.base = base.clone();
        this.effective = effective.clone();
        this.capacity = capacity.clone();
        this.capacityUsed = capacityUsed.clone();
        this.maxApBonus = maxApBonus;
        this.apRegenMultiplier = apRegenMultiplier;
        this.apCostMultiplier = apCostMultiplier;
        this.vatsHitBonus = vatsHitBonus;
        this.criticalGainMultiplier = criticalGainMultiplier;
        this.criticalDamageBonus = criticalDamageBonus;
        this.criticalProcChance = criticalProcChance;
        this.maxHealthBonus = maxHealthBonus;
        this.radiationResistance = radiationResistance;
        this.conditionReduction = conditionReduction;
        this.xpBonus = xpBonus;
        this.teamXpBonus = teamXpBonus;
        this.meleeBonus = meleeBonus;
        this.damageEffects = List.copyOf(damageEffects);
        this.filteredEffects = List.copyOf(filteredEffects);
        this.extra = extra.isEmpty() ? Map.of() : new EnumMap<>(extra);
    }

    public float extra(PerkEffectType type) {
        return extra.getOrDefault(type, 0F);
    }

    public float value(PerkEffectType type, WeaponCategory category, boolean automatic) {
        var total = extra(type);
        for (var effect : filteredEffects) {
            if (effect.type() == type && effect.matches(category, automatic)) {
                total += effect.value();
            }
        }
        return total;
    }

    public float value(PerkEffectType type, WeaponCategory category) {
        var total = extra(type);
        for (var effect : filteredEffects) {
            if (effect.type() == type && (effect.categories().isEmpty() || effect.categories().contains(category))) {
                total += effect.value();
            }
        }
        return total;
    }

    public boolean has(PerkEffectType type) {
        if (extra(type) != 0F) {
            return true;
        }
        for (var effect : filteredEffects) {
            if (effect.type() == type) {
                return true;
            }
        }
        return false;
    }

    public int base(SpecialAttribute attribute) {
        return base[attribute.ordinal()];
    }

    public int effective(SpecialAttribute attribute) {
        return effective[attribute.ordinal()];
    }

    public int temporary(SpecialAttribute attribute) {
        return effective[attribute.ordinal()] - base[attribute.ordinal()];
    }

    public int capacity(SpecialAttribute attribute) {
        return capacity[attribute.ordinal()];
    }

    public int capacityUsed(SpecialAttribute attribute) {
        return capacityUsed[attribute.ordinal()];
    }

    public float damageBonus(WeaponCategory category, boolean automatic, boolean melee) {
        var bonus = melee ? meleeBonus : 0F;
        for (var effect : damageEffects) {
            if (effect.matches(category, automatic)) {
                bonus += effect.value();
            }
        }
        return Math.clamp(bonus, 0F, (float) AtomicDawn.CONFIG.common.progression.perks.caps.PERKS_MAX_DAMAGE_BONUS);
    }

    public float conditionLossMultiplier() {
        return 1F - conditionReduction;
    }

    public float radiationMultiplier() {
        return 1F - radiationResistance;
    }
}

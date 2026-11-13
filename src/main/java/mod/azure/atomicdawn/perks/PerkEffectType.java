package mod.azure.atomicdawn.perks;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.special.SpecialAttribute;

public enum PerkEffectType implements StringRepresentable {

    DAMAGE("damage", true),

    CRITICAL_DAMAGE("critical_damage", true),

    MAX_HEALTH("max_health", false),

    MAX_AP("max_ap", false),

    AP_REGEN("ap_regen", true),

    VATS_AP_COST("vats_ap_cost", true, true),

    VATS_HIT_CHANCE("vats_hit_chance", true),

    CRITICAL_GAIN("critical_gain", true),

    CRITICAL_PROC("critical_proc", true),

    RADIATION_RESISTANCE("radiation_resistance", true),

    CONDITION_LOSS("condition_loss", true, true),

    XP_BONUS("xp_bonus", true),

    TEAM_XP_BONUS("team_xp_bonus", true),

    MUTATION_LOCK_RANDOM("mutation_lock_random", true),

    MUTATION_LOCK_REMOVAL("mutation_lock_removal", true),

    RADS_PER_HEAL("rads_per_heal", false),

    RAD_SPONGE("rad_sponge", false),

    RAD_DECAY("rad_decay", false),

    GLOW_CONVERSION("glow_conversion", true),

    GLOW_MAX("glow_max", false),

    GLOW_DAMAGE_RESISTANCE("glow_damage_resistance", true),

    GLOW_DAMAGE("glow_damage", true),

    GLOW_AP_REGEN("glow_ap_regen", true),

    FERAL_MELEE("feral_melee", true),

    FERAL_DECAY("feral_decay", true, true),

    STABILIZER_BOOST("stabilizer_boost", true),

    SNEAK_DAMAGE("sneak_damage", true),

    NIGHT_DAMAGE("night_damage", true),

    LOW_HEALTH_DAMAGE("low_health_damage", true),

    VATS_DAMAGE("vats_damage", true),

    LONG_RANGE_DAMAGE("long_range_damage", true),

    BUG_DAMAGE("bug_damage", true),

    GLOWING_DAMAGE("glowing_damage", true),

    KILL_DAMAGE("kill_damage", true),

    ARMOR_PENETRATION("armor_penetration", true),

    TARGET_MARK("target_mark", true),

    SNEAK_MARK("sneak_mark", true),

    MARK_ATTACKER("mark_attacker", true),

    WEAKEN_TARGET("weaken_target", true),

    CRIPPLE_CHANCE("cripple_chance", true),

    STAGGER_CHANCE("stagger_chance", true),

    BLEED_CHANCE("bleed_chance", true),

    FIRE_CHANCE("fire_chance", true),

    VATS_PROC("vats_proc", true),

    VATS_CLEAVE("vats_cleave", true),

    MELEE_KILL_EXPLOSION("melee_kill_explosion", true),

    RANGED_KILL_EXPLOSION("ranged_kill_explosion", true),

    EXPLOSIVE_KILL_EXPLOSION("explosive_kill_explosion", true),

    UNARMED_EXPLOSION("unarmed_explosion", true),

    SPREAD("spread", true, true),

    RELOAD_SPEED("reload_speed", true),

    AMMO_SAVE("ammo_save", true),

    ATTACK_SPEED("attack_speed", true),

    DAMAGE_REDUCTION("damage_reduction", true),

    REDUCE_MELEE("reduce_melee", true),

    REDUCE_PROJECTILE("reduce_projectile", true),

    REDUCE_EXPLOSION("reduce_explosion", true),

    REDUCE_FIRE("reduce_fire", true),

    REDUCE_ENERGY("reduce_energy", true),

    REDUCE_POISON("reduce_poison", true),

    REDUCE_FALL("reduce_fall", true),

    LOW_HEALTH_REDUCTION("low_health_reduction", true),

    NIGHT_REDUCTION("night_reduction", true),

    SPRINT_REDUCTION("sprint_reduction", true),

    SOLO_REDUCTION("solo_reduction", true),

    PARTY_REDUCTION("party_reduction", true),

    DODGE_AP("dodge_ap", true),

    DODGE_LOW_HEALTH("dodge_low_health", true),

    RICOCHET("ricochet", true),

    ARMOR("armor", false),

    KNOCKBACK_RESISTANCE("knockback_resistance", true),

    MOVEMENT_SPEED("movement_speed", true),

    LOW_HEALTH_SPEED("low_health_speed", true),

    HEALING_RECEIVED("healing_received", true),

    REGEN("regen", false),

    REGEN_DAY("regen_day", false),

    REGEN_RAIN("regen_rain", false),

    REGEN_IRRADIATED("regen_irradiated", false),

    AUTO_HEAL("auto_heal", true),

    CHEAT_DEATH("cheat_death", true),

    LAST_LAUGH("last_laugh", false),

    REVENANT("revenant", false),

    KILL_HEAL("kill_heal", false),

    KILL_AP("kill_ap", false),

    KILL_FOOD("kill_food", false),

    KILL_STABILITY("kill_stability", false),

    SOLO_AP_REGEN("solo_ap_regen", true),

    HUNGER_REDUCTION("hunger_reduction", true),

    FOOD_BONUS("food_bonus", true),

    DISEASE_RESISTANCE("disease_resistance", true),

    CHEM_DURATION("chem_duration", true),

    WATER_BREATHING("water_breathing", false),

    NIGHT_VISION("night_vision", false),

    STEALTH("stealth", true),

    ITEM_MAGNET("item_magnet", false),

    ARMOR_WEIGHT("armor_weight", true, true),

    ARMOR_CONDITION_LOSS("armor_condition_loss", true, true),

    LOOT_BONUS("loot_bonus", true),

    HARVEST_BONUS("harvest_bonus", true),

    CRAFT_AMMO_BONUS("craft_ammo_bonus", true),

    CRAFT_DUPLICATE("craft_duplicate", true),

    BARTER("barter", false),

    PACIFY("pacify", true),

    TEAM_HEALING("team_healing", true),

    TEAM_SPEED("team_speed", false),

    CLASS_FREAK("class_freak", true),

    STRANGE_IN_NUMBERS("strange_in_numbers", true),

    RADICOOL("radicool", false),

    PA_CORE_DRAIN("pa_core_drain", true, true),

    PA_SPRINT_DRAIN("pa_sprint_drain", true, true),

    PA_REBOOT("pa_reboot", true),

    PAIN_TRAIN("pain_train", false),

    ENERGY_ABSORB("energy_absorb", true),

    VATS_HEALTH_COST("vats_health_cost", true),

    CHEM_PROC("chem_proc", true),

    SET_POISON_RESISTANCE("set_poison_resistance", true),

    SET_FIRE_RESISTANCE("set_fire_resistance", true),

    RETRIBUTION("retribution", true),

    SURVIVAL_AID("survival_aid", false),

    GLOW_COST("glow_cost", true, true),

    LOCKPICK_TIER("lockpick_tier", false),

    HACK_TIER("hack_tier", false),

    SPECIAL_STRENGTH("special_strength", false, false, SpecialAttribute.STRENGTH),

    SPECIAL_PERCEPTION("special_perception", false, false, SpecialAttribute.PERCEPTION),

    SPECIAL_ENDURANCE("special_endurance", false, false, SpecialAttribute.ENDURANCE),

    SPECIAL_CHARISMA("special_charisma", false, false, SpecialAttribute.CHARISMA),

    SPECIAL_INTELLIGENCE("special_intelligence", false, false, SpecialAttribute.INTELLIGENCE),

    SPECIAL_AGILITY("special_agility", false, false, SpecialAttribute.AGILITY),

    SPECIAL_LUCK("special_luck", false, false, SpecialAttribute.LUCK);

    public static final Codec<PerkEffectType> CODEC = StringRepresentable.fromEnum(PerkEffectType::values);

    private final String serializedName;

    private final boolean percent;

    private final boolean reduction;

    @Nullable
    private final SpecialAttribute special;

    PerkEffectType(String serializedName, boolean percent) {
        this(serializedName, percent, false, null);
    }

    PerkEffectType(String serializedName, boolean percent, boolean reduction) {
        this(serializedName, percent, reduction, null);
    }

    PerkEffectType(String serializedName, boolean percent, boolean reduction, @Nullable SpecialAttribute special) {
        this.serializedName = serializedName;
        this.percent = percent;
        this.reduction = reduction;
        this.special = special;
    }

    public static @Nullable PerkEffectType forSpecial(SpecialAttribute attribute) {
        for (var type : values()) {
            if (type.special == attribute) {
                return type;
            }
        }
        return null;
    }

    public @Nullable SpecialAttribute special() {
        return special;
    }

    public boolean reduction() {
        return reduction;
    }

    public boolean signed() {
        return switch (this) {
            case LOCKPICK_TIER, HACK_TIER, SURVIVAL_AID, ITEM_MAGNET, BARTER, TEAM_SPEED, LAST_LAUGH, REVENANT,
                PAIN_TRAIN, WATER_BREATHING, NIGHT_VISION, RADICOOL -> false;
            default -> true;
        };
    }

    public boolean percent() {
        return percent;
    }

    public String translationKey() {
        return "perk_effect.atomicdawn." + serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

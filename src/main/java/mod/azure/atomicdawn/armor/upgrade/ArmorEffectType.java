package mod.azure.atomicdawn.armor.upgrade;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import mod.azure.atomicdawn.special.SpecialAttribute;

public enum ArmorEffectType implements StringRepresentable {

    ARMOR,
    TOUGHNESS,
    KNOCKBACK_RESISTANCE,
    MAX_HEALTH,
    SPEED,
    WEIGHT_REDUCTION,

    SPECIAL_STRENGTH(SpecialAttribute.STRENGTH),
    SPECIAL_PERCEPTION(SpecialAttribute.PERCEPTION),
    SPECIAL_ENDURANCE(SpecialAttribute.ENDURANCE),
    SPECIAL_CHARISMA(SpecialAttribute.CHARISMA),
    SPECIAL_INTELLIGENCE(SpecialAttribute.INTELLIGENCE),
    SPECIAL_AGILITY(SpecialAttribute.AGILITY),
    SPECIAL_LUCK(SpecialAttribute.LUCK),

    AP_REGEN,
    MAX_AP,
    AP_COST,
    CRITICAL_DAMAGE,
    SPREAD,
    MELEE_DAMAGE,
    UNARMED_DAMAGE,

    REDUCE_FROM,
    REDUCE_LOW_HEALTH,
    REDUCE_HIGH_HEALTH,
    REDUCE_AT_NIGHT,
    REDUCE_WELL_FED,
    REDUCE_EXPERIENCE,
    REDUCE_IRRADIATED,
    REDUCE_SPRINTING,
    REDUCE_STILL,
    REDUCE_BLOCKING,
    REDUCE_PROJECTILE,
    REDUCE_EXPLOSION,
    REDUCE_FIRE,
    REDUCE_ENERGY,
    REDUCE_MELEE,
    REDUCE_FALL,
    RADIATION_RESISTANCE,

    STEALTH,
    STEALTH_DARK,
    STEALTH_NIGHT,

    CHEM_DURATION,
    JET_PACK,
    AUTO_STIM,
    CHAMELEON,
    CLOAKING,
    REGENERATING,
    LIFE_SAVING,
    UNYIELDING,
    IMMUNITY,
    ADRENAL,
    EXHAUSTION,
    WARMING,
    WATER_BREATHING,
    CONDITION_LOSS,
    ARMOR_DURABILITY,

    RETALIATE_FIRE,
    RETALIATE_ENERGY,
    RETALIATE_SLOW,
    RETALIATE_POISON,

    DISSIPATE,
    HEALING_RECEIVED,
    ITEM_MAGNET,
    HASTE;

    public static final Codec<ArmorEffectType> CODEC = StringRepresentable.fromEnum(ArmorEffectType::values);

    private final SpecialAttribute special;

    ArmorEffectType() {
        this(null);
    }

    ArmorEffectType(SpecialAttribute special) {
        this.special = special;
    }

    public SpecialAttribute special() {
        return special;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

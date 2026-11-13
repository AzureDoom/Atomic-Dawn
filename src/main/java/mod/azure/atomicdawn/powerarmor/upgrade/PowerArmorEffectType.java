package mod.azure.atomicdawn.powerarmor.upgrade;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import mod.azure.atomicdawn.special.SpecialAttribute;

public enum PowerArmorEffectType implements StringRepresentable {

    HEADLAMP,

    TARGETING_HUD,
    NIGHT_VISION,

    RAD_SCRUB,

    SPECIAL_STRENGTH(SpecialAttribute.STRENGTH),
    SPECIAL_PERCEPTION(SpecialAttribute.PERCEPTION),
    SPECIAL_ENDURANCE(SpecialAttribute.ENDURANCE),
    SPECIAL_CHARISMA(SpecialAttribute.CHARISMA),
    SPECIAL_INTELLIGENCE(SpecialAttribute.INTELLIGENCE),
    SPECIAL_AGILITY(SpecialAttribute.AGILITY),
    SPECIAL_LUCK(SpecialAttribute.LUCK),

    JET_PACK,

    SPEED,

    AP_REGEN,

    MAX_AP,

    CORE_DRAIN,

    KINETIC_CHARGE,

    JUMP,

    LANDING_ABSORB,

    MELEE_AP_COST,

    UNARMED_DAMAGE,

    UNARMED_ENERGY,

    BLEED,

    REDUCE_FROM,

    REDUCE_LOW_HEALTH,

    REDUCE_HIGH_HEALTH,
    REDUCE_AT_NIGHT,

    REDUCE_WELL_FED,

    REDUCE_EXPERIENCE,

    REDUCE_IRRADIATED,
    REDUCE_MOVING,
    REDUCE_STILL,
    REDUCE_PROJECTILE,
    REDUCE_EXPLOSION,
    REDUCE_FIRE,

    REDUCE_ENERGY,

    RADIATION_RESISTANCE,

    AUTO_STIM,

    CHAMELEON,

    CLOAKING,

    REGENERATING,

    IMMUNITY,

    ADRENAL,

    EXHAUSTION,

    WARMING,

    AP_COST,

    CONDITION_LOSS,

    RETALIATE_FIRE,

    RETALIATE_ENERGY,

    RETALIATE_SLOW,

    RETALIATE_POISON,

    DISSIPATE,

    HEALING_RECEIVED,

    PIECE_DURABILITY,

    MAX_HEALTH,

    ITEM_MAGNET,

    HASTE;

    public static final Codec<PowerArmorEffectType> CODEC = StringRepresentable.fromEnum(PowerArmorEffectType::values);

    private final SpecialAttribute special;

    PowerArmorEffectType() {
        this(null);
    }

    PowerArmorEffectType(SpecialAttribute special) {
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

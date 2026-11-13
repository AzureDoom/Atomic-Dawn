package mod.azure.atomicdawn.weapon.modification;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public enum WeaponStat implements StringRepresentable {

    DAMAGE("damage", false),
    PELLETS("pellets", true),
    HEADSHOT_MULTIPLIER("headshot_multiplier", false),
    FALLOFF_START("falloff_start", false),
    RANGE("range", false),
    FIRE_INTERVAL("fire_interval", false),
    BASE_SPREAD("base_spread", false),
    ADS_SPREAD_MULTIPLIER("ads_spread_multiplier", false),
    MOVING_SPREAD_MULTIPLIER("moving_spread_multiplier", false),
    BLOOM_PER_SHOT("bloom_per_shot", false),
    MAX_BLOOM("max_bloom", false),
    RECOIL_VERTICAL("recoil_vertical", false),
    RECOIL_HORIZONTAL("recoil_horizontal", false),
    MAGAZINE_SIZE("magazine_size", true),
    RELOAD_TICKS("reload_ticks", true),
    DRAW_TICKS("draw_ticks", true),
    BURST_COUNT("burst_count", true),
    CHARGE_TICKS("charge_ticks", true),
    SPIN_UP_TICKS("spin_up_ticks", true),
    MELEE_REACH("melee_reach", false),
    KNOCKBACK("knockback", false),
    CONDITION_MAX("condition_max", true),
    CONDITION_LOSS("condition_loss", false),
    VATS_AP_COST("vats_ap_cost", false),
    VATS_HIT_CHANCE("vats_hit_chance", false),
    CRIT_DAMAGE("crit_damage", false);

    public static final Codec<WeaponStat> CODEC = StringRepresentable.fromEnum(WeaponStat::values);

    private final String serializedName;

    private final boolean integer;

    WeaponStat(String serializedName, boolean integer) {
        this.serializedName = serializedName;
        this.integer = integer;
    }

    public boolean isInteger() {
        return integer;
    }

    public String translationKey() {
        return "weaponstat.atomicdawn." + serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

package mod.azure.atomicdawn.powerarmor;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@SuppressWarnings("unused")
public enum PowerArmorSlot implements StringRepresentable {

    HELMET(0.15F),
    TORSO(0.35F),
    LEFT_ARM(0.125F),
    RIGHT_ARM(0.125F),
    LEFT_LEG(0.125F),
    RIGHT_LEG(0.125F);

    public static final int COUNT = 6;

    private static final PowerArmorSlot[] VALUES = values();

    private final float coverage;

    PowerArmorSlot(float coverage) {
        this.coverage = coverage;
    }

    public static PowerArmorSlot byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public static PowerArmorSlot[] all() {
        return VALUES;
    }

    public float coverage() {
        return coverage;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    public String translationKey() {
        return "power_armor.atomicdawn.slot." + getSerializedName();
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

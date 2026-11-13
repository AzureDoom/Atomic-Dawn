package mod.azure.atomicdawn.powerarmor.upgrade;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;

public enum PowerArmorModSlot implements StringRepresentable {

    HELMET,
    TORSO,
    ARM,
    LEG;

    public static final Codec<PowerArmorModSlot> CODEC = StringRepresentable.fromEnum(PowerArmorModSlot::values);

    public static PowerArmorModSlot of(PowerArmorSlot slot) {
        return switch (slot) {
            case HELMET -> HELMET;
            case TORSO -> TORSO;
            case LEFT_ARM, RIGHT_ARM -> ARM;
            case LEFT_LEG, RIGHT_LEG -> LEG;
        };
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

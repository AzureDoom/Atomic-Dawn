package mod.azure.atomicdawn.ghoul;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum PlayerForm implements StringRepresentable {

    HUMAN,
    GHOUL;

    public static final Codec<PlayerForm> CODEC = StringRepresentable.fromEnum(PlayerForm::values);

    private static final PlayerForm[] VALUES = values();

    public static PlayerForm byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : HUMAN;
    }

    public String translationKey() {
        return "ghoul.atomicdawn.form." + getSerializedName();
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

package mod.azure.atomicdawn.armor.upgrade;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum ArmorModKind implements StringRepresentable {

    MATERIAL,
    MISC;

    public static final Codec<ArmorModKind> CODEC = StringRepresentable.fromEnum(ArmorModKind::values);

    public String translationKey() {
        return "armor.atomicdawn.mod_kind." + getSerializedName();
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

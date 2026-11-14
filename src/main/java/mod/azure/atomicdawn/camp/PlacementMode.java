package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum PlacementMode implements StringRepresentable {

    SNAP,
    COLLISION,
    FREE;

    public static final Codec<PlacementMode> CODEC = StringRepresentable.fromEnum(PlacementMode::values);

    private static final PlacementMode[] VALUES = values();

    public static PlacementMode byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public Component label() {
        return Component.translatable("camp.atomicdawn.mode." + getSerializedName());
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

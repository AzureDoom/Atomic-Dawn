package mod.azure.atomicdawn.weapon.definition;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ReloadType implements StringRepresentable {

    MAGAZINE("magazine"),

    INDIVIDUAL_ROUND("individual_round"),

    SINGLE_SHOT("single_shot"),

    ENERGY_CELL("energy_cell"),

    HEAVY("heavy");

    public static final Codec<ReloadType> CODEC = StringRepresentable.fromEnum(ReloadType::values);

    private final String serializedName;

    public boolean perRound() {
        return this == INDIVIDUAL_ROUND;
    }

    ReloadType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

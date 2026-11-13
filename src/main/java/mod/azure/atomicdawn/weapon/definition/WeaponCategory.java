package mod.azure.atomicdawn.weapon.definition;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum WeaponCategory implements StringRepresentable {

    PISTOL("pistol"),
    RIFLE("rifle"),
    SHOTGUN("shotgun"),
    HEAVY("heavy"),
    ENERGY("energy"),
    EXPLOSIVE("explosive"),
    MELEE("melee"),
    UNARMED("unarmed"),
    BOW("bow");

    public static final Codec<WeaponCategory> CODEC = StringRepresentable.fromEnum(WeaponCategory::values);

    private final String serializedName;

    WeaponCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    public boolean melee() {
        return this == MELEE || this == UNARMED;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

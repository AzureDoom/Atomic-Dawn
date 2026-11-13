package mod.azure.atomicdawn.armor;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum ArmorWeight implements StringRepresentable {

    LIGHT(0F),
    STURDY(0.03F),
    HEAVY(0.06F);

    public static final Codec<ArmorWeight> CODEC = StringRepresentable.fromEnum(ArmorWeight::values);

    private final float speedPenalty;

    ArmorWeight(float speedPenalty) {
        this.speedPenalty = speedPenalty;
    }

    public float speedPenalty() {
        return speedPenalty;
    }

    public String translationKey() {
        return "armor.atomicdawn.weight." + getSerializedName();
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

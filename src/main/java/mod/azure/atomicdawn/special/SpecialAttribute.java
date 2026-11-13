package mod.azure.atomicdawn.special;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum SpecialAttribute implements StringRepresentable {

    STRENGTH("strength", 'S'),
    PERCEPTION("perception", 'P'),
    ENDURANCE("endurance", 'E'),
    CHARISMA("charisma", 'C'),
    INTELLIGENCE("intelligence", 'I'),
    AGILITY("agility", 'A'),
    LUCK("luck", 'L');

    public static final int COUNT = 7;

    private static final SpecialAttribute[] VALUES = values();

    public static final Codec<SpecialAttribute> CODEC = StringRepresentable.fromEnum(SpecialAttribute::values);

    public static final StreamCodec<ByteBuf, SpecialAttribute> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        SpecialAttribute::byIndex,
        SpecialAttribute::ordinal
    );

    private final String serializedName;

    private final char letter;

    SpecialAttribute(String serializedName, char letter) {
        this.serializedName = serializedName;
        this.letter = letter;
    }

    public static SpecialAttribute byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public static SpecialAttribute[] all() {
        return VALUES;
    }

    public char letter() {
        return letter;
    }

    public String translationKey() {
        return "special.atomicdawn." + serializedName;
    }

    public String descriptionKey() {
        return "special.atomicdawn." + serializedName + ".desc";
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

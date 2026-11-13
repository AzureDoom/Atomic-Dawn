package mod.azure.atomicdawn.weapon.modification;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ModSlot implements StringRepresentable {

    RECEIVER("receiver"),
    BARREL("barrel"),
    STOCK("stock"),
    GRIP("grip"),
    MAGAZINE("magazine"),
    SIGHT("sight"),
    MUZZLE("muzzle"),
    CAPACITOR("capacitor"),
    HEAD("head"),
    APPEARANCE("appearance");

    private static final ModSlot[] VALUES = values();

    public static final Codec<ModSlot> CODEC = StringRepresentable.fromEnum(ModSlot::values);

    public static final StreamCodec<ByteBuf, ModSlot> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        index -> VALUES[Math.floorMod(index, VALUES.length)],
        ModSlot::ordinal
    );

    private final String serializedName;

    ModSlot(String serializedName) {
        this.serializedName = serializedName;
    }

    public String translationKey() {
        return "modslot.atomicdawn." + serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

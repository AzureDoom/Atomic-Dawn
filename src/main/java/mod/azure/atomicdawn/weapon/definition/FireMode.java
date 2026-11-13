package mod.azure.atomicdawn.weapon.definition;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public enum FireMode implements StringRepresentable {

    SEMI_AUTO("semi_auto", false),

    AUTOMATIC("automatic", true),

    BURST("burst", false),

    CHARGED("charged", false),

    SPIN_UP("spin_up", true),

    MANUAL_CYCLING("manual_cycling", false);

    private static final FireMode[] VALUES = values();

    public static final Codec<FireMode> CODEC = StringRepresentable.fromEnum(FireMode::values);

    public static final StreamCodec<ByteBuf, FireMode> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        index -> VALUES[Math.floorMod(index, VALUES.length)],
        FireMode::ordinal
    );

    private final String serializedName;

    private final boolean repeatsWhileHeld;

    FireMode(String serializedName, boolean repeatsWhileHeld) {
        this.serializedName = serializedName;
        this.repeatsWhileHeld = repeatsWhileHeld;
    }

    public boolean repeatsWhileHeld() {
        return repeatsWhileHeld;
    }

    public boolean tracksTrigger() {
        return this == CHARGED || this == SPIN_UP;
    }

    public String translationKey() {
        return "firemode.atomicdawn." + serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

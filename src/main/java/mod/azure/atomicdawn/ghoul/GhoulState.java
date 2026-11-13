package mod.azure.atomicdawn.ghoul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

public record GhoulState(
    int schemaVersion,
    PlayerForm form,
    float glow,
    float stability,
    long lastFormChangeMillis,
    boolean transformedBefore,
    int appearanceVariant,
    boolean cosmeticVisible
) {

    public static final int CURRENT_SCHEMA = 1;

    public static final int APPEARANCE_VARIANTS = 4;

    public static final GhoulState DEFAULT = new GhoulState(
        CURRENT_SCHEMA,
        PlayerForm.HUMAN,
        0F,
        100F,
        0L,
        false,
        0,
        true
    );

    public static final MapCodec<GhoulState> MAP_CODEC = RecordCodecBuilder.<GhoulState>mapCodec(
        instance -> instance.group(
            Codec.INT.optionalFieldOf("schema", CURRENT_SCHEMA).forGetter(GhoulState::schemaVersion),
            PlayerForm.CODEC.optionalFieldOf("form", PlayerForm.HUMAN).forGetter(GhoulState::form),
            Codec.FLOAT.optionalFieldOf("glow", 0F).forGetter(GhoulState::glow),
            Codec.FLOAT.optionalFieldOf("stability", 100F).forGetter(GhoulState::stability),
            Codec.LONG.optionalFieldOf("last_form_change", 0L).forGetter(GhoulState::lastFormChangeMillis),
            Codec.BOOL.optionalFieldOf("transformed_before", false).forGetter(GhoulState::transformedBefore),
            Codec.INT.optionalFieldOf("appearance_variant", 0).forGetter(GhoulState::appearanceVariant),
            Codec.BOOL.optionalFieldOf("cosmetic_visible", true).forGetter(GhoulState::cosmeticVisible)
        ).apply(instance, GhoulState::new)
    ).xmap(GhoulState::migrate, Function.identity());

    public static final StreamCodec<ByteBuf, GhoulState> STREAM_CODEC = StreamCodec.of(
        (buffer, state) -> {
            buffer.writeByte(state.form.ordinal());
            buffer.writeFloat(state.glow);
            buffer.writeFloat(state.stability);
            buffer.writeLong(state.lastFormChangeMillis);
            buffer.writeBoolean(state.transformedBefore);
            buffer.writeByte(state.appearanceVariant);
            buffer.writeBoolean(state.cosmeticVisible);
        },
        buffer -> new GhoulState(
            CURRENT_SCHEMA,
            PlayerForm.byIndex(buffer.readByte()),
            finite(buffer.readFloat(), 0F),
            finite(buffer.readFloat(), 100F),
            buffer.readLong(),
            buffer.readBoolean(),
            clampVariant(buffer.readByte()),
            buffer.readBoolean()
        )
    );

    private static GhoulState migrate(GhoulState state) {
        return new GhoulState(
            CURRENT_SCHEMA,
            state.form == null ? PlayerForm.HUMAN : state.form,
            Math.max(0F, finite(state.glow, 0F)),
            Math.max(0F, finite(state.stability, 100F)),
            Math.max(0L, state.lastFormChangeMillis),
            state.transformedBefore || state.form == PlayerForm.GHOUL,
            clampVariant(state.appearanceVariant),
            state.cosmeticVisible
        );
    }

    private static float finite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    public static int clampVariant(int variant) {
        return Math.clamp(variant, 0, APPEARANCE_VARIANTS - 1);
    }

    public boolean isGhoul() {
        return form == PlayerForm.GHOUL;
    }

    public GhoulState sanitized(float maxGlow, float maxStability) {
        var cleanGlow = form == PlayerForm.GHOUL ? Math.clamp(finite(glow, 0F), 0F, maxGlow) : 0F;
        var cleanStability = Math.clamp(finite(stability, maxStability), 0F, maxStability);
        if (form != PlayerForm.GHOUL) {
            cleanStability = maxStability;
        }
        return new GhoulState(
            CURRENT_SCHEMA,
            form,
            cleanGlow,
            cleanStability,
            Math.max(0L, lastFormChangeMillis),
            transformedBefore,
            clampVariant(appearanceVariant),
            cosmeticVisible
        );
    }

    public GhoulState withForm(PlayerForm value, long changedAt, float maxStability) {
        return new GhoulState(
            CURRENT_SCHEMA,
            value,
            0F,
            maxStability,
            changedAt,
            transformedBefore || value == PlayerForm.GHOUL,
            appearanceVariant,
            cosmeticVisible
        );
    }

    public GhoulState withGlow(float value) {
        return new GhoulState(
            schemaVersion,
            form,
            value,
            stability,
            lastFormChangeMillis,
            transformedBefore,
            appearanceVariant,
            cosmeticVisible
        );
    }

    public GhoulState withStability(float value) {
        return new GhoulState(
            schemaVersion,
            form,
            glow,
            value,
            lastFormChangeMillis,
            transformedBefore,
            appearanceVariant,
            cosmeticVisible
        );
    }

    public GhoulState withLastFormChange(long value) {
        return new GhoulState(
            schemaVersion,
            form,
            glow,
            stability,
            value,
            transformedBefore,
            appearanceVariant,
            cosmeticVisible
        );
    }

    public GhoulState withAppearance(int variant, boolean visible) {
        return new GhoulState(
            schemaVersion,
            form,
            glow,
            stability,
            lastFormChangeMillis,
            transformedBefore,
            clampVariant(variant),
            visible
        );
    }
}

package mod.azure.atomicdawn.limb;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mod.azure.atomicdawn.vats.BodyPart;

public final class LimbState {

    public static final int PARTS = BodyPart.values().length;

    public static final LimbState HEALTHY = new LimbState(filled(), 0L);

    public static final MapCodec<LimbState> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.FLOAT.listOf().fieldOf("condition").forGetter(LimbState::asList),
            Codec.LONG.optionalFieldOf("last_hit", 0L).forGetter(LimbState::lastHit)
        ).apply(instance, LimbState::fromList)
    );

    public static final StreamCodec<ByteBuf, LimbState> STREAM_CODEC = StreamCodec.of(
        (buffer, state) -> {
            for (var value : state.condition) {
                buffer.writeFloat(value);
            }
        },
        buffer -> {
            var values = new float[PARTS];
            for (var i = 0; i < PARTS; i++) {
                values[i] = buffer.readFloat();
            }
            return new LimbState(values, 0L);
        }
    );

    private final float[] condition;

    private final long lastHit;

    private LimbState(float[] condition, long lastHit) {
        this.condition = condition;
        this.lastHit = lastHit;
    }

    private static float[] filled() {
        var values = new float[PARTS];
        Arrays.fill(values, 1F);
        return values;
    }

    private static LimbState fromList(List<Float> values, long lastHit) {
        var condition = filled();
        for (var i = 0; i < Math.min(PARTS, values.size()); i++) {
            condition[i] = Math.clamp(values.get(i), 0F, 1F);
        }
        return new LimbState(condition, lastHit);
    }

    private List<Float> asList() {
        var list = new ArrayList<Float>(PARTS);
        for (var value : condition) {
            list.add(value);
        }
        return list;
    }

    public long lastHit() {
        return lastHit;
    }

    public float get(BodyPart part) {
        return condition[part.ordinal()];
    }

    public boolean crippled(BodyPart part) {
        return condition[part.ordinal()] <= 0F;
    }

    public boolean anyCrippled() {
        for (var value : condition) {
            if (value <= 0F) {
                return true;
            }
        }
        return false;
    }

    public boolean healthy() {
        for (var value : condition) {
            if (value < 1F) {
                return false;
            }
        }
        return true;
    }

    public LimbState with(BodyPart part, float value, long hitTime) {
        var copy = condition.clone();
        copy[part.ordinal()] = Math.clamp(value, 0F, 1F);
        return new LimbState(copy, hitTime);
    }

    public LimbState restore(float amount, boolean crippled) {
        var copy = condition.clone();
        for (var i = 0; i < PARTS; i++) {
            if (copy[i] > 0F || crippled) {
                copy[i] = Math.min(1F, copy[i] + amount);
            }
        }
        return new LimbState(copy, lastHit);
    }

    public boolean sameConditions(LimbState other) {
        return Arrays.equals(condition, other.condition);
    }
}

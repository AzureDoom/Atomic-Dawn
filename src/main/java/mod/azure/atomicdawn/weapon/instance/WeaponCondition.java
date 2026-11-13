package mod.azure.atomicdawn.weapon.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record WeaponCondition(
    int current,
    int max
) {

    public static final Codec<WeaponCondition> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("current").forGetter(WeaponCondition::current),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max").forGetter(WeaponCondition::max)
        ).apply(instance, WeaponCondition::new)
    );

    public static final StreamCodec<ByteBuf, WeaponCondition> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        WeaponCondition::current,
        ByteBufCodecs.VAR_INT,
        WeaponCondition::max,
        WeaponCondition::new
    );

    public static WeaponCondition pristine(int max) {
        return new WeaponCondition(max, max);
    }

    public boolean isBroken() {
        return max > 0 && current <= 0;
    }

    public float fraction() {
        return max <= 0 ? 1F : current / (float) max;
    }

    public int missing() {
        return Math.max(0, max - current);
    }

    public WeaponCondition withCurrent(int value) {
        return new WeaponCondition(Math.clamp(value, 0, max), max);
    }

    public WeaponCondition withMax(int newMax) {
        if (newMax == max) {
            return this;
        }
        var scaled = max <= 0 ? newMax : Math.round(fraction() * newMax);
        return new WeaponCondition(Math.clamp(scaled, 0, newMax), newMax);
    }

    public float damageMultiplier() {
        var f = fraction();
        return f >= 0.5F ? 1F : 0.75F + 0.5F * f;
    }
}

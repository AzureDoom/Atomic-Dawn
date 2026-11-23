package mod.azure.atomicdawn.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record BlastZone(
    BlockPos center,
    float radius,
    long created,
    long expires
) {

    public static final Codec<BlastZone> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockPos.CODEC.fieldOf("center").forGetter(BlastZone::center),
            Codec.FLOAT.fieldOf("radius").forGetter(BlastZone::radius),
            Codec.LONG.fieldOf("created").forGetter(BlastZone::created),
            Codec.LONG.fieldOf("expires").forGetter(BlastZone::expires)
        ).apply(instance, BlastZone::new)
    );

    public boolean expired(long now) {
        return now >= expires;
    }

    public float strength(long now) {
        var lifetime = expires - created;
        if (lifetime <= 0L) {
            return 0F;
        }
        return Math.clamp((float) (expires - now) / lifetime, 0F, 1F);
    }

    public float proximity(BlockPos pos) {
        if (radius <= 0F) {
            return 0F;
        }
        var distance = Math.sqrt(pos.distSqr(center));
        if (distance > radius) {
            return 0F;
        }
        return 1F - 0.75F * (float) (distance / radius);
    }
}

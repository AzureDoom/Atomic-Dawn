package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

public record EntitySnapshot(
    Identifier type,
    Vec3 offset,
    Rotation rotation,
    CompoundTag data
) {

    public static final Codec<EntitySnapshot> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.fieldOf("type").forGetter(EntitySnapshot::type),
            Vec3.CODEC.fieldOf("offset").forGetter(EntitySnapshot::offset),
            Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(EntitySnapshot::rotation),
            CampNbt.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(EntitySnapshot::data)
        ).apply(instance, EntitySnapshot::new)
    );

    public EntitySnapshot {
        data = data == null ? new CompoundTag() : data.copy();
    }

    public EntitySnapshot withoutData() {
        return data.isEmpty() ? this : new EntitySnapshot(type, offset, rotation, new CompoundTag());
    }
}

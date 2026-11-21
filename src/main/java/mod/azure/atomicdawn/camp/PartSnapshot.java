package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record PartSnapshot(
    BlockPos offset,
    Identifier block,
    Map<String, String> properties,
    CompoundTag data
) {

    public static final Codec<PartSnapshot> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockPos.CODEC.fieldOf("offset").forGetter(PartSnapshot::offset),
            Identifier.CODEC.fieldOf("block").forGetter(PartSnapshot::block),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("properties", Map.of())
                .forGetter(PartSnapshot::properties),
            CampNbt.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(PartSnapshot::data)
        ).apply(instance, PartSnapshot::new)
    );

    public PartSnapshot {
        offset = offset.immutable();
        properties = Map.copyOf(properties);
        data = data == null ? new CompoundTag() : data.copy();
    }

    public PartSnapshot(BlockPos offset, Identifier block, Map<String, String> properties) {
        this(offset, block, properties, new CompoundTag());
    }

    public static PartSnapshot capture(BlockPos offset, BlockState state) {
        var values = new HashMap<String, String>();
        for (var property : state.getProperties()) {
            values.put(property.getName(), valueName(state, property));
        }
        return new PartSnapshot(offset, BuiltInRegistries.BLOCK.getKey(state.getBlock()), values);
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public Optional<BlockState> state() {
        return BuiltInRegistries.BLOCK.getOptional(block).map(found -> {
            var state = found.defaultBlockState();
            for (var entry : properties.entrySet()) {
                var property = found.getStateDefinition().getProperty(entry.getKey());
                if (property != null) {
                    state = apply(state, property, entry.getValue());
                }
            }
            return state;
        });
    }

    private static <T extends Comparable<T>> BlockState apply(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }

    public boolean hasData() {
        return !data.isEmpty();
    }

    public PartSnapshot withData(CompoundTag value) {
        return new PartSnapshot(offset, block, properties, value);
    }

    public PartSnapshot withoutData() {
        return data.isEmpty() ? this : new PartSnapshot(offset, block, properties);
    }

    public PartSnapshot shifted(BlockPos delta) {
        return new PartSnapshot(offset.offset(delta), block, properties, data);
    }

    public PartSnapshot rotated(Rotation rotation, LevelAccessor level, BlockPos pos) {
        var rotatedOffset = offset.rotate(rotation);

        return state()
            .map(
                found -> capture(
                    rotatedOffset,
                    found.rotate(level, pos, rotation)
                ).withData(data)
            )
            .orElse(new PartSnapshot(rotatedOffset, block, properties, data));
    }
}

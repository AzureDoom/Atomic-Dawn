package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record PartSnapshot(
    BlockPos offset,
    Identifier block,
    Map<String, String> properties
) {

    public static final Codec<PartSnapshot> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockPos.CODEC.fieldOf("offset").forGetter(PartSnapshot::offset),
            Identifier.CODEC.fieldOf("block").forGetter(PartSnapshot::block),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("properties", Map.of())
                .forGetter(PartSnapshot::properties)
        ).apply(instance, PartSnapshot::new)
    );

    public PartSnapshot {
        offset = offset.immutable();
        properties = Map.copyOf(properties);
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

    public PartSnapshot rotated(Rotation rotation) {
        return state().map(found -> capture(offset.rotate(rotation), found.rotate(rotation)))
            .orElse(new PartSnapshot(offset.rotate(rotation), block, properties));
    }
}

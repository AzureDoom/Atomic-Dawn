package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public record CampBlueprint(
    String name,
    Kind kind,
    List<Entry> entries,
    Optional<Identifier> plan
) {

    public static final int MAX_ENTRIES = 2048;

    public static final int MAX_PARTS = 64;

    public static final int MAX_ENTITIES = 16;

    public static final int MAX_NAME_LENGTH = 32;

    public enum Kind implements StringRepresentable {

        PREFAB,
        CAMP;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private static final Kind[] VALUES = values();

        public static Kind byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }

        public Component label() {
            return Component.translatable("camp.atomicdawn.blueprint.kind." + getSerializedName());
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Entry(
        Identifier definition,
        BlockPos pos,
        Direction facing,
        PlacementMode mode,
        List<PartSnapshot> parts,
        List<EntitySnapshot> entities
    ) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Identifier.CODEC.fieldOf("definition").forGetter(Entry::definition),
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Direction.CODEC.optionalFieldOf("facing", Direction.NORTH).forGetter(Entry::facing),
                PlacementMode.CODEC.optionalFieldOf("mode", PlacementMode.COLLISION).forGetter(Entry::mode),
                PartSnapshot.CODEC.listOf().fieldOf("parts").forGetter(Entry::parts),
                EntitySnapshot.CODEC.listOf().optionalFieldOf("entities", List.of()).forGetter(Entry::entities)
            ).apply(instance, Entry::new)
        );

        private static final Direction[] DIRECTIONS = Direction.values();

        private static final Rotation[] ROTATIONS = Rotation.values();

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
            (buf, entry) -> {
                Identifier.STREAM_CODEC.encode(buf, entry.definition());
                BlockPos.STREAM_CODEC.encode(buf, entry.pos());
                ByteBufCodecs.VAR_INT.encode(buf, entry.facing().ordinal());
                ByteBufCodecs.VAR_INT.encode(buf, entry.mode().ordinal());
                var parts = new ArrayList<PartSnapshot>();
                for (var part : entry.parts()) {
                    if (parts.size() < MAX_PARTS && part.state().isPresent()) {
                        parts.add(part);
                    }
                }
                ByteBufCodecs.VAR_INT.encode(buf, parts.size());
                for (var part : parts) {
                    BlockPos.STREAM_CODEC.encode(buf, part.offset());
                    ByteBufCodecs.VAR_INT.encode(buf, Block.getId(part.state().orElseThrow()));
                }
                var entities = entry.entities().subList(0, Math.min(entry.entities().size(), MAX_ENTITIES));
                ByteBufCodecs.VAR_INT.encode(buf, entities.size());
                for (var entity : entities) {
                    Identifier.STREAM_CODEC.encode(buf, entity.type());
                    ByteBufCodecs.DOUBLE.encode(buf, entity.offset().x);
                    ByteBufCodecs.DOUBLE.encode(buf, entity.offset().y);
                    ByteBufCodecs.DOUBLE.encode(buf, entity.offset().z);
                    ByteBufCodecs.VAR_INT.encode(buf, entity.rotation().ordinal());
                }
            },
            buf -> {
                var definition = Identifier.STREAM_CODEC.decode(buf);
                var pos = BlockPos.STREAM_CODEC.decode(buf);
                var facing = DIRECTIONS[Math.floorMod(ByteBufCodecs.VAR_INT.decode(buf), DIRECTIONS.length)];
                var mode = PlacementMode.byIndex(ByteBufCodecs.VAR_INT.decode(buf));
                var partCount = count(buf, MAX_PARTS);
                var parts = new ArrayList<PartSnapshot>(partCount);
                for (var i = 0; i < partCount; i++) {
                    var offset = BlockPos.STREAM_CODEC.decode(buf);
                    parts.add(PartSnapshot.capture(offset, Block.stateById(ByteBufCodecs.VAR_INT.decode(buf))));
                }
                var entityCount = count(buf, MAX_ENTITIES);
                var entities = new ArrayList<EntitySnapshot>(entityCount);
                for (var i = 0; i < entityCount; i++) {
                    var type = Identifier.STREAM_CODEC.decode(buf);
                    var offset = new Vec3(
                        ByteBufCodecs.DOUBLE.decode(buf),
                        ByteBufCodecs.DOUBLE.decode(buf),
                        ByteBufCodecs.DOUBLE.decode(buf)
                    );
                    var rotation = ROTATIONS[Math.floorMod(ByteBufCodecs.VAR_INT.decode(buf), ROTATIONS.length)];
                    entities.add(new EntitySnapshot(type, offset, rotation, null));
                }
                return new Entry(definition, pos, facing, mode, parts, entities);
            }
        );

        public Entry {
            pos = pos.immutable();
            facing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
            parts = List.copyOf(parts);
            entities = List.copyOf(entities);
        }

        public Entry withoutContents() {
            return new Entry(
                definition,
                pos,
                facing,
                mode,
                parts.stream().map(PartSnapshot::withoutData).toList(),
                List.of()
            );
        }

        public boolean hasContents() {
            return !entities.isEmpty() || parts.stream().anyMatch(PartSnapshot::hasData);
        }
    }

    public static final Codec<CampBlueprint> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(CampBlueprint::name),
            Kind.CODEC.optionalFieldOf("kind", Kind.PREFAB).forGetter(CampBlueprint::kind),
            Entry.CODEC.listOf().fieldOf("objects").forGetter(CampBlueprint::entries),
            Identifier.CODEC.optionalFieldOf("plan").forGetter(CampBlueprint::plan)
        ).apply(instance, CampBlueprint::new)
    );

    public static final Codec<CampBlueprint> NETWORK_CODEC = CODEC.xmap(
        blueprint -> blueprint,
        CampBlueprint::withoutContents
    );

    public static final StreamCodec<ByteBuf, CampBlueprint> STREAM_CODEC = StreamCodec.of(
        (buf, blueprint) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, blueprint.name());
            ByteBufCodecs.VAR_INT.encode(buf, blueprint.kind().ordinal());
            var entries = blueprint.entries().subList(0, Math.min(blueprint.entries().size(), MAX_ENTRIES));
            ByteBufCodecs.VAR_INT.encode(buf, entries.size());
            for (var entry : entries) {
                Entry.STREAM_CODEC.encode(buf, entry);
            }
            ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buf, blueprint.plan());
        },
        buf -> {
            var name = ByteBufCodecs.STRING_UTF8.decode(buf);
            var kind = Kind.byIndex(ByteBufCodecs.VAR_INT.decode(buf));
            var count = count(buf, MAX_ENTRIES);
            var entries = new ArrayList<Entry>(count);
            for (var i = 0; i < count; i++) {
                entries.add(Entry.STREAM_CODEC.decode(buf));
            }
            var plan = ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buf);
            return new CampBlueprint(name, kind, entries, plan);
        }
    );

    public CampBlueprint {
        name = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        entries = List.copyOf(entries);
    }

    public CampBlueprint(String name, Kind kind, List<Entry> entries) {
        this(name, kind, entries, Optional.empty());
    }

    private static int count(ByteBuf buf, int max) {
        int count = ByteBufCodecs.VAR_INT.decode(buf);
        if (count < 0 || count > max) {
            throw new IllegalArgumentException("C.A.M.P. blueprint list too large: " + count);
        }
        return count;
    }

    public CampBlueprint withoutContents() {
        if (!hasContents()) {
            return this;
        }
        return new CampBlueprint(name, kind, entries.stream().map(Entry::withoutContents).toList(), plan);
    }

    public boolean hasContents() {
        return entries.stream().anyMatch(Entry::hasContents);
    }

    public Component displayName(Identifier id) {
        return name.isBlank() ? Component.literal(id.getPath()) : Component.literal(name);
    }

    public int partCount() {
        var total = 0;
        for (var entry : entries) {
            total += entry.parts().size();
        }
        return total;
    }

    public int budget(RegistryAccess access) {
        var total = 0;
        for (var entry : entries) {
            total += CampBuildable.get(access, entry.definition()).map(CampBuildable::budget).orElse(0);
        }
        return total;
    }

    public Map<Identifier, Integer> counts() {
        var counts = new HashMap<Identifier, Integer>();
        for (var entry : entries) {
            counts.merge(entry.definition(), 1, Integer::sum);
        }
        return counts;
    }

    public List<SizedIngredient> cost(RegistryAccess access) {
        var all = new ArrayList<SizedIngredient>();
        for (var entry : entries) {
            CampBuildable.get(access, entry.definition()).ifPresent(definition -> all.addAll(definition.cost()));
        }
        return merge(all);
    }

    public static List<SizedIngredient> merge(List<SizedIngredient> cost) {
        var totals = new LinkedHashMap<List<?>, Integer>();
        var ingredients = new HashMap<List<?>, Ingredient>();
        for (var entry : cost) {
            List<?> key = entry.ingredient().getValues().stream().toList();
            totals.merge(key, entry.count(), Integer::sum);
            ingredients.putIfAbsent(key, entry.ingredient());
        }
        var merged = new ArrayList<SizedIngredient>();
        for (var total : totals.entrySet()) {
            merged.add(new SizedIngredient(ingredients.get(total.getKey()), total.getValue()));
        }
        return merged;
    }
}

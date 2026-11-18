package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record CampStorage(List<Stored> objects) {

    public static final int MAX_OBJECTS = 1024;

    public static final CampStorage EMPTY = new CampStorage(List.of());

    public record Stored(
        UUID id,
        CampBlueprint.Entry entry,
        boolean contents
    ) {

        public static final Codec<Stored> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Stored::id),
                CampBlueprint.Entry.CODEC.fieldOf("object").forGetter(Stored::entry)
            ).apply(instance, Stored::new)
        );

        public Stored {
            contents = contents || entry.hasContents();
        }

        public Stored(UUID id, CampBlueprint.Entry entry) {
            this(id, entry, false);
        }
    }

    public static final MapCodec<CampStorage> MAP_CODEC = Stored.CODEC.listOf()
        .optionalFieldOf("objects", List.of())
        .xmap(CampStorage::new, CampStorage::objects);

    public static final StreamCodec<ByteBuf, CampStorage> STREAM_CODEC = StreamCodec.of(
        (buf, storage) -> {
            var list = storage.objects().subList(0, Math.min(storage.objects().size(), MAX_OBJECTS));
            ByteBufCodecs.VAR_INT.encode(buf, list.size());
            for (var stored : list) {
                UUIDUtil.STREAM_CODEC.encode(buf, stored.id());
                ByteBufCodecs.BOOL.encode(buf, stored.contents());
                CampBlueprint.Entry.STREAM_CODEC.encode(buf, stored.entry());
            }
        },
        buf -> {
            var count = ByteBufCodecs.VAR_INT.decode(buf);
            if (count < 0 || count > MAX_OBJECTS) {
                throw new IllegalArgumentException("C.A.M.P. storage too large: " + count);
            }
            var list = new ArrayList<Stored>(count);
            for (var i = 0; i < count; i++) {
                var id = UUIDUtil.STREAM_CODEC.decode(buf);
                var contents = ByteBufCodecs.BOOL.decode(buf);
                var entry = CampBlueprint.Entry.STREAM_CODEC.decode(buf);
                list.add(new Stored(id, entry, contents));
            }
            return new CampStorage(list);
        }
    );

    public CampStorage {
        objects = List.copyOf(objects);
    }

    public static CampStorage of(Player player) {
        return player.getData(CampAttachments.STORAGE.get());
    }

    public int size() {
        return objects.size();
    }

    public Optional<Stored> get(UUID id) {
        for (var stored : objects) {
            if (stored.id().equals(id)) {
                return Optional.of(stored);
            }
        }
        return Optional.empty();
    }

    public boolean has(UUID id) {
        return get(id).isPresent();
    }

    public CampStorage with(Stored stored) {
        var list = new ArrayList<>(objects);
        list.add(stored);
        return new CampStorage(list);
    }

    public CampStorage without(UUID id) {
        return new CampStorage(objects.stream().filter(stored -> !stored.id().equals(id)).toList());
    }
}

package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record CampBlueprintLibrary(Map<String, CampBlueprint> blueprints) {

    public static final int MAX_BLUEPRINTS = 100;

    public static final CampBlueprintLibrary EMPTY = new CampBlueprintLibrary(Map.of());

    public static final MapCodec<CampBlueprintLibrary> MAP_CODEC = Codec.unboundedMap(Codec.STRING, CampBlueprint.CODEC)
        .optionalFieldOf("blueprints", Map.of())
        .xmap(CampBlueprintLibrary::new, CampBlueprintLibrary::blueprints);

    public static final StreamCodec<ByteBuf, CampBlueprintLibrary> STREAM_CODEC = StreamCodec.of(
        (buf, library) -> {
            var entries = library.blueprints().entrySet().stream().limit(MAX_BLUEPRINTS).toList();
            ByteBufCodecs.VAR_INT.encode(buf, entries.size());
            for (var entry : entries) {
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey());
                CampBlueprint.STREAM_CODEC.encode(buf, entry.getValue());
            }
        },
        buf -> {
            var count = ByteBufCodecs.VAR_INT.decode(buf);
            if (count < 0 || count > MAX_BLUEPRINTS) {
                throw new IllegalArgumentException("C.A.M.P. blueprint library too large: " + count);
            }
            var map = new LinkedHashMap<String, CampBlueprint>();
            for (var i = 0; i < count; i++) {
                var key = ByteBufCodecs.STRING_UTF8.decode(buf);
                map.put(key, CampBlueprint.STREAM_CODEC.decode(buf));
            }
            return new CampBlueprintLibrary(map);
        }
    );

    public CampBlueprintLibrary {
        blueprints = Map.copyOf(blueprints);
    }

    public static CampBlueprintLibrary of(Player player) {
        return player.getData(CampAttachments.BLUEPRINTS.get());
    }

    public Optional<CampBlueprint> get(String key) {
        return Optional.ofNullable(blueprints.get(key));
    }

    public boolean has(String key) {
        return blueprints.containsKey(key);
    }

    public int size() {
        return blueprints.size();
    }

    public CampBlueprintLibrary with(String key, CampBlueprint blueprint) {
        var copy = new HashMap<>(blueprints);
        copy.put(key, blueprint);
        return new CampBlueprintLibrary(copy);
    }

    public CampBlueprintLibrary without(String key) {
        if (!blueprints.containsKey(key)) {
            return this;
        }
        var copy = new HashMap<>(blueprints);
        copy.remove(key);
        return new CampBlueprintLibrary(copy);
    }
}

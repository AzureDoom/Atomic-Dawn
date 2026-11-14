package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record CampRegistry(
    Map<UUID, CampSlot> camps,
    Map<UUID, Integer> selected,
    int version
) {

    public static final int CURRENT_VERSION = 2;

    public static final CampRegistry EMPTY = new CampRegistry(Map.of(), Map.of(), CURRENT_VERSION);

    public static final MapCodec<CampRegistry> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            CampSlot.CODEC.listOf().optionalFieldOf("camps", List.of()).forGetter(CampRegistry::list),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                .optionalFieldOf("selected", Map.of())
                .forGetter(CampRegistry::selected),
            Codec.INT.optionalFieldOf("version", CURRENT_VERSION).forGetter(CampRegistry::version)
        ).apply(instance, CampRegistry::fromList)
    );

    public CampRegistry {
        camps = Map.copyOf(camps);
        selected = Map.copyOf(selected);
    }

    private static CampRegistry fromList(List<CampSlot> list, Map<UUID, Integer> selected, int version) {
        var map = new HashMap<UUID, CampSlot>();
        for (var slot : list) {
            map.putIfAbsent(slot.campId(), slot);
        }
        return new CampRegistry(map, selected, CURRENT_VERSION);
    }

    public List<CampSlot> list() {
        return camps.values().stream().sorted(Comparator.comparing(CampSlot::createdAt)).toList();
    }

    public Optional<CampSlot> get(UUID campId) {
        return Optional.ofNullable(camps.get(campId));
    }

    public List<CampSlot> ownedBy(UUID owner) {
        return camps.values()
            .stream()
            .filter(slot -> slot.ownedBy(owner))
            .sorted(Comparator.comparingInt(CampSlot::slotIndex))
            .toList();
    }

    public int selectedSlot(UUID owner) {
        return selected.getOrDefault(owner, 0);
    }

    public CampRegistry with(CampSlot slot) {
        var copy = new HashMap<>(camps);
        copy.put(slot.campId(), slot);
        return new CampRegistry(copy, selected, CURRENT_VERSION);
    }

    public CampRegistry without(UUID campId) {
        if (!camps.containsKey(campId)) {
            return this;
        }
        var copy = new HashMap<>(camps);
        copy.remove(campId);
        return new CampRegistry(copy, selected, CURRENT_VERSION);
    }

    public CampRegistry withSelected(UUID owner, int slot) {
        var copy = new HashMap<>(selected);
        copy.put(owner, slot);
        return new CampRegistry(camps, copy, CURRENT_VERSION);
    }
}

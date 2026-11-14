package mod.azure.atomicdawn.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import mod.azure.atomicdawn.registry.ADAttachments;

public final class CampManager {

    private record Index(
        CampRegistry source,
        Map<ResourceKey<Level>, Map<Long, List<CampSlot>>> byChunk,
        Map<ResourceKey<Level>, List<CampSlot>> byDimension
    ) {}

    @Nullable
    private static Index index;

    private CampManager() {}

    public static CampRegistry registry(MinecraftServer server) {
        var overworld = server.overworld();
        return overworld.hasData(ADAttachments.CAMPS.get())
            ? overworld.getData(ADAttachments.CAMPS.get())
            : CampRegistry.EMPTY;
    }

    static void save(MinecraftServer server, CampRegistry registry) {
        server.overworld().setData(ADAttachments.CAMPS.get(), registry);
        index = null;
    }

    public static void invalidate() {
        index = null;
    }

    private static Index index(MinecraftServer server) {
        var registry = registry(server);
        var current = index;
        if (current != null && current.source() == registry) {
            return current;
        }
        var byChunk = new HashMap<ResourceKey<Level>, Map<Long, List<CampSlot>>>();
        var byDimension = new HashMap<ResourceKey<Level>, List<CampSlot>>();
        for (var slot : registry.list()) {
            if (!slot.deployed()) {
                continue;
            }
            byDimension.computeIfAbsent(slot.dimension(), _ -> new ArrayList<>()).add(slot);
            var chunks = byChunk.computeIfAbsent(slot.dimension(), _ -> new HashMap<>());
            CampClaim.forEachChunk(slot.origin(), key -> chunks.computeIfAbsent(key, _ -> new ArrayList<>()).add(slot));
        }
        var built = new Index(registry, byChunk, byDimension);
        index = built;
        return built;
    }

    public static Optional<CampSlot> get(MinecraftServer server, UUID campId) {
        return registry(server).get(campId);
    }

    public static List<CampSlot> owned(MinecraftServer server, UUID owner) {
        return registry(server).ownedBy(owner);
    }

    public static Optional<CampSlot> active(MinecraftServer server, UUID owner) {
        return owned(server, owner).stream().filter(CampSlot::deployed).findFirst();
    }

    public static Optional<CampSlot> inSlot(MinecraftServer server, UUID owner, int slotIndex) {
        return owned(server, owner).stream().filter(slot -> slot.slotIndex() == slotIndex).findFirst();
    }

    public static int selectedSlot(MinecraftServer server, UUID owner) {
        return registry(server).selectedSlot(owner);
    }

    public static void select(MinecraftServer server, UUID owner, int slotIndex) {
        save(server, registry(server).withSelected(owner, slotIndex));
    }

    public static List<CampSlot> inDimension(MinecraftServer server, ResourceKey<Level> dimension) {
        return index(server).byDimension().getOrDefault(dimension, List.of());
    }

    public static Optional<CampSlot> claimAt(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
        var chunks = index(server).byChunk().get(dimension);
        if (chunks == null) {
            return Optional.empty();
        }
        var candidates = chunks.get(CampClaim.chunkKey(pos));
        if (candidates == null) {
            return Optional.empty();
        }
        for (var slot : candidates) {
            if (CampClaim.contains(slot.origin(), pos)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public static Optional<CampSlot> moduleAt(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
        return claimAt(server, dimension, pos).filter(slot -> slot.origin().equals(pos));
    }

    public record Tracked(
        CampSlot camp,
        CampObject object
    ) {}

    public static Optional<Tracked> objectAt(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
        return claimAt(server, dimension, pos)
            .flatMap(camp -> camp.objectAt(pos).map(object -> new Tracked(camp, object)));
    }

    public static Optional<CampSlot> update(MinecraftServer server, UUID campId, UnaryOperator<CampSlot> change) {
        var current = registry(server).get(campId);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        var updated = change.apply(current.get());
        put(server, updated);
        return Optional.of(updated);
    }

    public static void put(MinecraftServer server, CampSlot slot) {
        save(server, registry(server).with(slot));
    }

    public static void remove(MinecraftServer server, UUID campId) {
        save(server, registry(server).without(campId));
    }
}

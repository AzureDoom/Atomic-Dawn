package mod.azure.atomicdawn.camp;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CampModuleFacing {

    private static final Map<UUID, Direction> FACING = new ConcurrentHashMap<>();

    private CampModuleFacing() {}

    public static void set(UUID player, Direction facing) {
        if (facing.getAxis().isHorizontal()) {
            FACING.put(player, facing);
        }
    }

    public static @Nullable Direction get(UUID player) {
        return FACING.get(player);
    }

    public static void forget(UUID player) {
        FACING.remove(player);
    }
}

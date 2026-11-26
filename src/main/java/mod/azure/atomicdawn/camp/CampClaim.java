package mod.azure.atomicdawn.camp;

import net.minecraft.core.BlockPos;

import java.util.function.LongConsumer;

import mod.azure.atomicdawn.AtomicDawn;

public final class CampClaim {

    private CampClaim() {}

    public static int radius() {
        return AtomicDawn.CONFIG.common.settlement.camp.claims.CAMP_RADIUS;
    }

    public static int up() {
        return AtomicDawn.CONFIG.common.settlement.camp.claims.CAMP_HEIGHT_UP;
    }

    public static int down() {
        return AtomicDawn.CONFIG.common.settlement.camp.claims.CAMP_HEIGHT_DOWN;
    }

    public static int spacing() {
        return Math.max(AtomicDawn.CONFIG.common.settlement.camp.claims.CAMP_MIN_SPACING, radius() * 2 + 1);
    }

    public static boolean contains(BlockPos origin, BlockPos pos) {
        var dy = pos.getY() - origin.getY();
        if (dy > up() || dy < -down()) {
            return false;
        }
        return horizontalDistanceSqr(origin, pos) <= (long) radius() * radius();
    }

    public static long horizontalDistanceSqr(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public static boolean tooClose(BlockPos a, BlockPos b) {
        var spacing = (long) spacing();
        return horizontalDistanceSqr(a, b) < spacing * spacing;
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    public static long chunkKey(BlockPos pos) {
        return chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static void forEachChunk(BlockPos origin, LongConsumer consumer) {
        var r = radius();
        var minX = (origin.getX() - r) >> 4;
        var maxX = (origin.getX() + r) >> 4;
        var minZ = (origin.getZ() - r) >> 4;
        var maxZ = (origin.getZ() + r) >> 4;
        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                consumer.accept(chunkKey(x, z));
            }
        }
    }
}

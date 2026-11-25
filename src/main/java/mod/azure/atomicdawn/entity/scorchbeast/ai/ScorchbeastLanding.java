package mod.azure.atomicdawn.entity.scorchbeast.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

final class ScorchbeastLanding {

    private ScorchbeastLanding() {}

    @Nullable
    static BlockPos find(ScorchbeastEntity beast, Vec3 center, double minRadius, double maxRadius, int attempts) {
        var random = beast.getRandom();
        BlockPos best = null;
        var bestScore = Double.MAX_VALUE;
        for (var i = 0; i < attempts; i++) {
            var angle = random.nextDouble() * Math.PI * 2;
            var radius = minRadius + random.nextDouble() * Math.max(0.0, maxRadius - minRadius);
            var x = center.x + Math.cos(angle) * radius;
            var z = center.z + Math.sin(angle) * radius;
            var spot = suitable(beast, x, z);
            if (spot == null) {
                continue;
            }
            var score = Vec3.atBottomCenterOf(spot).distanceToSqr(beast.position()) * 0.25 + Vec3.atBottomCenterOf(
                spot
            ).distanceToSqr(center);
            if (score < bestScore) {
                bestScore = score;
                best = spot;
            }
        }
        return best;
    }

    @Nullable
    static BlockPos suitable(ScorchbeastEntity beast, double x, double z) {
        var level = beast.level();
        var y = (int) beast.groundY(x, z);
        var spot = BlockPos.containing(x, y, z);
        if (!level.isLoaded(spot)) {
            return null;
        }
        var below = level.getBlockState(spot.below());
        if (!below.isSolid() || below.is(BlockTags.LEAVES) || !level.getFluidState(spot.below()).isEmpty()) {
            return null;
        }
        if (!level.getFluidState(spot).isEmpty()) {
            return null;
        }
        var half = beast.getBbWidth() * 0.5;
        for (var dx = -1; dx <= 1; dx += 2) {
            for (var dz = -1; dz <= 1; dz += 2) {
                var corner = beast.groundY(x + dx * half, z + dz * half);
                if (Math.abs(corner - y) > 1.5) {
                    return null;
                }
            }
        }
        var box = beast.getType().getDimensions().makeBoundingBox(spot.getX() + 0.5, y + 0.1, spot.getZ() + 0.5);
        if (!level.noCollision(beast, box) || level.containsAnyLiquid(box)) {
            return null;
        }
        return spot;
    }

    static BlockPos below(ScorchbeastEntity beast) {
        for (var ring = 0; ring <= 12; ring += 3) {
            for (var step = 0; step < 8; step++) {
                var angle = step * Mth.TWO_PI / 8;
                var spot = suitable(beast, beast.getX() + Mth.cos(angle) * ring, beast.getZ() + Mth.sin(angle) * ring);
                if (spot != null) {
                    return spot;
                }
                if (ring == 0) {
                    break;
                }
            }
        }
        return BlockPos.containing(beast.getX(), beast.groundY(beast.getX(), beast.getZ()), beast.getZ());
    }
}

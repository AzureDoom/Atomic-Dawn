package mod.azure.atomicdawn.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADStructureTypes;

public class FissurePiece extends StructurePiece {

    private static final int KNOTS = 16;

    private static final int CRACK_REACH = 20;

    private final int centerX;

    private final int centerY;

    private final int centerZ;

    private final float angle;

    private final int length;

    private final int halfWidth;

    private final int depth;

    private final int crackCount;

    private final long seed;

    private float[] widthNoise;

    private float[] bendNoise;

    private float[] depthNoise;

    private Set<Long> crackCells;

    private Set<Long> crackEdges;

    public FissurePiece(
        BlockPos center,
        float angle,
        int length,
        int halfWidth,
        int depth,
        int crackCount,
        long seed,
        int minY
    ) {
        super(ADStructureTypes.FISSURE_PIECE.get(), 0, bounds(center, length, halfWidth, depth, minY));
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
        this.angle = angle;
        this.length = length;
        this.halfWidth = halfWidth;
        this.depth = depth;
        this.crackCount = crackCount;
        this.seed = seed;
    }

    public FissurePiece(CompoundTag tag) {
        super(ADStructureTypes.FISSURE_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.centerY = tag.getIntOr("CY", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
        this.angle = tag.getFloatOr("Angle", 0F);
        this.length = tag.getIntOr("Length", 32);
        this.halfWidth = tag.getIntOr("HalfWidth", 5);
        this.depth = tag.getIntOr("Depth", 20);
        this.crackCount = tag.getIntOr("Cracks", 10);
        this.seed = tag.getLongOr("Seed", 0L);
    }

    public BlockPos center() {
        return new BlockPos(centerX, centerY, centerZ);
    }

    private static BoundingBox bounds(BlockPos center, int length, int halfWidth, int depth, int minY) {
        var reach = length / 2 + halfWidth + CRACK_REACH;
        return new BoundingBox(
            center.getX() - reach,
            Math.max(minY, center.getY() - depth - 8),
            center.getZ() - reach,
            center.getX() + reach,
            center.getY() + 24,
            center.getZ() + reach
        );
    }

    @Override
    protected void addAdditionalSaveData(
        @NotNull StructurePieceSerializationContext context,
        @NotNull CompoundTag tag
    ) {
        tag.putInt("CX", centerX);
        tag.putInt("CY", centerY);
        tag.putInt("CZ", centerZ);
        tag.putFloat("Angle", angle);
        tag.putInt("Length", length);
        tag.putInt("HalfWidth", halfWidth);
        tag.putInt("Depth", depth);
        tag.putInt("Cracks", crackCount);
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(
        @NotNull WorldGenLevel level,
        @NotNull StructureManager structureManager,
        @NotNull ChunkGenerator generator,
        @NotNull RandomSource random,
        @NotNull BoundingBox box,
        @NotNull ChunkPos chunkPos,
        @NotNull BlockPos pivot
    ) {
        prepare();
        var cursor = new BlockPos.MutableBlockPos();
        var cos = Mth.cos(angle);
        var sin = Mth.sin(angle);
        var half = length / 2F;
        var minX = Math.max(box.minX(), boundingBox.minX());
        var maxX = Math.min(box.maxX(), boundingBox.maxX());
        var minZ = Math.max(box.minZ(), boundingBox.minZ());
        var maxZ = Math.min(box.maxZ(), boundingBox.maxZ());
        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                var dx = x + 0.5F - centerX;
                var dz = z + 0.5F - centerZ;
                var u = dx * cos + dz * sin;
                var v = -dx * sin + dz * cos;
                var t = u / half;
                var top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (top < boundingBox.minY() + 2) {
                    continue;
                }
                var wet = !level.getFluidState(cursor.set(x, top, z)).isEmpty();
                var edge = edgeDistance(t, v);
                if (edge < 0F && !wet) {
                    carve(level, box, cursor, x, z, top, t, v);
                } else if (edge < 3.5F + sample(widthNoise, t) * 1.5F && !wet) {
                    line(level, box, cursor, x, z, top, t, edge);
                }
                var key = pack(x, z);
                if (crackCells.contains(key)) {
                    crack(level, box, cursor, x, z, top, true);
                } else if (crackEdges.contains(key)) {
                    crack(level, box, cursor, x, z, top, false);
                }
            }
        }
    }

    private float halfWidthAt(float t) {
        if (Math.abs(t) >= 1F) {
            return 0F;
        }
        var taper = (float) Math.pow(1F - t * t, 0.55);
        return halfWidth * taper * (0.72F + 0.28F * sample(widthNoise, t));
    }

    private float bendAt(float t) {
        return halfWidth * 0.9F * sample(bendNoise, t);
    }

    private float edgeDistance(float t, float v) {
        if (Math.abs(t) >= 1F) {
            var tip = (Math.abs(t) - 1F) * length / 2F;
            return tip + Math.abs(v - bendAt(Math.signum(t) * 0.999F));
        }
        return Math.abs(v - bendAt(t)) - halfWidthAt(t);
    }

    private int holeDepth(float t, float v) {
        var width = Math.max(0.5F, halfWidthAt(t));
        var ratio = Math.min(1F, Math.abs(v - bendAt(t)) / width);
        var profile = (float) Math.sqrt(Math.max(0F, 1F - ratio * ratio * ratio * ratio));
        var along = (float) Math.pow(Math.max(0F, 1F - t * t), 0.3);
        return Math.max(2, Math.round(depth * profile * along * (0.8F + 0.2F * sample(depthNoise, t))) + 1);
    }

    private void carve(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int z,
        int top,
        float t,
        float v
    ) {
        var floor = Math.max(boundingBox.minY() + 1, Math.min(top, centerY) - holeDepth(t, v));
        for (var y = top + 10; y > top; y--) {
            var state = level.getBlockState(cursor.set(x, y, z));
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced()) {
                set(level, box, cursor, Blocks.AIR.defaultBlockState());
            }
        }
        for (var y = top; y > floor; y--) {
            set(level, box, cursor.set(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var central = Math.abs(v - bendAt(t)) / Math.max(1F, halfWidthAt(t)) < 0.4F;
        var roll = hash(x, floor, z, 1);
        BlockState floorState;
        if (central && roll < 0.08F) {
            floorState = ADBlocks.FISSURE_VENT.get().defaultBlockState();
        } else if (central || roll < 0.35F) {
            floorState = ADBlocks.FISSURE_SCAR.get().defaultBlockState();
        } else {
            floorState = ADBlocks.FISSURE_ROCK.get().defaultBlockState();
        }
        set(level, box, cursor.set(x, floor, z), floorState);
        for (var y = floor - 1; y >= floor - 2 && y > boundingBox.minY(); y--) {
            if (!level.getBlockState(cursor.set(x, y, z)).isAir()) {
                set(level, box, cursor, wallState(x, y, z));
            }
        }
        if (!floorState.is(ADBlocks.FISSURE_VENT.get()) && hash(x, floor, z, 2) < 0.12F) {
            set(level, box, cursor.set(x, floor + 1, z), ADBlocks.TOXIC_SLUDGE.get().defaultBlockState());
        }
    }

    private void line(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int z,
        int top,
        float t,
        float edge
    ) {
        var clamped = Mth.clamp(t, -0.98F, 0.98F);
        var bottom = Math.min(top, centerY) - Math.round(depth * 0.9F * (float) Math.pow(1F - clamped * clamped, 0.3))
            - 2;
        if (edge > 1.6F) {
            bottom = top - 1;
        }
        for (var y = top; y >= Math.max(boundingBox.minY() + 1, bottom); y--) {
            var state = level.getBlockState(cursor.set(x, y, z));
            if (state.isAir() || !level.getFluidState(cursor).isEmpty()) {
                continue;
            }
            set(level, box, cursor, y == top && edge > 1.6F ? rimState(x, y, z) : wallState(x, y, z));
        }
        var above = level.getBlockState(cursor.set(x, top + 1, z));
        if (!above.isAir() && above.canBeReplaced()) {
            set(level, box, cursor, Blocks.AIR.defaultBlockState());
        }
    }

    private void crack(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int z,
        int top,
        boolean core
    ) {
        var state = level.getBlockState(cursor.set(x, top, z));
        if (state.isAir() || !level.getFluidState(cursor).isEmpty() || state.is(ADBlocks.FISSURE_VENT.get())) {
            return;
        }
        if (core) {
            set(level, box, cursor, ADBlocks.FISSURE_SCAR.get().defaultBlockState());
            if (!level.getBlockState(cursor.set(x, top - 1, z)).isAir()) {
                set(level, box, cursor, ADBlocks.FISSURE_SCAR.get().defaultBlockState());
            }
        } else if (!state.is(ADBlocks.FISSURE_SCAR.get())) {
            set(level, box, cursor, rimState(x, top, z));
        }
        var above = level.getBlockState(cursor.set(x, top + 1, z));
        if (!above.isAir() && above.canBeReplaced()) {
            set(level, box, cursor, Blocks.AIR.defaultBlockState());
        }
    }

    private BlockState wallState(int x, int y, int z) {
        var streak = (y + Math.round(hash(x, 0, z, 3) * 7F)) % 4 == 0;
        var roll = hash(x, y, z, 4);
        if ((streak && roll < 0.65F) || roll < 0.12F) {
            return ADBlocks.FISSURE_SCAR.get().defaultBlockState();
        }
        return roll < 0.8F
            ? ADBlocks.FISSURE_ROCK.get().defaultBlockState()
            : Blocks.BLACKSTONE.defaultBlockState();
    }

    private BlockState rimState(int x, int y, int z) {
        var roll = hash(x, y, z, 5);
        if (roll < 0.14F) {
            return ADBlocks.FISSURE_SCAR.get().defaultBlockState();
        }
        if (roll < 0.62F) {
            return ADBlocks.FISSURE_ROCK.get().defaultBlockState();
        }
        return roll < 0.85F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.TUFF.defaultBlockState();
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private void prepare() {
        if (crackCells != null) {
            return;
        }
        var random = RandomSource.create(seed);
        widthNoise = knots(random);
        bendNoise = knots(random);
        depthNoise = knots(random);
        crackCells = new HashSet<>();
        crackEdges = new HashSet<>();
        var cos = Mth.cos(angle);
        var sin = Mth.sin(angle);
        var half = length / 2F;
        for (var i = 0; i < crackCount; i++) {
            float t;
            float side;
            float heading;
            if (i < 2) {
                t = i == 0 ? 0.97F : -0.97F;
                side = 0F;
                heading = i == 0 ? 0F : Mth.PI;
            } else {
                t = (random.nextFloat() * 2F - 1F) * 0.85F;
                side = random.nextBoolean() ? 1F : -1F;
                heading = side * Mth.HALF_PI;
            }
            var u = t * half;
            var v = bendAt(t) + side * (halfWidthAt(t) + 1.5F);
            heading += (random.nextFloat() - 0.5F) * 1.2F;
            walk(random, u, v, heading, 6 + random.nextInt(11), cos, sin, true);
        }
    }

    private void walk(
        RandomSource random,
        float u,
        float v,
        float heading,
        int steps,
        float cos,
        float sin,
        boolean branch
    ) {
        for (var step = 0; step < steps; step++) {
            var wx = Mth.floor(centerX + u * cos - v * sin);
            var wz = Mth.floor(centerZ + u * sin + v * cos);
            crackCells.add(pack(wx, wz));
            if (random.nextFloat() < 0.45F) {
                crackEdges.add(pack(wx + random.nextInt(3) - 1, wz + random.nextInt(3) - 1));
            }
            heading += (random.nextFloat() - 0.5F) * 0.9F;
            u += Mth.cos(heading);
            v += Mth.sin(heading);
            if (branch && step > 2 && random.nextFloat() < 0.18F) {
                walk(
                    random,
                    u,
                    v,
                    heading + (random.nextBoolean() ? 0.8F : -0.8F),
                    3 + random.nextInt(5),
                    cos,
                    sin,
                    false
                );
            }
        }
    }

    private static float[] knots(RandomSource random) {
        var values = new float[KNOTS + 1];
        for (var i = 0; i <= KNOTS; i++) {
            values[i] = random.nextFloat() * 2F - 1F;
        }
        return values;
    }

    private static float sample(float[] values, float t) {
        var position = (Mth.clamp(t, -1F, 1F) + 1F) * 0.5F * KNOTS;
        var index = Math.min(KNOTS - 1, (int) position);
        var fraction = position - index;
        var smooth = fraction * fraction * (3F - 2F * fraction);
        return Mth.lerp(smooth, values[index], values[index + 1]);
    }

    private float hash(int x, int y, int z, int salt) {
        var h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L) ^ salt;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 40) / (float) (1L << 24);
    }

    private static long pack(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }
}

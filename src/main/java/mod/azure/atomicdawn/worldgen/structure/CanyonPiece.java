package mod.azure.atomicdawn.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
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
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.registry.ADStructureTypes;

public class CanyonPiece extends StructurePiece {

    private static final int KNOTS = 16;

    private static final int TERRACES = 4;

    private static final float FLOOR_WIDTH = 0.3F;

    private static final float STREAM_WIDTH = 0.12F;

    private static final float WALL_MARGIN = 1.2F;

    private static final BlockState[] BANDS = {
        Blocks.TERRACOTTA.defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE).defaultBlockState(),
        Blocks.RED_SANDSTONE.defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE).defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE).defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.RED).defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.RED).defaultBlockState(),
        Blocks.TERRACOTTA.defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.YELLOW).defaultBlockState(),
        Blocks.RED_SANDSTONE.defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.BROWN).defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE).defaultBlockState(),
        Blocks.DYED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY).defaultBlockState(),
        Blocks.RED_SANDSTONE.defaultBlockState()
    };

    private final int centerX;

    private final int centerY;

    private final int centerZ;

    private final float angle;

    private final int length;

    private final int halfWidth;

    private final int depth;

    private final boolean flowing;

    private final long seed;

    private float[] widthNoise;

    private float[] bendNoise;

    private float[] depthNoise;

    private float[] bandNoise;

    public CanyonPiece(
        BlockPos center,
        float angle,
        int length,
        int halfWidth,
        int depth,
        boolean flowing,
        long seed,
        int minY
    ) {
        super(ADStructureTypes.CANYON_PIECE.get(), 0, bounds(center, length, halfWidth, depth, minY));
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
        this.angle = angle;
        this.length = length;
        this.halfWidth = halfWidth;
        this.depth = depth;
        this.flowing = flowing;
        this.seed = seed;
    }

    public CanyonPiece(CompoundTag tag) {
        super(ADStructureTypes.CANYON_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.centerY = tag.getIntOr("CY", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
        this.angle = tag.getFloatOr("Angle", 0F);
        this.length = tag.getIntOr("Length", 96);
        this.halfWidth = tag.getIntOr("HalfWidth", 12);
        this.depth = tag.getIntOr("Depth", 24);
        this.flowing = tag.getBooleanOr("Stream", true);
        this.seed = tag.getLongOr("Seed", 0L);
    }

    private static BoundingBox bounds(BlockPos center, int length, int halfWidth, int depth, int minY) {
        var reach = Mth.ceil(length / 2F + halfWidth * 3F) + 4;
        return new BoundingBox(
            center.getX() - reach,
            Math.max(minY, center.getY() - depth - 6),
            center.getZ() - reach,
            center.getX() + reach,
            center.getY() + depth + 32,
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
        tag.putBoolean("Stream", flowing);
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
        var bottom = Math.max(boundingBox.minY() + 1, centerY - depth - 3);
        var minX = Math.max(box.minX(), boundingBox.minX());
        var maxX = Math.min(box.maxX(), boundingBox.maxX());
        var minZ = Math.max(box.minZ(), boundingBox.minZ());
        var maxZ = Math.min(box.maxZ(), boundingBox.maxZ());
        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                var dx = x + 0.5F - centerX;
                var dz = z + 0.5F - centerZ;
                var t = (dx * cos + dz * sin) / half;
                if (Math.abs(t) >= 1F) {
                    continue;
                }
                var v = -dx * sin + dz * cos;
                var width = halfWidthAt(t);
                var s = Math.abs(v - bendAt(t)) / Math.max(1F, width);
                if (s >= WALL_MARGIN) {
                    continue;
                }
                var top = surface(level, cursor, x, z);
                if (top <= bottom || !level.getFluidState(cursor.set(x, top, z)).isEmpty()) {
                    continue;
                }
                var cut = cutDepth(t, s);
                if (cut < 2) {
                    paint(level, box, cursor, x, z, top - 1, bottom, t);
                    if (s < 1F) {
                        set(level, box, cursor.set(x, top, z), rimState(x, top, z));
                    }
                    continue;
                }
                var floor = Math.min(top, centerY - cut);
                clearAbove(level, box, cursor, x, z, top);
                for (var y = top; y > floor; y--) {
                    set(level, box, cursor.set(x, y, z), Blocks.AIR.defaultBlockState());
                }
                var stream = flowing && s < STREAM_WIDTH && alongDepth(t) > 0.8F;
                if (stream) {
                    set(level, box, cursor.set(x, floor, z), Blocks.WATER.defaultBlockState());
                    if (box.isInside(cursor)) {
                        level.scheduleTick(cursor.immutable(), Fluids.WATER, 1);
                    }
                    set(level, box, cursor.set(x, floor - 1, z), Blocks.GRAVEL.defaultBlockState());
                    paint(level, box, cursor, x, z, floor - 2, floor - 3, t);
                } else if (s < FLOOR_WIDTH) {
                    set(level, box, cursor.set(x, floor, z), floorState(x, floor, z, s));
                    paint(level, box, cursor, x, z, floor - 1, floor - 3, t);
                } else {
                    set(level, box, cursor.set(x, floor, z), terraceState(x, floor, z));
                    paint(level, box, cursor, x, z, floor - 1, bottom, t);
                }
            }
        }
    }

    private float alongDepth(float t) {
        return (float) Math.pow(Math.max(0F, 1F - t * t), 0.4);
    }

    private int cutDepth(float t, float s) {
        if (s >= 1F) {
            return 0;
        }
        var cross = 1F;
        if (s > FLOOR_WIDTH) {
            var outward = 1F - (s - FLOOR_WIDTH) / (1F - FLOOR_WIDTH);
            cross = Mth.ceil(outward * TERRACES) / (float) TERRACES;
        }
        var scale = 0.85F + 0.15F * sample(depthNoise, t);
        return Math.round(depth * alongDepth(t) * cross * scale);
    }

    private float halfWidthAt(float t) {
        var taper = (float) Math.pow(Math.max(0F, 1F - t * t), 0.35);
        return halfWidth * taper * (0.8F + 0.2F * sample(widthNoise, t));
    }

    private float bendAt(float t) {
        return halfWidth * 1.6F * sample(bendNoise, t);
    }

    private void paint(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int z,
        int from,
        int to,
        float t
    ) {
        var shift = Math.round(sample(bandNoise, t) * 3F);
        for (var y = from; y >= to; y--) {
            var state = level.getBlockState(cursor.set(x, y, z));
            if (paintable(state)) {
                set(level, box, cursor, BANDS[Math.floorMod(y + shift, BANDS.length)]);
            }
        }
    }

    private static boolean paintable(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
            || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.GRAVEL) || state.is(Blocks.SANDSTONE);
    }

    private int surface(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int x, int z) {
        var top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        while (top > boundingBox.minY()) {
            var state = level.getBlockState(cursor.set(x, top, z));
            var plant = state.canBeReplaced() && state.getFluidState().isEmpty();
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !plant) {
                break;
            }
            top--;
        }
        return top;
    }

    private void clearAbove(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int z,
        int from
    ) {
        for (var y = from + 24; y > from; y--) {
            var state = level.getBlockState(cursor.set(x, y, z));
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || (!state.isAir() && state.canBeReplaced())) {
                set(level, box, cursor, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private BlockState floorState(int x, int y, int z, float s) {
        var roll = hash(x, y, z, 1);
        if (!flowing) {
            if (roll < 0.5F) {
                return Blocks.COARSE_DIRT.defaultBlockState();
            }
            return roll < 0.8F ? Blocks.GRAVEL.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
        }
        if (s < STREAM_WIDTH + 0.06F && roll < 0.6F) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (roll < 0.75F) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return roll < 0.9F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.RED_SAND.defaultBlockState();
    }

    private BlockState terraceState(int x, int y, int z) {
        var roll = hash(x, y, z, 2);
        if (roll < 0.35F) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (roll < 0.7F) {
            return Blocks.RED_SAND.defaultBlockState();
        }
        return roll < 0.88F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.RED_SANDSTONE.defaultBlockState();
    }

    private BlockState rimState(int x, int y, int z) {
        var roll = hash(x, y, z, 3);
        if (roll < 0.5F) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return roll < 0.8F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.RED_SAND.defaultBlockState();
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private void prepare() {
        if (widthNoise != null) {
            return;
        }
        var random = RandomSource.create(seed);
        widthNoise = knots(random);
        bendNoise = knots(random);
        depthNoise = knots(random);
        bandNoise = knots(random);
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
}

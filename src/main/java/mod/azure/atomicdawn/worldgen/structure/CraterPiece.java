package mod.azure.atomicdawn.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import mod.azure.atomicdawn.block.WasteBarrelBlock;
import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADStructureTypes;

public class CraterPiece extends StructurePiece {

    private static final int KNOTS = 16;

    private static final float RIM_WIDTH = 0.45F;

    private static final float EJECTA_WIDTH = 0.5F;

    private final int centerX;

    private final int centerY;

    private final int centerZ;

    private final int radius;

    private final int depth;

    private final int rimHeight;

    private final boolean flooded;

    private final long seed;

    private float[] shapeNoise;

    public CraterPiece(
        BlockPos center,
        int radius,
        int depth,
        int rimHeight,
        boolean flooded,
        long seed,
        int minY
    ) {
        super(ADStructureTypes.CRATER_PIECE.get(), 0, bounds(center, radius, depth, rimHeight, minY));
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
        this.radius = radius;
        this.depth = depth;
        this.rimHeight = rimHeight;
        this.flooded = flooded;
        this.seed = seed;
    }

    public CraterPiece(CompoundTag tag) {
        super(ADStructureTypes.CRATER_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.centerY = tag.getIntOr("CY", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
        this.radius = tag.getIntOr("Radius", 16);
        this.depth = tag.getIntOr("Depth", 8);
        this.rimHeight = tag.getIntOr("RimHeight", 3);
        this.flooded = tag.getBooleanOr("Flooded", false);
        this.seed = tag.getLongOr("Seed", 0L);
    }

    public BlockPos center() {
        return new BlockPos(centerX, centerY, centerZ);
    }

    private static BoundingBox bounds(BlockPos center, int radius, int depth, int rimHeight, int minY) {
        var reach = Mth.ceil(radius * 1.15F * (1F + RIM_WIDTH + EJECTA_WIDTH)) + 2;
        return new BoundingBox(
            center.getX() - reach,
            Math.max(minY, center.getY() - depth - 4),
            center.getZ() - reach,
            center.getX() + reach,
            center.getY() + rimHeight + 24,
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
        tag.putInt("Radius", radius);
        tag.putInt("Depth", depth);
        tag.putInt("RimHeight", rimHeight);
        tag.putBoolean("Flooded", flooded);
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
        var waterLevel = centerY - Math.round(depth * 0.45F);
        var minX = Math.max(box.minX(), boundingBox.minX());
        var maxX = Math.min(box.maxX(), boundingBox.maxX());
        var minZ = Math.max(box.minZ(), boundingBox.minZ());
        var maxZ = Math.min(box.maxZ(), boundingBox.maxZ());
        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                var dx = x + 0.5F - centerX;
                var dz = z + 0.5F - centerZ;
                var angle = (float) Mth.atan2(dz, dx);
                var r = Mth.sqrt(dx * dx + dz * dz) / (radius * (0.88F + 0.24F * shape(angle)));
                if (r >= 1F + RIM_WIDTH + EJECTA_WIDTH) {
                    continue;
                }
                var top = surface(level, cursor, x, z);
                if (top < boundingBox.minY() + 2 || !level.getFluidState(cursor.set(x, top, z)).isEmpty()) {
                    continue;
                }
                if (r >= 1F + RIM_WIDTH) {
                    ejecta(level, box, cursor, x, z, top, r);
                    continue;
                }
                var target = centerY + Math.round(profile(r));
                clearAbove(level, box, cursor, x, z, Math.max(top, target));
                if (r < 1F && top > target) {
                    for (var y = top; y > target; y--) {
                        set(level, box, cursor.set(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                } else if (top < target) {
                    for (var y = top + 1; y < target; y++) {
                        set(level, box, cursor.set(x, y, z), rimState(x, y, z));
                    }
                }
                if (r >= 1F && top > target) {
                    target = top;
                }
                var pooled = flooded && r < 1F && target < waterLevel;
                var floor = r < 1F ? floorState(x, target, z, r, pooled) : rimState(x, target, z);
                set(level, box, cursor.set(x, target, z), floor);
                for (var y = target - 1; y >= target - 2 && y > boundingBox.minY(); y--) {
                    if (!level.getBlockState(cursor.set(x, y, z)).isAir()) {
                        set(level, box, cursor, wallState(x, y, z));
                    }
                }
                if (pooled) {
                    for (var y = target + 1; y <= waterLevel; y++) {
                        set(level, box, cursor.set(x, y, z), ADBlocks.IRRADIATED_WATER.get().defaultBlockState());
                    }
                } else if (r < 0.85F && hash(x, target, z, 6) < 0.04F) {
                    set(level, box, cursor.set(x, target + 1, z), ADBlocks.TOXIC_SLUDGE.get().defaultBlockState());
                } else if (r > 0.95F && r < 1.2F && hash(x, target, z, 7) < 0.012F) {
                    set(level, box, cursor.set(x, target + 1, z), barrelState(x, target, z));
                }
            }
        }
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

    private float profile(float r) {
        if (r < 1F) {
            var squared = r * r;
            return -depth * (1F - squared) + rimHeight * squared * squared;
        }
        var t = (r - 1F) / RIM_WIDTH;
        return rimHeight * (1F - t) * (1F - t);
    }

    private void ejecta(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int z,
        int top,
        float r
    ) {
        var fade = 1F - (r - 1F - RIM_WIDTH) / EJECTA_WIDTH;
        if (hash(x, top, z, 8) >= fade * 0.6F) {
            return;
        }
        var state = level.getBlockState(cursor.set(x, top, z));
        if (state.isAir() || !state.isSolid()) {
            return;
        }
        set(level, box, cursor, rimState(x, top, z));
        var above = level.getBlockState(cursor.set(x, top + 1, z));
        if (!above.isAir() && above.canBeReplaced()) {
            set(level, box, cursor, Blocks.AIR.defaultBlockState());
        }
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

    private BlockState floorState(int x, int y, int z, float r, boolean pooled) {
        var roll = hash(x, y, z, 1);
        if (r < 0.3F) {
            if (!pooled && roll < 0.05F) {
                return ADBlocks.FISSURE_VENT.get().defaultBlockState();
            }
            if (roll < 0.3F) {
                return ADBlocks.FISSURE_SCAR.get().defaultBlockState();
            }
            return roll < 0.75F ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
        }
        if (roll < 0.06F) {
            return ADBlocks.FISSURE_SCAR.get().defaultBlockState();
        }
        if (roll < 0.35F) {
            return Blocks.TUFF.defaultBlockState();
        }
        if (roll < 0.6F) {
            return ADBlocks.FISSURE_ROCK.get().defaultBlockState();
        }
        return roll < 0.85F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
    }

    private BlockState wallState(int x, int y, int z) {
        var roll = hash(x, y, z, 2);
        if (roll < 0.5F) {
            return Blocks.TUFF.defaultBlockState();
        }
        return roll < 0.8F ? Blocks.BLACKSTONE.defaultBlockState() : ADBlocks.FISSURE_ROCK.get().defaultBlockState();
    }

    private BlockState rimState(int x, int y, int z) {
        var roll = hash(x, y, z, 3);
        if (roll < 0.45F) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (roll < 0.7F) {
            return Blocks.TUFF.defaultBlockState();
        }
        return roll < 0.88F ? Blocks.GRAVEL.defaultBlockState() : ADBlocks.FISSURE_ROCK.get().defaultBlockState();
    }

    private BlockState barrelState(int x, int y, int z) {
        var block = hash(x, y, z, 4) < 0.4F ? ADBlocks.LEAKING_WASTE_BARREL.get() : ADBlocks.WASTE_BARREL.get();
        var facing = Direction.from2DDataValue(Mth.floor(hash(x, y, z, 5) * 4F));
        return block.defaultBlockState().setValue(WasteBarrelBlock.FACING, facing);
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private void prepare() {
        if (shapeNoise != null) {
            return;
        }
        var random = RandomSource.create(seed);
        shapeNoise = new float[KNOTS];
        for (var i = 0; i < KNOTS; i++) {
            shapeNoise[i] = random.nextFloat();
        }
    }

    private float shape(float angle) {
        var position = (angle / (Mth.PI * 2F) + 1F) % 1F * KNOTS;
        var index = Mth.floor(position) % KNOTS;
        var next = (index + 1) % KNOTS;
        var fraction = position - Mth.floor(position);
        var smooth = fraction * fraction * (3F - 2F * fraction);
        return Mth.lerp(smooth, shapeNoise[index], shapeNoise[next]);
    }

    private float hash(int x, int y, int z, int salt) {
        var h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L) ^ salt;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 40) / (float) (1L << 24);
    }
}

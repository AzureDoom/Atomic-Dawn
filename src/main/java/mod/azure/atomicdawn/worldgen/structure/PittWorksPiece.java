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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.helper.ADSerialization;
import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADStructureTypes;

public class PittWorksPiece extends StructurePiece {

    private static final int KNOTS = 16;

    private static final float EJECTA = 5F;

    private final int centerX;

    private final int centerY;

    private final int centerZ;

    private final int heapRadius;

    private final int heapHeight;

    private final int[] stacks;

    private final long seed;

    private float[] shapeNoise;

    public PittWorksPiece(BlockPos center, int heapRadius, int heapHeight, int[] stacks, long seed, int minY) {
        super(ADStructureTypes.PITT_WORKS_PIECE.get(), 0, bounds(center, heapRadius, heapHeight, stacks, minY));
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
        this.heapRadius = heapRadius;
        this.heapHeight = heapHeight;
        this.stacks = stacks;
        this.seed = seed;
    }

    public PittWorksPiece(CompoundTag tag) {
        super(ADStructureTypes.PITT_WORKS_PIECE.get(), tag);
        this.centerX = ADSerialization.getInt(tag, "CX", 0);
        this.centerY = ADSerialization.getInt(tag, "CY", 0);
        this.centerZ = ADSerialization.getInt(tag, "CZ", 0);
        this.heapRadius = ADSerialization.getInt(tag, "HeapRadius", 10);
        this.heapHeight = ADSerialization.getInt(tag, "HeapHeight", 8);
        this.stacks = ADSerialization.getIntArray(tag, "Stacks", new int[0]);
        this.seed = ADSerialization.getLong(tag, "Seed", 0L);
    }

    private static BoundingBox bounds(BlockPos center, int heapRadius, int heapHeight, int[] stacks, int minY) {
        var minX = center.getX() - heapRadius - (int) EJECTA - 2;
        var maxX = center.getX() + heapRadius + (int) EJECTA + 2;
        var minZ = center.getZ() - heapRadius - (int) EJECTA - 2;
        var maxZ = center.getZ() + heapRadius + (int) EJECTA + 2;
        var lowY = center.getY() - 4;
        var highY = center.getY() + heapHeight + 4;
        for (var i = 0; i + 4 < stacks.length; i += 5) {
            var radius = stacks[i + 3] + 1;
            minX = Math.min(minX, stacks[i] - radius);
            maxX = Math.max(maxX, stacks[i] + radius);
            minZ = Math.min(minZ, stacks[i + 2] - radius);
            maxZ = Math.max(maxZ, stacks[i + 2] + radius);
            lowY = Math.min(lowY, stacks[i + 1] - 4);
            highY = Math.max(highY, stacks[i + 1] + stacks[i + 4] + 2);
        }
        return new BoundingBox(minX, Math.max(minY, lowY), minZ, maxX, highY + 24, maxZ);
    }

    @Override
    protected void addAdditionalSaveData(
        @NotNull StructurePieceSerializationContext context,
        @NotNull CompoundTag tag
    ) {
        tag.putInt("CX", centerX);
        tag.putInt("CY", centerY);
        tag.putInt("CZ", centerZ);
        tag.putInt("HeapRadius", heapRadius);
        tag.putInt("HeapHeight", heapHeight);
        tag.putIntArray("Stacks", stacks);
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
        var minX = Math.max(box.minX(), boundingBox.minX());
        var maxX = Math.min(box.maxX(), boundingBox.maxX());
        var minZ = Math.max(box.minZ(), boundingBox.minZ());
        var maxZ = Math.min(box.maxZ(), boundingBox.maxZ());
        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                heap(level, box, cursor, x, z);
                for (var i = 0; i + 4 < stacks.length; i += 5) {
                    stack(level, box, cursor, x, z, i);
                }
            }
        }
    }

    private void heap(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos cursor, int x, int z) {
        var dx = x + 0.5F - centerX;
        var dz = z + 0.5F - centerZ;
        var angle = (float) Mth.atan2(dz, dx);
        var radius = heapRadius * (0.85F + 0.3F * shape(angle));
        var distance = Mth.sqrt(dx * dx + dz * dz);
        if (distance >= radius + EJECTA) {
            return;
        }
        var top = surface(level, cursor, x, z);
        if (top <= boundingBox.minY() || !level.getFluidState(cursor.set(x, top, z)).isEmpty()) {
            return;
        }
        if (distance >= radius) {
            var fade = 1F - (distance - radius) / EJECTA;
            if (hash(x, top, z, 1) < fade * 0.55F) {
                set(level, box, cursor.set(x, top, z), slagState(x, top, z));
                clearPlant(level, box, cursor, x, top + 1, z);
            }
            return;
        }
        var ratio = distance / radius;
        var height = Math.round(heapHeight * (1F - (float) Math.pow(ratio, 1.6)));
        clearPlant(level, box, cursor, x, top + 1, z);
        for (var y = top; y <= top + height; y++) {
            set(level, box, cursor.set(x, y, z), slagState(x, y, z));
        }
        if (hash(x, top + height, z, 2) < 0.04F) {
            set(level, box, cursor.set(x, top + height + 1, z), ADBlocks.TOXIC_SLUDGE.get().defaultBlockState());
        }
    }

    private void stack(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos cursor, int x, int z, int index) {
        var sx = stacks[index];
        var sy = stacks[index + 1];
        var sz = stacks[index + 2];
        var radius = stacks[index + 3];
        var height = stacks[index + 4];
        var dx = x - sx;
        var dz = z - sz;
        var distance = Mth.sqrt(dx * dx + dz * dz);
        if (distance >= radius + 0.5F) {
            return;
        }
        if (distance >= radius - 0.5F) {
            var top = sy + height - Math.round(hash(x, sy, z, 3) * 4F);
            for (var y = sy - 3; y <= top; y++) {
                var soot = y > top - 4 && hash(x, y, z, 4) < 0.5F;
                var state = soot || hash(x, y, z, 5) < 0.12F
                    ? Blocks.BLACKSTONE.defaultBlockState()
                    : Blocks.BRICKS.defaultBlockState();
                set(level, box, cursor.set(x, y, z), state);
            }
            return;
        }
        for (var y = sy + 1; y <= sy + height; y++) {
            set(level, box, cursor.set(x, y, z), Blocks.AIR.defaultBlockState());
        }
        set(level, box, cursor.set(x, sy, z), Blocks.BRICKS.defaultBlockState());
        if (dx == 0 && dz == 0) {
            set(level, box, cursor.set(x, sy, z), Blocks.HAY_BLOCK.defaultBlockState());
            set(
                level,
                box,
                cursor.set(x, sy + 1, z),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true)
            );
        }
    }

    private BlockState slagState(int x, int y, int z) {
        var roll = hash(x, y, z, 6);
        if (roll < 0.03F) {
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        }
        if (roll < 0.06F) {
            return Blocks.COAL_ORE.defaultBlockState();
        }
        if (roll < 0.4F) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (roll < 0.6F) {
            return Blocks.BASALT.defaultBlockState();
        }
        return roll < 0.85F ? Blocks.GRAVEL.defaultBlockState() : Blocks.TUFF.defaultBlockState();
    }

    private void clearPlant(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int y,
        int z
    ) {
        var state = level.getBlockState(cursor.set(x, y, z));
        if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
            set(level, box, cursor, Blocks.AIR.defaultBlockState());
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

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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADStructureTypes;

public class ToxicCavePiece extends StructurePiece {

    private static final int KNOTS = 16;

    private static final float SHELL = 1.35F;

    private final int centerX;

    private final int centerY;

    private final int centerZ;

    private final int radiusX;

    private final int radiusY;

    private final int radiusZ;

    private final long seed;

    private float[] shapeNoise;

    public ToxicCavePiece(BlockPos center, int radiusX, int radiusY, int radiusZ, long seed) {
        super(ADStructureTypes.TOXIC_CAVE_PIECE.get(), 0, bounds(center, radiusX, radiusY, radiusZ));
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
        this.seed = seed;
    }

    public ToxicCavePiece(CompoundTag tag) {
        super(ADStructureTypes.TOXIC_CAVE_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.centerY = tag.getIntOr("CY", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
        this.radiusX = tag.getIntOr("RX", 12);
        this.radiusY = tag.getIntOr("RY", 6);
        this.radiusZ = tag.getIntOr("RZ", 12);
        this.seed = tag.getLongOr("Seed", 0L);
    }

    private static BoundingBox bounds(BlockPos center, int radiusX, int radiusY, int radiusZ) {
        var reachX = Mth.ceil(radiusX * 1.15F * Mth.sqrt(SHELL)) + 1;
        var reachY = Mth.ceil(radiusY * Mth.sqrt(SHELL)) + 1;
        var reachZ = Mth.ceil(radiusZ * 1.15F * Mth.sqrt(SHELL)) + 1;
        return new BoundingBox(
            center.getX() - reachX,
            center.getY() - reachY,
            center.getZ() - reachZ,
            center.getX() + reachX,
            center.getY() + reachY,
            center.getZ() + reachZ
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
        tag.putInt("RX", radiusX);
        tag.putInt("RY", radiusY);
        tag.putInt("RZ", radiusZ);
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
        var waterLevel = centerY - Math.round(radiusY * 0.45F);
        var minX = Math.max(box.minX(), boundingBox.minX());
        var maxX = Math.min(box.maxX(), boundingBox.maxX());
        var minY = Math.max(box.minY(), boundingBox.minY());
        var maxY = Math.min(box.maxY(), boundingBox.maxY());
        var minZ = Math.max(box.minZ(), boundingBox.minZ());
        var maxZ = Math.min(box.maxZ(), boundingBox.maxZ());
        for (var x = minX; x <= maxX; x++) {
            for (var z = minZ; z <= maxZ; z++) {
                var dx = x + 0.5F - centerX;
                var dz = z + 0.5F - centerZ;
                var scale = 0.85F + 0.3F * shape((float) Mth.atan2(dz, dx));
                var nx = dx / (radiusX * scale);
                var nz = dz / (radiusZ * scale);
                var flat = nx * nx + nz * nz;
                if (flat >= SHELL) {
                    continue;
                }
                for (var y = minY; y <= maxY; y++) {
                    var ny = (y + 0.5F - centerY) / radiusY;
                    var distance = flat + ny * ny;
                    if (distance >= SHELL) {
                        continue;
                    }
                    var state = level.getBlockState(cursor.set(x, y, z));
                    if (state.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    if (distance < 1F) {
                        carve(level, box, cursor, x, y, z, flat, waterLevel);
                    } else if (y <= waterLevel + 1 || shellable(state)) {
                        set(level, box, cursor, shellState(x, y, z));
                    }
                }
            }
        }
    }

    private void carve(
        WorldGenLevel level,
        BoundingBox box,
        BlockPos.MutableBlockPos cursor,
        int x,
        int y,
        int z,
        float flat,
        int waterLevel
    ) {
        if (y <= waterLevel) {
            set(level, box, cursor, ADBlocks.IRRADIATED_WATER.get().defaultBlockState());
            return;
        }
        var below = (y - 0.5F - centerY) / radiusY;
        var onFloor = flat + below * below >= 1F;
        if (onFloor && y == waterLevel + 1 && hash(x, y, z, 1) < 0.08F) {
            set(level, box, cursor, ADBlocks.TOXIC_SLUDGE.get().defaultBlockState());
            return;
        }
        if (onFloor && hash(x, y, z, 2) < 0.03F) {
            set(level, box, cursor, ADBlocks.TOXIC_SLUDGE.get().defaultBlockState());
            return;
        }
        set(level, box, cursor, Blocks.CAVE_AIR.defaultBlockState());
    }

    private static boolean shellable(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL);
    }

    private BlockState shellState(int x, int y, int z) {
        var roll = hash(x, y, z, 3);
        if (roll < 0.1F) {
            return ADBlocks.FISSURE_SCAR.get().defaultBlockState();
        }
        if (roll < 0.45F) {
            return ADBlocks.FISSURE_ROCK.get().defaultBlockState();
        }
        if (roll < 0.75F) {
            return Blocks.TUFF.defaultBlockState();
        }
        return y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
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

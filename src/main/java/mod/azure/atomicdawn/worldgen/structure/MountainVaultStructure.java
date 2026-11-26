package mod.azure.atomicdawn.worldgen.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import mod.azure.atomicdawn.helper.ADWorldgenUtil;
import mod.azure.atomicdawn.registry.ADStructureTypes;

public class MountainVaultStructure extends Structure {

    public static final MapCodec<MountainVaultStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(structure -> structure.size),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistance),
            Codec.intRange(0, 64).optionalFieldOf("floor_offset", 3).forGetter(structure -> structure.floorOffset),
            Codec.intRange(4, 64).optionalFieldOf("min_rise", 10).forGetter(structure -> structure.minRise),
            Codec.intRange(1, 32).optionalFieldOf("attempts", 12).forGetter(structure -> structure.attempts)
        ).apply(instance, MountainVaultStructure::new)
    );

    private static final int[][] ORIGIN_OFFSETS = { { 0, 0 }, { 16, 0 }, { -16, 0 }, { 0, 16 }, { 0, -16 } };

    private final Holder<StructureTemplatePool> startPool;

    private final int size;

    private final int maxDistance;

    private final int floorOffset;

    private final int minRise;

    private final int attempts;

    public MountainVaultStructure(
        StructureSettings settings,
        Holder<StructureTemplatePool> startPool,
        int size,
        int maxDistance,
        int floorOffset,
        int minRise,
        int attempts
    ) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.maxDistance = maxDistance;
        this.floorOffset = floorOffset;
        this.minRise = minRise;
        this.attempts = attempts;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var middle = new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
        for (var offset : ORIGIN_OFFSETS) {
            var origin = middle.offset(offset[0], 0, offset[1]);
            var slopes = slopes(context, origin);
            if (slopes.isEmpty()) {
                continue;
            }
            var found = tryOrigin(context, origin, slopes);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<GenerationStub> tryOrigin(GenerationContext context, BlockPos origin, Set<Direction> slopes) {
        for (var attempt = 0; attempt < attempts; attempt++) {
            var stub = JigsawPlacement.addPieces(
                context,
                startPool,
                Optional.empty(),
                size,
                origin,
                false,
                Optional.empty(),
                new JigsawStructure.MaxDistance(maxDistance, maxDistance),
                PoolAliasLookup.EMPTY,
                DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING
            );
            if (stub.isEmpty() || !slopes.contains(predictedFacing(stub.get().position(), origin))) {
                continue;
            }
            var builder = stub.get().getPiecesBuilder();
            var pieces = builder.build().pieces();
            if (pieces.isEmpty() || !(pieces.getFirst() instanceof PoolElementStructurePiece start)) {
                continue;
            }
            if (!slopes.contains(start.getRotation().rotate(Direction.NORTH))) {
                continue;
            }
            var placed = place(context, start.getRotation(), start);
            if (placed.isEmpty()) {
                continue;
            }
            int frontY = placed.get();
            var box = start.getBoundingBox();
            int offsetY = frontY - 1 - (box.minY() + floorOffset);
            builder.build().pieces().forEach(piece -> piece.move(0, offsetY, 0));
            if (!buried(context, pieces)) {
                continue;
            }
            var center = start.getBoundingBox().getCenter();
            return Optional.of(
                new GenerationStub(
                    new BlockPos(center.getX(), frontY, center.getZ()),
                    Either.right(builder)
                )
            );
        }
        return Optional.empty();
    }

    private static Direction predictedFacing(BlockPos center, BlockPos origin) {
        var dx = center.getX() - origin.getX();
        var dz = center.getZ() - origin.getZ();
        Rotation rotation;
        if (dx >= 0) {
            rotation = dz >= 0 ? Rotation.NONE : Rotation.COUNTERCLOCKWISE_90;
        } else {
            rotation = dz >= 0 ? Rotation.CLOCKWISE_90 : Rotation.CLOCKWISE_180;
        }
        return rotation.rotate(Direction.NORTH);
    }

    private Set<Direction> slopes(GenerationContext context, BlockPos origin) {
        var found = EnumSet.noneOf(Direction.class);
        for (var facing : Direction.Plane.HORIZONTAL) {
            var front = height(context, origin.relative(facing, 8));
            var back = height(context, origin.relative(facing.getOpposite(), 8));
            if (back >= front + minRise && front > context.chunkGenerator().getSeaLevel() + 2) {
                found.add(facing);
            }
        }
        return found;
    }

    private Optional<Integer> place(GenerationContext context, Rotation rotation, PoolElementStructurePiece start) {
        var facing = rotation.rotate(Direction.NORTH);
        var box = start.getBoundingBox();
        var center = new BlockPos(box.getCenter().getX(), 0, box.getCenter().getZ());
        var halfDepth = (facing.getAxis() == Direction.Axis.X ? box.getXSpan() : box.getZSpan()) / 2;
        var front = center.relative(facing, Math.max(1, halfDepth - 2));
        var frontY = height(context, front);
        if (frontY <= context.chunkGenerator().getSeaLevel() + 2) {
            return Optional.empty();
        }
        if (height(context, front.relative(facing, 6)) > frontY + 3) {
            return Optional.empty();
        }
        if (height(context, center) < frontY + minRise) {
            return Optional.empty();
        }
        if (height(context, center.relative(facing.getOpposite(), 10)) < frontY + minRise + 4) {
            return Optional.empty();
        }
        return Optional.of(frontY);
    }

    private static boolean buried(GenerationContext context, List<StructurePiece> pieces) {
        for (var i = 1; i < pieces.size(); i++) {
            var box = pieces.get(i).getBoundingBox();
            var center = box.getCenter();
            if (height(context, new BlockPos(center.getX(), 0, center.getZ())) < box.maxY()) {
                return false;
            }
        }
        return true;
    }

    private static int height(GenerationContext context, BlockPos pos) {
        return ADWorldgenUtil.firstFreeHeight(context, pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG);
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ADStructureTypes.MOUNTAIN_VAULT.get();
    }
}

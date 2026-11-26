package mod.azure.atomicdawn.worldgen.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.helper.ADWorldgenUtil;
import mod.azure.atomicdawn.registry.ADStructureTypes;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class RoadStructure extends Structure {

    public static final TagKey<Structure> ROAD_PASSABLE = TagKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("road_passable")
    );

    public static final TagKey<Structure> ROAD_BLOCKING = TagKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("road_blocking")
    );

    public static final MapCodec<RoadStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(structure -> structure.size),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap")
                .forGetter(structure -> structure.projectStartToHeightmap),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistance)
        ).apply(instance, RoadStructure::new)
    );

    private final Holder<StructureTemplatePool> startPool;

    private final int size;

    private final HeightProvider startHeight;

    private final Optional<Heightmap.Types> projectStartToHeightmap;

    private final int maxDistance;

    public RoadStructure(
        StructureSettings settings,
        Holder<StructureTemplatePool> startPool,
        int size,
        HeightProvider startHeight,
        Optional<Heightmap.Types> projectStartToHeightmap,
        int maxDistance
    ) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistance = maxDistance;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var y = startHeight.sample(
            context.random(),
            new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor())
        );
        var stub = JigsawPlacement.addPieces(
            context,
            startPool,
            Optional.empty(),
            size,
            new BlockPos(chunk.getMinBlockX(), y, chunk.getMinBlockZ()),
            false,
            projectStartToHeightmap,
            new JigsawStructure.MaxDistance(maxDistance, maxDistance),
            PoolAliasLookup.EMPTY,
            DimensionPadding.ZERO,
            LiquidSettings.APPLY_WATERLOGGING
        );
        if (stub.isEmpty() || !AtomicDawn.CONFIG.server.world.roads.ROADS_AVOID_STRUCTURES) {
            return stub;
        }
        var pieces = stub.get().getPiecesBuilder().build().pieces();
        if (pieces.isEmpty()) {
            return Optional.empty();
        }
        var zones = blockedZones(context, area(pieces), AtomicDawn.CONFIG.server.world.roads.ROAD_STRUCTURE_CLEARANCE);
        if (zones.isEmpty()) {
            return stub;
        }
        if (blocked(pieces.getFirst().getBoundingBox(), zones)) {
            return Optional.empty();
        }
        var builder = new StructurePiecesBuilder();
        for (var piece : pieces) {
            if (!blocked(piece.getBoundingBox(), zones)) {
                builder.addPiece(piece);
            }
        }
        return Optional.of(new GenerationStub(stub.get().position(), Either.right(builder)));
    }

    private static BoundingBox area(List<StructurePiece> pieces) {
        var first = pieces.getFirst().getBoundingBox();
        var minX = first.minX();
        var minZ = first.minZ();
        var maxX = first.maxX();
        var maxZ = first.maxZ();
        for (var piece : pieces) {
            var box = piece.getBoundingBox();
            minX = Math.min(minX, box.minX());
            minZ = Math.min(minZ, box.minZ());
            maxX = Math.max(maxX, box.maxX());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        return new BoundingBox(minX, first.minY(), minZ, maxX, first.maxY(), maxZ);
    }

    private static boolean blocked(BoundingBox box, List<BoundingBox> zones) {
        for (var zone : zones) {
            if (
                box.maxX() >= zone.minX() && box.minX() <= zone.maxX() && box.maxZ() >= zone.minZ() && box
                    .minZ() <= zone
                        .maxZ()
            ) {
                return true;
            }
        }
        return false;
    }

    private static List<BoundingBox> blockedZones(GenerationContext context, BoundingBox area, int clearance) {
        var zones = new ArrayList<BoundingBox>();
        var reach = clearance + 16;
        var minChunkX = SectionPos.blockToSectionCoord(area.minX() - reach);
        var maxChunkX = SectionPos.blockToSectionCoord(area.maxX() + reach);
        var minChunkZ = SectionPos.blockToSectionCoord(area.minZ() - reach);
        var maxChunkZ = SectionPos.blockToSectionCoord(area.maxZ() + reach);
        var sets = ADRegistryUtil.registry(context.registryAccess(), Registries.STRUCTURE_SET);
        sets.listElements().forEach(holder -> {
            var set = holder.value();
            if (!(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
                return;
            }
            var candidates = set.structures()
                .stream()
                .map(StructureSet.StructureSelectionEntry::structure)
                .filter(RoadStructure::blocksRoads)
                .toList();
            if (candidates.isEmpty()) {
                return;
            }
            var spacing = spread.spacing();
            for (
                var regionX = Math.floorDiv(minChunkX, spacing); regionX <= Math.floorDiv(maxChunkX, spacing); regionX++
            ) {
                for (
                    var regionZ = Math.floorDiv(minChunkZ, spacing); regionZ <= Math.floorDiv(maxChunkZ, spacing);
                    regionZ++
                ) {
                    var start = spread.getPotentialStructureChunk(
                        context.seed(),
                        regionX * spacing,
                        regionZ * spacing
                    );
                    var zone = new BoundingBox(
                        start.getMinBlockX() - clearance,
                        area.minY(),
                        start.getMinBlockZ() - clearance,
                        start.getMaxBlockX() + clearance,
                        area.maxY(),
                        start.getMaxBlockZ() + clearance
                    );
                    if (
                        !blocked(area, List.of(zone)) || !validBiome(
                            context,
                            start.getMiddleBlockX(),
                            start.getMiddleBlockZ(),
                            candidates
                        )
                    ) {
                        continue;
                    }
                    zones.add(zone);
                }
            }
        });
        return zones;
    }

    private static boolean blocksRoads(Holder<Structure> structure) {
        if (structure.is(ROAD_PASSABLE)) {
            return false;
        }
        return structure.is(ROAD_BLOCKING) || structure.value().step() == GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    private static boolean validBiome(GenerationContext context, int x, int z, List<Holder<Structure>> candidates) {
        var y = ADWorldgenUtil.baseHeight(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);

        var biome = ADWorldgenUtil.noiseBiome(context, x, y, z);

        return candidates.stream()
            .anyMatch(candidate -> candidate.value().biomes().contains(biome));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ADStructureTypes.ROAD.get();
    }
}

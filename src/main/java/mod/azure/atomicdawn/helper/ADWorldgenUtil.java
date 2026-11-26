package mod.azure.atomicdawn.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;

public final class ADWorldgenUtil {

    private ADWorldgenUtil() {}

    public record StructureHit(
        Holder<Structure> structure,
        StructureStart start
    ) {}

    public static Holder<Biome> biome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos);
    }

    public static boolean biomeIs(LevelReader level, BlockPos pos, TagKey<Biome> tag) {
        return level.getBiome(pos).is(tag);
    }

    public static boolean biomeIs(LevelReader level, BlockPos pos, ResourceKey<Biome> key) {
        return level.getBiome(pos).is(key);
    }

    public static int firstFreeHeight(Structure.GenerationContext context, int x, int z, Heightmap.Types type) {
        return context.chunkGenerator()
            .getFirstFreeHeight(x, z, type, context.heightAccessor(), context.randomState());
    }

    public static int baseHeight(Structure.GenerationContext context, int x, int z, Heightmap.Types type) {
        return context.chunkGenerator()
            .getBaseHeight(x, z, type, context.heightAccessor(), context.randomState());
    }

    public static Holder<Biome> noiseBiome(Structure.GenerationContext context, int x, int y, int z) {
        return context.biomeResolver()
            .getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
    }

    public static List<StructureHit> structuresAt(ServerLevel level, BlockPos pos, TagKey<Structure> tag) {
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var manager = level.structureManager();
        var hits = new ArrayList<StructureHit>();
        for (var structure : manager.getAllStructuresAt(pos).keySet()) {
            var holder = structures.wrapAsHolder(structure);
            if (!holder.is(tag)) {
                continue;
            }
            var start = manager.getStructureAt(pos, structure);
            if (start.isValid()) {
                hits.add(new StructureHit(holder, start));
            }
        }
        return hits;
    }

    public static boolean insideStructure(ServerLevel level, BlockPos pos, TagKey<Structure> tag) {
        return !structuresAt(level, pos, tag).isEmpty();
    }

    public static StructureStart structureWithPieceAt(ServerLevel level, BlockPos pos, TagKey<Structure> tag) {
        return level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(tag));
    }

    public static StructureStart structureWithPieceAt(ServerLevel level, BlockPos pos, ResourceKey<Structure> key) {
        return level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(key));
    }
}

package mod.azure.atomicdawn.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public final class ADOverworldRegion extends Region {

    public ADOverworldRegion(Identifier name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.DESERT, ADBiomes.BURNING_SPRINGS);
            builder.replaceBiome(Biomes.SAVANNA, ADBiomes.TOXIC_VALLEY);
            builder.replaceBiome(Biomes.SAVANNA_PLATEAU, ADBiomes.TOXIC_VALLEY);
            builder.replaceBiome(Biomes.BADLANDS, ADBiomes.BURNING_SPRINGS);
            builder.replaceBiome(Biomes.WINDSWEPT_HILLS, ADBiomes.SAVAGE_DIVIDE);
            builder.replaceBiome(Biomes.WINDSWEPT_GRAVELLY_HILLS, ADBiomes.SAVAGE_DIVIDE);
            builder.replaceBiome(Biomes.STONY_PEAKS, ADBiomes.SAVAGE_DIVIDE);
            builder.replaceBiome(Biomes.JAGGED_PEAKS, ADBiomes.SAVAGE_DIVIDE);
            builder.replaceBiome(Biomes.FROZEN_PEAKS, ADBiomes.SAVAGE_DIVIDE);
            builder.replaceBiome(Biomes.MEADOW, ADBiomes.SKYLINE_VALLEY);
            builder.replaceBiome(Biomes.GROVE, ADBiomes.SKYLINE_VALLEY);
            builder.replaceBiome(Biomes.WINDSWEPT_FOREST, ADBiomes.SKYLINE_VALLEY);
            builder.replaceBiome(Biomes.ERODED_BADLANDS, ADBiomes.ASH_HEAP);
            builder.replaceBiome(Biomes.WOODED_BADLANDS, ADBiomes.ASH_HEAP);
            builder.replaceBiome(Biomes.SWAMP, ADBiomes.IRRADIATED_MIRE);
            builder.replaceBiome(Biomes.JUNGLE, ADBiomes.IRRADIATED_MIRE);
            builder.replaceBiome(Biomes.SPARSE_JUNGLE, ADBiomes.IRRADIATED_MIRE);
            builder.replaceBiome(Biomes.MANGROVE_SWAMP, ADBiomes.CRIMSON_BOG);
            builder.replaceBiome(Biomes.BAMBOO_JUNGLE, ADBiomes.CRIMSON_BOG);
        });
    }
}

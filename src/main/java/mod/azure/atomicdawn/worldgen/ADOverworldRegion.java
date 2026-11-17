
package mod.azure.atomicdawn.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public final class ADOverworldRegion extends Region {

    public ADOverworldRegion(Identifier name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(
        Registry<Biome> registry,
        Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper
    ) {
        addBiome(
            mapper,
            ADBiomes.TOXIC_VALLEY,
            0.2F,
            0.8F,
            -0.5F,
            0.1F,
            0.1F,
            1.0F,
            0.0F,
            0.8F
        );

        addBiome(
            mapper,
            ADBiomes.ASH_HEAP,
            0.5F,
            1.0F,
            -1.0F,
            -0.3F,
            0.2F,
            1.0F,
            -1.0F,
            0.0F
        );

        addBiome(
            mapper,
            ADBiomes.IRRADIATED_MIRE,
            0.2F,
            0.8F,
            0.3F,
            1.0F,
            -0.1F,
            0.6F,
            0.0F,
            1.0F
        );

        addBiome(
            mapper,
            ADBiomes.CRIMSON_BOG,
            0.6F,
            1.0F,
            0.5F,
            1.0F,
            0.0F,
            1.0F,
            -0.5F,
            0.5F
        );

        addBiome(
            mapper,
            ADBiomes.BURNING_SPRINGS,
            0.7F,
            1.0F,
            -1.0F,
            -0.5F,
            0.3F,
            1.0F,
            0.0F,
            1.0F
        );

        addBiome(
            mapper,
            ADBiomes.SAVAGE_DIVIDE,
            -0.5F,
            0.5F,
            -0.5F,
            0.5F,
            0.3F,
            1.0F,
            -1.0F,
            -0.3F
        );

        addBiome(
            mapper,
            ADBiomes.SKYLINE_VALLEY,
            -0.3F,
            0.5F,
            0.0F,
            0.8F,
            0.2F,
            1.0F,
            -0.3F,
            0.5F
        );
    }

    private static void addBiome(
        Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper,
        ResourceKey<Biome> biome,
        float minTemperature,
        float maxTemperature,
        float minHumidity,
        float maxHumidity,
        float minContinentalness,
        float maxContinentalness,
        float minErosion,
        float maxErosion
    ) {
        mapper.accept(
            Pair.of(
                Climate.parameters(
                    Climate.Parameter.span(minTemperature, maxTemperature),
                    Climate.Parameter.span(minHumidity, maxHumidity),
                    Climate.Parameter.span(minContinentalness, maxContinentalness),
                    Climate.Parameter.span(minErosion, maxErosion),
                    Climate.Parameter.span(-1.0F, 1.0F),
                    Climate.Parameter.span(-1.0F, 1.0F),
                    0.0F
                ),
                biome
            )
        );
    }
}

package mod.azure.atomicdawn.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

public final class ADSurfaceRules {

    private ADSurfaceRules() {}

    public static MaterialRule makeRules(HolderGetter<Biome> biomes) {
        return MaterialRules.ifTrue(
            MaterialRules.stoneDepthCheck(0, false, CaveSurface.FLOOR),
            MaterialRules.sequence(
                palette(biomes, ADBiomes.TOXIC_VALLEY, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT),
                palette(biomes, ADBiomes.ASH_HEAP, Blocks.TUFF, Blocks.GRAVEL),
                palette(biomes, ADBiomes.IRRADIATED_MIRE, Blocks.MUD, Blocks.MUD),
                palette(biomes, ADBiomes.CRIMSON_BOG, Blocks.GRASS_BLOCK, Blocks.MUD),
                palette(biomes, ADBiomes.BURNING_SPRINGS, Blocks.RED_SAND, Blocks.TERRACOTTA),
                palette(biomes, ADBiomes.SAVAGE_DIVIDE, Blocks.STONE, Blocks.GRAVEL),
                palette(biomes, ADBiomes.SKYLINE_VALLEY, Blocks.COARSE_DIRT, Blocks.GRAVEL)
            )
        );
    }

    private static MaterialRule palette(
        HolderGetter<Biome> biomes,
        net.minecraft.resources.ResourceKey<Biome> biome,
        Block exposed,
        Block underwater
    ) {
        return MaterialRules.ifTrue(
            MaterialRules.isBiome(biomes, biome),
            MaterialRules.sequence(
                MaterialRules.ifTrue(MaterialRules.waterBlockCheck(-1, 0), state(exposed)),
                state(underwater)
            )
        );
    }

    private static MaterialRule state(Block block) {
        return MaterialRules.state(block.defaultBlockState());
    }
}

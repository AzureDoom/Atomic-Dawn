package mod.azure.atomicdawn.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

public final class ADSurfaceRules {

    private ADSurfaceRules() {}

    public static MaterialRule makeRules(HolderGetter<Biome> biomes) {
        return MaterialRules.sequence(
            MaterialRules.ifTrue(
                MaterialRules.stoneDepthCheck(0, false, CaveSurface.FLOOR),
                MaterialRules.sequence(
                    palette(biomes, ADBiomes.TOXIC_VALLEY, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT),
                    palette(biomes, ADBiomes.ASH_HEAP, Blocks.TUFF, Blocks.GRAVEL),
                    palette(biomes, ADBiomes.IRRADIATED_MIRE, Blocks.MUD, Blocks.MUD),
                    palette(biomes, ADBiomes.CRIMSON_BOG, Blocks.GRASS_BLOCK, Blocks.MUD),
                    palette(biomes, ADBiomes.BURNING_SPRINGS, Blocks.RED_SAND, Blocks.TERRACOTTA),
                    palette(biomes, ADBiomes.SAVAGE_DIVIDE, Blocks.STONE, Blocks.GRAVEL),
                    palette(biomes, ADBiomes.SKYLINE_VALLEY, Blocks.COARSE_DIRT, Blocks.GRAVEL),
                    terrain(
                        biomes,
                        ADBiomes.MOJAVE_WASTELAND,
                        state(Blocks.SAND),
                        state(Blocks.COARSE_DIRT),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.GLOWING_SEA,
                        state(Blocks.COARSE_DIRT),
                        state(Blocks.TUFF),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.ZION_CANYON,
                        state(Blocks.GRASS_BLOCK),
                        MaterialRules.bandlands(),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.FAR_HARBOR_COAST,
                        state(Blocks.PODZOL),
                        state(Blocks.STONE),
                        MaterialRules.sequence(patch(0.3, Blocks.GRAVEL), below(-0.45, Blocks.STONE)),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.POINT_LOOKOUT_SWAMP,
                        state(Blocks.GRASS_BLOCK),
                        state(Blocks.COARSE_DIRT),
                        MaterialRules.sequence(patch(0.45, Blocks.MOSS_BLOCK), patch(0.1, Blocks.MUD)),
                        Blocks.MUD
                    ),
                    terrain(
                        biomes,
                        ADBiomes.NEW_CALIFORNIA_BADLANDS,
                        state(Blocks.COARSE_DIRT),
                        state(Blocks.TERRACOTTA),
                        MaterialRules.sequence(patch(0.4, Blocks.GRAVEL), below(-0.3, Blocks.SAND)),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.CAPITAL_WASTELAND,
                        state(Blocks.GRASS_BLOCK),
                        state(Blocks.STONE),
                        MaterialRules.sequence(patch(0.35, Blocks.COARSE_DIRT), below(-0.4, Blocks.GRAVEL)),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.THE_DIVIDE,
                        state(Blocks.COARSE_DIRT),
                        MaterialRules.bandlands(),
                        MaterialRules.sequence(patch(0.3, Blocks.GRAVEL), below(-0.35, Blocks.PACKED_MUD)),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.COMMONWEALTH_WASTELAND,
                        state(Blocks.GRASS_BLOCK),
                        state(Blocks.STONE),
                        MaterialRules.sequence(patch(0.3, Blocks.COARSE_DIRT), below(-0.45, Blocks.PODZOL)),
                        Blocks.GRAVEL
                    ),
                    terrain(
                        biomes,
                        ADBiomes.THE_PITT,
                        state(Blocks.COARSE_DIRT),
                        state(Blocks.TUFF),
                        MaterialRules.sequence(
                            patch(0.45, Blocks.BLACKSTONE),
                            patch(0.15, Blocks.GRAVEL),
                            below(-0.35, Blocks.TUFF)
                        ),
                        Blocks.GRAVEL
                    )
                )
            ),
            MaterialRules.ifTrue(
                MaterialRules.stoneDepthCheck(0, true, CaveSurface.FLOOR),
                MaterialRules.sequence(
                    subsurface(biomes, ADBiomes.MOJAVE_WASTELAND, state(Blocks.SANDSTONE)),
                    subsurface(biomes, ADBiomes.ZION_CANYON, MaterialRules.bandlands())
                )
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

    private static MaterialRule terrain(
        HolderGetter<Biome> biomes,
        ResourceKey<Biome> biome,
        MaterialRule exposed,
        MaterialRule steep,
        Block underwater
    ) {
        return MaterialRules.ifTrue(
            MaterialRules.isBiome(biomes, biome),
            MaterialRules.ifTrue(
                MaterialRules.abovePreliminarySurface(),
                MaterialRules.sequence(
                    MaterialRules.ifTrue(
                        MaterialRules.waterBlockCheck(-1, 0),
                        MaterialRules.sequence(MaterialRules.ifTrue(MaterialRules.steep(), steep), exposed)
                    ),
                    state(underwater)
                )
            )
        );
    }

    private static MaterialRule terrain(
        HolderGetter<Biome> biomes,
        ResourceKey<Biome> biome,
        MaterialRule exposed,
        MaterialRule steep,
        MaterialRule patches,
        Block underwater
    ) {
        return MaterialRules.ifTrue(
            MaterialRules.isBiome(biomes, biome),
            MaterialRules.ifTrue(
                MaterialRules.abovePreliminarySurface(),
                MaterialRules.sequence(
                    MaterialRules.ifTrue(
                        MaterialRules.waterBlockCheck(-1, 0),
                        MaterialRules.sequence(MaterialRules.ifTrue(MaterialRules.steep(), steep), patches, exposed)
                    ),
                    state(underwater)
                )
            )
        );
    }

    private static MaterialRule patch(double threshold, Block block) {
        return MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.SURFACE, threshold), state(block));
    }

    private static MaterialRule below(double threshold, Block block) {
        return MaterialRules.ifTrue(
            MaterialRules.not(MaterialRules.noiseCondition2d(Noises.SURFACE, threshold)),
            state(block)
        );
    }

    private static MaterialRule subsurface(HolderGetter<Biome> biomes, ResourceKey<Biome> biome, MaterialRule rule) {
        return MaterialRules.ifTrue(
            MaterialRules.isBiome(biomes, biome),
            MaterialRules.ifTrue(MaterialRules.abovePreliminarySurface(), rule)
        );
    }

    private static MaterialRule state(Block block) {
        return MaterialRules.state(block.defaultBlockState());
    }
}

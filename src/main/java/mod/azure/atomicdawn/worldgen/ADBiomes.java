package mod.azure.atomicdawn.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADBiomes {

    public static final ResourceKey<Biome> TOXIC_VALLEY = key("toxic_valley");

    public static final ResourceKey<Biome> ASH_HEAP = key("ash_heap");

    public static final ResourceKey<Biome> IRRADIATED_MIRE = key("irradiated_mire");

    public static final ResourceKey<Biome> CRIMSON_BOG = key("crimson_bog");

    public static final ResourceKey<Biome> BURNING_SPRINGS = key("burning_springs");

    public static final ResourceKey<Biome> SAVAGE_DIVIDE = key("savage_divide");

    public static final ResourceKey<Biome> SKYLINE_VALLEY = key("skyline_valley");

    public static final TagKey<Biome> WASTELAND = TagKey.create(Registries.BIOME, AtomicDawn.id("wasteland_biomes"));

    private ADBiomes() {}

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, AtomicDawn.id(name));
    }
}

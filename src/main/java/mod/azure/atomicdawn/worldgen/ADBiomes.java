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

    public static final ResourceKey<Biome> MOJAVE_WASTELAND = key("mojave_wasteland");

    public static final ResourceKey<Biome> GLOWING_SEA = key("glowing_sea");

    public static final ResourceKey<Biome> ZION_CANYON = key("zion_canyon");

    public static final ResourceKey<Biome> FAR_HARBOR_COAST = key("far_harbor_coast");

    public static final ResourceKey<Biome> POINT_LOOKOUT_SWAMP = key("point_lookout_swamp");

    public static final ResourceKey<Biome> NEW_CALIFORNIA_BADLANDS = key("new_california_badlands");

    public static final ResourceKey<Biome> CAPITAL_WASTELAND = key("capital_wasteland");

    public static final ResourceKey<Biome> THE_DIVIDE = key("the_divide");

    public static final ResourceKey<Biome> COMMONWEALTH_WASTELAND = key("commonwealth_wasteland");

    public static final ResourceKey<Biome> THE_PITT = key("the_pitt");

    public static final TagKey<Biome> WASTELAND = TagKey.create(Registries.BIOME, AtomicDawn.id("wasteland_biomes"));

    public static final TagKey<Biome> AMBIENT_RADIATION_LOW = tag("ambient_radiation_low");

    public static final TagKey<Biome> AMBIENT_RADIATION_HIGH = tag("ambient_radiation_high");

    private ADBiomes() {}

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, AtomicDawn.id(name));
    }

    private static TagKey<Biome> tag(String name) {
        return TagKey.create(Registries.BIOME, AtomicDawn.id(name));
    }
}

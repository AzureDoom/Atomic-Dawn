package mod.azure.atomicdawn.entity.supermutant;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

import mod.azure.atomicdawn.AtomicDawn;

public enum SuperMutantVariant {

    APPALACHIAN("appalachian"),
    MOJAVE("mojave"),
    CAPITAL("capital"),
    COMMONWEALTH("commonwealth");

    private static final SuperMutantVariant[] VALUES = values();

    private final String name;

    private final Identifier model;

    private final Identifier animations;

    private final Identifier texture;

    private final TagKey<Biome> biomes;

    SuperMutantVariant(String name) {
        this.name = name;
        this.model = AtomicDawn.id("geo/entity/" + name + "_super_mutant.geo.json");
        this.animations = AtomicDawn.id("animations/entity/" + name + "_super_mutant.animation.json");
        this.texture = AtomicDawn.id("textures/entity/super_mutant/" + name + "_super_mutant.png");
        this.biomes = TagKey.create(Registries.BIOME, AtomicDawn.id("super_mutant_variants/" + name));
    }

    public String serializedName() {
        return name;
    }

    public Identifier model() {
        return model;
    }

    public Identifier animations() {
        return animations;
    }

    public Identifier texture() {
        return texture;
    }

    public TagKey<Biome> biomes() {
        return biomes;
    }

    public static SuperMutantVariant random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    public static SuperMutantVariant byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : APPALACHIAN;
    }

    public static SuperMutantVariant byName(String name) {
        for (var variant : VALUES) {
            if (variant.name.equals(name)) {
                return variant;
            }
        }
        return APPALACHIAN;
    }
}

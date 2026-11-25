package mod.azure.atomicdawn.entity.scorched;

import net.minecraft.resources.Identifier;

import mod.azure.atomicdawn.AtomicDawn;

public enum ScorchedVariant {

    STANDARD("standard", "base", true),
    OFFICER("officer", "officer", true),
    HOLIDAY("holiday", "holiday", false),
    SPOOKY("spooky", "spooky", false);

    private static final ScorchedVariant[] VALUES = values();

    private final String name;

    private final Identifier texture;

    private final boolean usesGuns;

    ScorchedVariant(String name, String texture, boolean usesGuns) {
        this.name = name;
        this.texture = AtomicDawn.id("textures/entity/scorched/scorched_" + texture + ".png");
        this.usesGuns = usesGuns;
    }

    public String serializedName() {
        return name;
    }

    public Identifier texture() {
        return texture;
    }

    public boolean usesGuns() {
        return usesGuns;
    }

    public static ScorchedVariant byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : STANDARD;
    }

    public static ScorchedVariant byName(String name) {
        for (var variant : VALUES) {
            if (variant.name.equals(name)) {
                return variant;
            }
        }
        return STANDARD;
    }
}

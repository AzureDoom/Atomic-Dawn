package mod.azure.atomicdawn.entity.ghoul;

import net.minecraft.resources.Identifier;

import mod.azure.atomicdawn.AtomicDawn;

public enum FeralGhoulVariant {

    ASHEN_GRAY("ashen_gray"),
    BRUISED_PURPLE("bruised_purple"),
    CHARRED("charred"),
    DUSTY_BEIGE("dusty_beige"),
    INFLAMED_PINK("inflamed_pink"),
    IRRADIATED_OCHRE("irradiated_ochre"),
    MOLDY_GREEN("moldy_green"),
    NECROTIC_BLUE("necrotic_blue"),
    RAW_FLESH("raw_flesh"),
    SCARRED_LEATHERY("scarred_leathery"),
    SICKLY_OLIVE("sickly_olive"),
    STITCHED_PATCHWORK("stitched_patchwork");

    private static final FeralGhoulVariant[] VALUES = values();

    private final String name;

    private final Identifier texture;

    FeralGhoulVariant(String name) {
        this.name = name;
        this.texture = AtomicDawn.id("textures/entity/ghoul_mob/ghoul_" + name + ".png");
    }

    public String serializedName() {
        return name;
    }

    public Identifier texture() {
        return texture;
    }

    public static FeralGhoulVariant byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : DUSTY_BEIGE;
    }

    public static FeralGhoulVariant byName(String name) {
        for (var variant : VALUES) {
            if (variant.name.equals(name)) {
                return variant;
            }
        }
        return DUSTY_BEIGE;
    }

    public static int count() {
        return VALUES.length;
    }
}

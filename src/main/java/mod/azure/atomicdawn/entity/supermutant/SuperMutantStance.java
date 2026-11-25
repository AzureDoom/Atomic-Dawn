package mod.azure.atomicdawn.entity.supermutant;

public enum SuperMutantStance {

    NONE(""),
    ONE_HANDED("animation.super_mutant.one_handed_hold"),
    RIFLE("animation.super_mutant.rifle_hold");

    private static final SuperMutantStance[] VALUES = values();

    private final String animation;

    SuperMutantStance(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static SuperMutantStance byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

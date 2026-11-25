package mod.azure.atomicdawn.entity.supermutant;

public enum SuperMutantState {

    IDLE("animation.super_mutant.idle"),
    WALK("animation.super_mutant.walk"),
    RUN("animation.super_mutant.run"),
    DEAD("animation.super_mutant.death");

    private static final SuperMutantState[] VALUES = values();

    private final String animation;

    SuperMutantState(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static SuperMutantState byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : IDLE;
    }
}

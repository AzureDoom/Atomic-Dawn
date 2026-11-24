package mod.azure.atomicdawn.entity.ghoul;

public enum FeralGhoulState {

    IDLE("idle"),
    WALK("walk"),
    RUN("run"),
    DORMANT("dormant"),
    DEAD("death");

    private static final FeralGhoulState[] VALUES = values();

    private final String animation;

    FeralGhoulState(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static FeralGhoulState byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : IDLE;
    }
}

package mod.azure.atomicdawn.entity.ghoul;

public enum FeralGhoulAction {

    NONE(""),
    CLAW_LEFT("claw_left"),
    CLAW_RIGHT("claw_right"),
    LEAP("leap"),
    WAKE("wake"),
    SCREAM("scream");

    private static final FeralGhoulAction[] VALUES = values();

    private final String animation;

    FeralGhoulAction(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static FeralGhoulAction byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

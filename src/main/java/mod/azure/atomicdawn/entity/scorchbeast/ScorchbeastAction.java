package mod.azure.atomicdawn.entity.scorchbeast;

public enum ScorchbeastAction {

    NONE(""),
    STOP(""),
    ATTACK_LEFT("animation.scorchbeast.attack_left"),
    ATTACK_RIGHT("animation.scorchbeast.attack_right"),
    SCREAM("animation.scorchbeast.scream"),
    FLYING_SCREAM("animation.scorchbeast.flying_scream");

    private static final ScorchbeastAction[] VALUES = values();

    private final String animation;

    ScorchbeastAction(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static ScorchbeastAction byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

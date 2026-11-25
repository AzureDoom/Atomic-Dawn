package mod.azure.atomicdawn.entity.scorched;

public enum ScorchedState {

    IDLE("idle"),
    WALK("walk"),
    RUN("run"),
    DORMANT("dormant"),
    DEAD("death");

    public static final int DORMANT_POSES = 5;

    private static final ScorchedState[] VALUES = values();

    private final String animation;

    ScorchedState(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public String animation(int pose) {
        return this == DORMANT ? animation + "_" + Math.floorMod(pose, DORMANT_POSES) : animation;
    }

    public static ScorchedState byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : IDLE;
    }
}

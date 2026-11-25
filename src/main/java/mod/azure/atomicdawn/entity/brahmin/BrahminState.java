package mod.azure.atomicdawn.entity.brahmin;

public enum BrahminState {

    IDLE("idle"),
    WALK("walk"),
    RUN("run"),
    DEAD("death");

    private static final BrahminState[] VALUES = values();

    private final String animation;

    BrahminState(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static BrahminState byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : IDLE;
    }
}

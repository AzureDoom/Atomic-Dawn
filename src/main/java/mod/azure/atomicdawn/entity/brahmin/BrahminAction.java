package mod.azure.atomicdawn.entity.brahmin;

public enum BrahminAction {

    NONE(""),
    GRAZE("graze"),
    CALL("call"),
    SHAKE("shake");

    private static final BrahminAction[] VALUES = values();

    private final String animation;

    BrahminAction(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static BrahminAction byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

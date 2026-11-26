package mod.azure.atomicdawn.entity.misterhandy;

public enum MisterHandyState {

    IDLE("animation.mister_handy.idle"),
    MOVE("animation.mister_handy.move"),
    SCAN("animation.mister_handy.scan"),
    DEAD("animation.mister_handy.death");

    private static final MisterHandyState[] VALUES = values();

    private final String animation;

    MisterHandyState(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static MisterHandyState byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : IDLE;
    }
}

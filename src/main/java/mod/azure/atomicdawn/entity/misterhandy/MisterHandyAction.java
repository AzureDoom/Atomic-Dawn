package mod.azure.atomicdawn.entity.misterhandy;

public enum MisterHandyAction {

    NONE("", 0),
    STOP("", 0),
    SAW("animation.mister_handy.attack_saw", 24),
    FLAMER("animation.mister_handy.attack_flamer", 40),
    CLAW("animation.mister_handy.attack_claw", 22),
    HURT("animation.mister_handy.hurt", 10);

    private static final MisterHandyAction[] VALUES = values();

    private final String animation;

    private final int duration;

    MisterHandyAction(String animation, int duration) {
        this.animation = animation;
        this.duration = duration;
    }

    public String animation() {
        return animation;
    }

    public int duration() {
        return duration;
    }

    public static MisterHandyAction byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

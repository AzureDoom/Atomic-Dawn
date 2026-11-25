package mod.azure.atomicdawn.entity.scorchbeast;

public enum ScorchbeastFlight {

    GROUNDED("animation.scorchbeast.ground_idle", true, false),
    TAKEOFF("animation.scorchbeast.takeoff", false, true),
    FLYING("animation.scorchbeast.fly", true, true),
    DESCENDING("animation.scorchbeast.fly", true, true),
    LANDING("animation.scorchbeast.land", false, false),
    LUNGING("animation.scorchbeast.fly", true, false),
    DEAD("animation.scorchbeast.land", false, false);

    private static final ScorchbeastFlight[] VALUES = values();

    private final String animation;

    private final boolean loop;

    private final boolean airborne;

    ScorchbeastFlight(String animation, boolean loop, boolean airborne) {
        this.animation = animation;
        this.loop = loop;
        this.airborne = airborne;
    }

    public String animation() {
        return animation;
    }

    public boolean loop() {
        return loop;
    }

    public boolean airborne() {
        return airborne;
    }

    public static ScorchbeastFlight byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : GROUNDED;
    }
}

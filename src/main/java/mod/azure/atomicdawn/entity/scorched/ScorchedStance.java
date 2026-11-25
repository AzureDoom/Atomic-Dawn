package mod.azure.atomicdawn.entity.scorched;

public enum ScorchedStance {

    NONE("arms_none"),
    PISTOL("hold_pistol"),
    PISTOL_AIM("aim_pistol"),
    RIFLE("hold_rifle"),
    RIFLE_AIM("aim_rifle"),
    MELEE("hold_melee");

    private static final ScorchedStance[] VALUES = values();

    private final String animation;

    ScorchedStance(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public static ScorchedStance byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

package mod.azure.atomicdawn.entity.scorched;

public enum ScorchedAction {

    NONE(""),
    STOP(""),
    WAKE("wake"),
    ALERT("alert"),
    SWING("swing"),
    PUNCH_LEFT("punch_left"),
    PUNCH_RIGHT("punch_right"),
    BASH("bash"),
    FIRE_PISTOL("fire_pistol"),
    FIRE_RIFLE("fire_rifle"),
    RELOAD_PISTOL("reload_pistol"),
    RELOAD_RIFLE("reload_rifle"),
    RADIO("");

    private static final ScorchedAction[] VALUES = values();

    private final String animation;

    ScorchedAction(String animation) {
        this.animation = animation;
    }

    public String animation() {
        return animation;
    }

    public boolean looping() {
        return this == RELOAD_PISTOL || this == RELOAD_RIFLE;
    }

    public static ScorchedAction byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

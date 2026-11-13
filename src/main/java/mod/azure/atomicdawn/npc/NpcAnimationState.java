package mod.azure.atomicdawn.npc;

import java.util.Locale;

public enum NpcAnimationState {

    IDLE("idle"),
    WALK("walk"),
    RUN("run"),
    TALK("talk"),
    SIT("sit"),
    CROUCH("crouch"),
    DEATH("death"),
    COMBAT("combat");

    private static final NpcAnimationState[] VALUES = values();

    private final String defaultAnimation;

    NpcAnimationState(String defaultAnimation) {
        this.defaultAnimation = defaultAnimation;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String defaultAnimation() {
        return defaultAnimation;
    }

    public static NpcAnimationState byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : IDLE;
    }
}

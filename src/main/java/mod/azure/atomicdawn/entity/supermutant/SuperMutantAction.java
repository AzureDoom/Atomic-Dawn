package mod.azure.atomicdawn.entity.supermutant;

public enum SuperMutantAction {

    NONE("", 0, 0),
    STOP("", 0, 0),
    PUNCH_RIGHT("animation.super_mutant.melee_right", 13, 26),
    PUNCH_LEFT("animation.super_mutant.melee_left", 13, 26),
    SLAM("animation.super_mutant.melee_slam", 21, 36),
    ROAR("animation.super_mutant.roar", 13, 40),
    HURT("animation.super_mutant.hurt", 0, 13),
    FIRE_RIFLE("animation.super_mutant.rifle_fire", 0, 0),
    FIRE_PISTOL("animation.super_mutant.pistol_fire", 0, 0),
    RELOAD_RIFLE("animation.super_mutant.rifle_reload", 0, 100),
    RELOAD_PISTOL("animation.super_mutant.pistol_reload", 0, 100),
    TAUNT("animation.super_mutant.taunt", 10, 40);

    private static final SuperMutantAction[] VALUES = values();

    private final String animation;

    private final int impactTick;

    private final int duration;

    SuperMutantAction(String animation, int impactTick, int duration) {
        this.animation = animation;
        this.impactTick = impactTick;
        this.duration = duration;
    }

    public String animation() {
        return animation;
    }

    public int impactTick() {
        return impactTick;
    }

    public int duration() {
        return duration;
    }

    public boolean looping() {
        return this == RELOAD_RIFLE || this == RELOAD_PISTOL;
    }

    public static SuperMutantAction byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}

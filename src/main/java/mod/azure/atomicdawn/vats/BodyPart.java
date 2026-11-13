package mod.azure.atomicdawn.vats;

import java.util.Locale;

public enum BodyPart {

    HEAD(1.5F, 0.6F, "head", "skull", "face"),
    TORSO(1.0F, 1.0F, "torso", "body", "chest", "spine", "pelvis", "hip"),
    LEFT_ARM(0.85F, 0.75F, "left_arm", "leftarm", "arm_l", "l_arm", "left_hand", "lefthand"),
    RIGHT_ARM(0.85F, 0.75F, "right_arm", "rightarm", "arm_r", "r_arm", "right_hand", "righthand"),
    LEFT_LEG(0.9F, 0.8F, "left_leg", "leftleg", "leg_l", "l_leg", "left_foot", "leftfoot"),
    RIGHT_LEG(0.9F, 0.8F, "right_leg", "rightleg", "leg_r", "r_leg", "right_foot", "rightfoot");

    private static final BodyPart[] VALUES = values();

    private final float damageMultiplier;

    private final float hitChanceMultiplier;

    private final String[] keywords;

    BodyPart(float damageMultiplier, float hitChanceMultiplier, String... keywords) {
        this.damageMultiplier = damageMultiplier;
        this.hitChanceMultiplier = hitChanceMultiplier;
        this.keywords = keywords;
    }

    public float damageMultiplier() {
        return damageMultiplier;
    }

    public float hitChanceMultiplier() {
        return hitChanceMultiplier;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public String translationKey() {
        return "vats.atomicdawn.part." + name().toLowerCase(Locale.ROOT);
    }

    public static BodyPart byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public static BodyPart classify(String name, String bone, java.util.function.Predicate<String> hasTag) {
        for (var part : VALUES) {
            if (hasTag.test("vats:" + part.name().toLowerCase(Locale.ROOT))) {
                return part;
            }
        }
        var text = (name + " " + bone).toLowerCase(Locale.ROOT).replace("-", "_");
        for (var part : VALUES) {
            for (var keyword : part.keywords) {
                if (text.contains(keyword)) {
                    return part;
                }
            }
        }
        return null;
    }
}

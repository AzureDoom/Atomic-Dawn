package mod.azure.atomicdawn.entity.scorchbeast;

import mod.azure.azurelib.hitbox.AzHitboxSet;

public final class ScorchbeastHitboxes {

    public static final AzHitboxSet SET = AzHitboxSet.builder()
        .add("head", h -> h.damageMultiplier(2.0F).tag("vats:head"))
        .add("jaw", h -> h.damageMultiplier(2.0F).tag("vats:head"))
        .add("body", h -> h.tag("vats:torso"))
        .add("upperBody", h -> h.tag("vats:torso"))
        .add("rightArm", h -> h.damageMultiplier(0.9F).tag("vats:right_arm"))
        .add("rightForearm", h -> h.damageMultiplier(0.9F).tag("vats:right_arm"))
        .add("rightWing", h -> h.damageMultiplier(0.9F).tag("vats:right_arm"))
        .add("leftArm", h -> h.damageMultiplier(0.9F).tag("vats:left_arm"))
        .add("leftForearm", h -> h.damageMultiplier(0.9F).tag("vats:left_arm"))
        .add("leftWing", h -> h.damageMultiplier(0.9F).tag("vats:left_arm"))
        .add("rightFrontArm", h -> h.damageMultiplier(0.8F).tag("vats:right_leg"))
        .add("rightFrontForearm", h -> h.damageMultiplier(0.8F).tag("vats:right_leg"))
        .add("rightFrontHand", h -> h.damageMultiplier(0.8F).tag("vats:right_leg"))
        .add("leftFrontArm", h -> h.damageMultiplier(0.8F).tag("vats:left_leg"))
        .add("leftFrontForearm", h -> h.damageMultiplier(0.8F).tag("vats:left_leg"))
        .add("leftFrontHand", h -> h.damageMultiplier(0.8F).tag("vats:left_leg"))
        .add("rightLeg", h -> h.damageMultiplier(0.8F).tag("vats:right_leg"))
        .add("rightShin", h -> h.damageMultiplier(0.8F).tag("vats:right_leg"))
        .add("rightFoot", h -> h.damageMultiplier(0.8F).tag("vats:right_leg"))
        .add("leftLeg", h -> h.damageMultiplier(0.8F).tag("vats:left_leg"))
        .add("leftShin", h -> h.damageMultiplier(0.8F).tag("vats:left_leg"))
        .add("leftFoot", h -> h.damageMultiplier(0.8F).tag("vats:left_leg"))
        .maxReach(10)
        .build();

    private ScorchbeastHitboxes() {}
}

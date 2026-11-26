package mod.azure.atomicdawn.entity.misterhandy;

import mod.azure.azurelib.hitbox.AzHitboxSet;

public final class MisterHandyHitboxes {

    public static final AzHitboxSet SET = AzHitboxSet.builder()
        .add("eye_center", h -> h.damageMultiplier(1.6F).tag("vats:head"))
        .add("optic_center", h -> h.damageMultiplier(2.0F).tag("vats:head"))
        .add("eye_left", h -> h.damageMultiplier(1.6F).tag("vats:head"))
        .add("optic_left", h -> h.damageMultiplier(2.0F).tag("vats:head"))
        .add("eye_right", h -> h.damageMultiplier(1.6F).tag("vats:head"))
        .add("optic_right", h -> h.damageMultiplier(2.0F).tag("vats:head"))
        .add("body", h -> h.tag("vats:torso"))
        .add("thruster", h -> h.damageMultiplier(1.25F).tag("vats:torso"))
        .add("claw_shoulder", h -> h.damageMultiplier(0.8F).tag("vats:torso"))
        .add("claw_elbow", h -> h.damageMultiplier(0.8F).tag("vats:torso"))
        .add("claw_wrist", h -> h.damageMultiplier(0.8F).tag("vats:torso"))
        .add("saw_shoulder", h -> h.damageMultiplier(0.8F).tag("vats:right_arm"))
        .add("saw_elbow", h -> h.damageMultiplier(0.8F).tag("vats:right_arm"))
        .add("saw_wrist", h -> h.damageMultiplier(0.8F).tag("vats:right_arm"))
        .add("saw_tool", h -> h.damageMultiplier(0.8F).tag("vats:right_arm"))
        .add("flamer_shoulder", h -> h.damageMultiplier(0.8F).tag("vats:left_arm"))
        .add("flamer_elbow", h -> h.damageMultiplier(0.8F).tag("vats:left_arm"))
        .add("flamer_wrist", h -> h.damageMultiplier(0.8F).tag("vats:left_arm"))
        .add("flamer_tool", h -> h.damageMultiplier(1.2F).tag("vats:left_arm"))
        .maxReach(3)
        .build();

    private MisterHandyHitboxes() {}
}

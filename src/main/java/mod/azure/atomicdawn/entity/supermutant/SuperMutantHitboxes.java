package mod.azure.atomicdawn.entity.supermutant;

import mod.azure.azurelib.hitbox.AzHitboxSet;

public final class SuperMutantHitboxes {

    public static final AzHitboxSet SET = AzHitboxSet.builder()
        .add("head", h -> h.damageMultiplier(2.0F).tag("vats:head"))
        .add("body", h -> h.tag("vats:torso"))
        .add("right_arm", h -> h.damageMultiplier(0.85F).tag("vats:right_arm"))
        .add("right_forearm", h -> h.damageMultiplier(0.85F).tag("vats:right_arm"))
        .add("left_arm", h -> h.damageMultiplier(0.85F).tag("vats:left_arm"))
        .add("left_forearm", h -> h.damageMultiplier(0.85F).tag("vats:left_arm"))
        .add("right_leg", h -> h.damageMultiplier(0.85F).tag("vats:right_leg"))
        .add("right_shin", h -> h.damageMultiplier(0.85F).tag("vats:right_leg"))
        .add("right_foot", h -> h.damageMultiplier(0.75F).tag("vats:right_leg"))
        .add("left_leg", h -> h.damageMultiplier(0.85F).tag("vats:left_leg"))
        .add("left_shin", h -> h.damageMultiplier(0.85F).tag("vats:left_leg"))
        .add("left_foot", h -> h.damageMultiplier(0.75F).tag("vats:left_leg"))
        .maxReach(4)
        .build();

    private SuperMutantHitboxes() {}
}

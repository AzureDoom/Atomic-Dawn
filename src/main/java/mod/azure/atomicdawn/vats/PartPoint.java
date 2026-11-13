package mod.azure.atomicdawn.vats;

import net.minecraft.world.phys.Vec3;

public record PartPoint(
    BodyPart part,
    Vec3 position,
    float damageMultiplier,
    float hitChanceMultiplier,
    boolean fromHitbox
) {}

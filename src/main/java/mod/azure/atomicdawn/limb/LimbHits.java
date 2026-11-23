package mod.azure.atomicdawn.limb;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.vats.BodyPart;

public final class LimbHits {

    public record Pending(
        Entity target,
        @Nullable BodyPart part,
        Vec3 position
    ) {}

    private static Pending pending;

    private LimbHits() {}

    public static void begin(Entity target, @Nullable BodyPart part, Vec3 position) {
        pending = new Pending(target, part, position);
    }

    public static void end() {
        pending = null;
    }

    public static @Nullable Pending current(Entity target) {
        var hit = pending;
        return hit != null && hit.target() == target ? hit : null;
    }
}

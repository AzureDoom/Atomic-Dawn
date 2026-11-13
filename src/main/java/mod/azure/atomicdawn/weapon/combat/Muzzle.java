package mod.azure.atomicdawn.weapon.combat;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.weapon.action.WeaponActions;

public final class Muzzle {

    private Muzzle() {}

    public static Vec3 position(Player shooter) {
        var look = shooter.getViewVector(1F);
        var eye = shooter.getEyePosition();
        var right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? Vec3.ZERO : right.normalize();
        var up = right.cross(look);
        if (WeaponActions.isAiming(shooter, shooter.getMainHandItem())) {
            return eye.add(look.scale(1D)).add(up.scale(-0.06D));
        }
        var side = shooter.getMainArm() == HumanoidArm.LEFT ? -0.25D : 0.25D;
        return eye.add(look.scale(0.9D)).add(right.scale(side)).add(0, -0.18D, 0);
    }
}

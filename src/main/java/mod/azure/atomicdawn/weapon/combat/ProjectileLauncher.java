package mod.azure.atomicdawn.weapon.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.ExplosiveProjectileEntity;
import mod.azure.atomicdawn.registry.ADEntities;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public final class ProjectileLauncher {

    private ProjectileLauncher() {}

    public static void launch(
        ServerLevel level,
        ServerPlayer shooter,
        ItemStack weapon,
        WeaponDefinition definition,
        float damageMultiplier,
        float coneDeg,
        @Nullable Vec3 aimAt
    ) {
        var parameters = definition.projectile().orElseThrow();
        var start = Muzzle.position(shooter);
        Vec3 direction;
        if (aimAt != null) {
            direction = aimAt.subtract(start).normalize();
        } else {
            direction = SpreadModel.randomDirection(shooter.getViewVector(1F), coneDeg, shooter.getRandom());
        }
        var projectile = new ExplosiveProjectileEntity(ADEntities.MINI_NUKE.get(), level);
        projectile.setOwner(shooter);
        projectile.setPos(start.x, start.y - 0.25, start.z);
        projectile.configure(weapon, definition, parameters, definition.damage().base() * damageMultiplier);
        projectile.setDeltaMovement(direction.scale(parameters.speed()));
        level.addFreshEntity(projectile);
    }
}

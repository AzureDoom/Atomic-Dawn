package mod.azure.atomicdawn.client.powerarmor;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorBonuses;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorEffectType;

public final class TargetingHud {

    private static final double RANGE = 32.0;

    private TargetingHud() {}

    public static boolean highlights(Entity entity) {
        if (
            !(entity instanceof LivingEntity living) || !living.isAlive() || living instanceof ArmorStand
                || living instanceof PowerArmorEntity
        ) {
            return false;
        }
        var player = Minecraft.getInstance().player;
        if (player == null || entity == player || !(player.getVehicle() instanceof PowerArmorEntity armor)) {
            return false;
        }
        return entity.distanceToSqr(player) <= RANGE * RANGE
            && PowerArmorBonuses.of(armor).has(PowerArmorEffectType.TARGETING_HUD);
    }
}

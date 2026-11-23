package mod.azure.atomicdawn.weapon.combat;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.armor.upgrade.ArmorEvents;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.limb.LimbService;
import mod.azure.atomicdawn.mutation.MutationService;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public final class SpreadModel {

    private SpreadModel() {}

    public static float coneDegrees(Player player, WeaponDefinition definition, float bloom, boolean aiming) {
        return coneDegrees(player, definition.accuracy(), bloom, aiming)
            * ArmorEvents.spreadMultiplier(player, definition.category())
            * PerkHooks.spreadMultiplier(player, definition.category());
    }

    public static float coneDegrees(Player player, WeaponDefinition.Accuracy accuracy, float bloom, boolean aiming) {
        var spread = accuracy.baseSpread() + bloom;
        if (aiming) {
            spread *= accuracy.adsMultiplier();
        }
        if (!player.onGround() && !player.isPassenger()) {
            spread *= accuracy.airborneMultiplier();
        } else if (player.getKnownMovement().horizontalDistanceSqr() > 0.03 * 0.03) {
            spread *= accuracy.movingMultiplier();
        }
        if (player.isCrouching()) {
            spread *= accuracy.crouchMultiplier();
        }
        spread *= MutationService.spreadMultiplier(player) * GhoulService.spreadMultiplier(player)
            * LimbService.spreadMultiplier(player);
        return Math.max(0F, spread);
    }

    public static Vec3 randomDirection(Vec3 forward, float coneDegrees, RandomSource random) {
        if (coneDegrees <= 0F) {
            return forward;
        }
        var maxAngle = Math.toRadians(coneDegrees);
        var angle = maxAngle * Math.sqrt(random.nextDouble());
        var theta = random.nextDouble() * Mth.TWO_PI;

        var reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        var right = forward.cross(reference).normalize();
        var up = right.cross(forward).normalize();

        var offset = right.scale(Math.cos(theta)).add(up.scale(Math.sin(theta)));
        return forward.scale(Math.cos(angle)).add(offset.scale(Math.sin(angle))).normalize();
    }
}

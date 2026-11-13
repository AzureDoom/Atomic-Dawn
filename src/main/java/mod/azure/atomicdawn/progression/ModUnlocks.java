package mod.azure.atomicdawn.progression;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.weapon.modification.WeaponModification;

public final class ModUnlocks {

    private ModUnlocks() {}

    public static KnownMods known(Player player) {
        return player.getData(ADAttachments.KNOWN_MODS.get());
    }

    public static boolean isUnlocked(Player player, Holder.Reference<WeaponModification> mod) {
        return !mod.value().requiresUnlock() || player.hasInfiniteMaterials() || known(player).knows(mod.key());
    }

    public static boolean learn(ServerPlayer player, ResourceKey<WeaponModification> mod) {
        var known = known(player);
        if (known.knows(mod)) {
            return false;
        }
        player.setData(ADAttachments.KNOWN_MODS.get(), known.with(mod));
        return true;
    }
}

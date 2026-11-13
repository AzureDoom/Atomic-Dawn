package mod.azure.atomicdawn.pipboy.holotape;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.HolotapePlayPayload;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.PipBoyNotifications;
import mod.azure.atomicdawn.quest.QuestService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;

public final class HolotapeService {

    private HolotapeService() {}

    public static HolotapeLog log(ServerPlayer player) {
        return player.getData(ADAttachments.HOLOTAPES.get());
    }

    public static ItemStack stack(Identifier holotape) {
        var stack = new ItemStack(ADItems.HOLOTAPE.get());
        stack.set(ADDataComponents.HOLOTAPE.get(), holotape);
        return stack;
    }

    public static boolean collect(ServerPlayer player, Identifier id) {
        if (Holotape.get(player.level().registryAccess(), id).isEmpty()) {
            return false;
        }
        var log = log(player);
        if (!log.has(id)) {
            player.setData(ADAttachments.HOLOTAPES.get(), log.with(id));
            PipBoyNotifications.send(
                player,
                PipBoyNotifications.Kind.HOLOTAPE_ADDED,
                0,
                PipBoyNames.key("holotape", id)
            );
            QuestService.evaluate(player);
        }
        return true;
    }

    public static void play(ServerPlayer player, Identifier id) {
        if (collect(player, id)) {
            ADNetwork.sendTo(player, new HolotapePlayPayload(id));
        }
    }
}

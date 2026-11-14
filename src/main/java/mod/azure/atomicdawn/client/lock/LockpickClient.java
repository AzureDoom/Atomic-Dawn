package mod.azure.atomicdawn.client.lock;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import mod.azure.atomicdawn.lock.LockLevel;
import mod.azure.atomicdawn.network.LockpickActionPayload;
import mod.azure.atomicdawn.network.LockpickOpenPayload;
import mod.azure.atomicdawn.network.LockpickStatePayload;

public final class LockpickClient {

    private LockpickClient() {}

    public static void onOpen(LockpickOpenPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.gui.setScreen(new LockpickScreen(payload.pos(), LockLevel.byIndex(payload.level()), payload.pins()));
    }

    public static void onState(LockpickStatePayload payload) {
        if (Minecraft.getInstance().gui.screen() instanceof LockpickScreen screen) {
            screen.onState(payload);
        }
    }

    public static void send(LockpickActionPayload.Action action, float angle) {
        ClientPacketDistributor.sendToServer(new LockpickActionPayload(action, angle));
    }
}

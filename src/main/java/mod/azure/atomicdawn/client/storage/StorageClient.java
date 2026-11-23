package mod.azure.atomicdawn.client.storage;

import net.minecraft.client.Minecraft;

import mod.azure.atomicdawn.network.StorageOpenPayload;

public final class StorageClient {

    private StorageClient() {}

    public static void onOpen(StorageOpenPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.gui.setScreen(new StorageScreen(payload.mode(), payload.pos()));
    }
}

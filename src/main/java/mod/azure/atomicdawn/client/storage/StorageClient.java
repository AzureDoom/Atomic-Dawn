package mod.azure.atomicdawn.client.storage;

import mod.azure.atomicdawn.network.StorageOpenPayload;
import net.minecraft.client.Minecraft;

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

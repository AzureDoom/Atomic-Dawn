package mod.azure.atomicdawn.client.hud.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.input.ADKeyMappings;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class HudEditor {

    private static boolean pending;

    private HudEditor() {}

    public static void requestOpen() {
        pending = true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            pending = false;
            return;
        }
        while (ADKeyMappings.HUD_EDITOR.consumeClick()) {
            pending = true;
        }
        if (pending && minecraft.gui.screen() == null) {
            pending = false;
            minecraft.gui.setScreen(new HudEditorScreen());
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("adhud").executes(_ -> {
            requestOpen();
            return 1;
        }));
    }
}

package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.config.ADConfigHelper;
import mod.azure.atomicdawn.network.PipBoyActionPayload;
import mod.azure.atomicdawn.network.PipBoyNotificationPayload;
import mod.azure.atomicdawn.network.PipBoyStatsPayload;
import mod.azure.atomicdawn.network.SpecialActionPayload;
import mod.azure.atomicdawn.network.SpecialResultPayload;
import mod.azure.atomicdawn.registry.ADSounds;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class PipBoyClient {

    private PipBoyClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        while (ADKeyMappings.PIPBOY.consumeClick()) {
            if (
                minecraft.gui.screen() == null && !player.isSpectator()
                    && AtomicDawn.CONFIG.common.pipboy.general.PIPBOY_ENABLED
            ) {
                open();
            }
        }
        while (ADKeyMappings.PIPBOY_LIGHT.consumeClick()) {
            if (!player.isSpectator()) {
                ClientPacketDistributor.sendToServer(
                    new PipBoyActionPayload(PipBoyActionPayload.Action.TOGGLE_LIGHT, SpecialActionPayload.NONE)
                );
            }
        }
        while (ADKeyMappings.WAYPOINT_HUD.consumeClick()) {
            var config = AtomicDawn.CONFIG.client.hud.waypointCompass;
            ADConfigHelper.setClientBoolean("hud.waypointCompass.WAYPOINT_HUD", !config.WAYPOINT_HUD);
        }
    }

    public static void open() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null && AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_SOUNDS) {
            minecraft.player.playSound(ADSounds.PIPBOY_OPEN.get(), 1F, 1F);
        }
        minecraft.gui.setScreen(new PipBoyScreen());
        ClientPacketDistributor.sendToServer(
            new PipBoyActionPayload(PipBoyActionPayload.Action.OPENED, SpecialActionPayload.NONE)
        );
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PipBoyClientState.reset();
        PipBoyNotificationHud.clear();
    }

    public static void onResult(SpecialResultPayload payload) {
        var minecraft = Minecraft.getInstance();
        var now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        PipBoyClientState.onResult(payload.result(), now);
        if (
            minecraft.player != null && AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_SOUNDS && !payload.result()
                .success()
        ) {
            minecraft.player.playSound(ADSounds.PIPBOY_ERROR.get(), 1F, 1F);
        }
        if (minecraft.gui.screen() instanceof PipBoyScreen screen) {
            screen.refresh();
        }
    }

    public static void onStats(PipBoyStatsPayload payload) {
        PipBoyClientState.stats = payload;
    }

    public static void onNotification(PipBoyNotificationPayload payload) {
        PipBoyNotificationHud.push(payload.kind(), payload.value(), payload.detail());
    }
}

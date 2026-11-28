package mod.azure.atomicdawn.client.hud.fallout;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorClient;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class FalloutHudEvents {

    private FalloutHudEvents() {}

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        var name = event.getName();
        if (
            (name.equals(VanillaGuiLayers.PLAYER_HEALTH) && FalloutHud.health())
                || (name.equals(VanillaGuiLayers.FOOD_LEVEL) && FalloutHud.food())
                || (name.equals(VanillaGuiLayers.ARMOR_LEVEL) && FalloutHud.armor())
                || (name.equals(VanillaGuiLayers.AIR_LEVEL) && FalloutHud.air() && !PowerArmorClient.inPowerArmor())
        ) {
            event.setCanceled(true);
        }
    }
}

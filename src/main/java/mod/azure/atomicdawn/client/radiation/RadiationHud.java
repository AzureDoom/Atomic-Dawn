package mod.azure.atomicdawn.client.radiation;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADMobEffects;
import mod.azure.atomicdawn.registry.ADSounds;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class RadiationHud implements GuiLayer {

    private static final int COLOR = 0xFFFF5A3C;

    private static final int COLOR_TEXT = 0xFFFFD27A;

    private static final int COLOR_BACKGROUND = 0x80000000;

    private static final int COLOR_BAR_BG = 0x60FFFFFF;

    private static final int COLOR_RESIST = 0xFFE07A2A;

    private static final int BAR_WIDTH = 30;

    private static float lastRads = -1F;

    private static float pendingClicks;

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        if (player == null || hudHidden || player.isSpectator()) {
            return;
        }
        var rads = player.getData(ADAttachments.RADIATION.get()).rads();
        var resisting = player.hasEffect(ADMobEffects.RAD_RESISTANCE);
        if (rads < 0.5F && !resisting) {
            return;
        }
        var font = minecraft.font;
        var width = minecraft.getWindow().getGuiScaledWidth();
        var height = minecraft.getWindow().getGuiScaledHeight();
        var label = Component.literal("☢ " + Math.round(rads));
        var blockWidth = Math.max(font.width(label), BAR_WIDTH) + 6;
        var blockHeight = font.lineHeight + 8;
        var x = width / 2 - 91 - 4 - blockWidth;
        var y = height - blockHeight - 2;
        if (x < 2) {
            x = width / 2 - 91;
            y = height - 22 - blockHeight - 2;
        }
        if (!HudLayout.push(graphics, HudElement.RADIATION, x, y, blockWidth, blockHeight)) {
            return;
        }
        graphics.fill(x, y, x + blockWidth, y + blockHeight, COLOR_BACKGROUND);
        graphics.text(font, label, x + 3, y + 3, resisting ? COLOR_RESIST : COLOR_TEXT, true);
        var fraction = Math.clamp(rads / (float) AtomicDawn.CONFIG.common.survival.radiation.sickness.MAX_RADS, 0F, 1F);
        var barY = y + blockHeight - 4;
        graphics.fill(x + 3, barY, x + 3 + BAR_WIDTH, barY + 2, COLOR_BAR_BG);
        graphics.fill(x + 3, barY, x + 3 + Math.round(BAR_WIDTH * fraction), barY + 2, COLOR);
        HudLayout.pop(graphics);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            lastRads = -1F;
            pendingClicks = 0F;
            return;
        }
        var rads = player.getData(ADAttachments.RADIATION.get()).rads();
        if (lastRads >= 0F && rads > lastRads) {
            pendingClicks = Math.min(24F, pendingClicks + (rads - lastRads) * 0.75F);
        }
        lastRads = rads;
        var random = player.getRandom();
        if (pendingClicks >= 1F && random.nextFloat() < 0.6F) {
            pendingClicks -= 1F;
            player.playSound(ADSounds.GEIGER_CLICK.get(), 0.5F, 0.85F + random.nextFloat() * 0.3F);
        }
    }
}

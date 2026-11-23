package mod.azure.atomicdawn.client.thirst;

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
import mod.azure.atomicdawn.thirst.Thirst;
import mod.azure.atomicdawn.thirst.ThirstService;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class ThirstHud implements GuiLayer {

    private static final int COLOR = 0xFF3CA0FF;

    private static final int COLOR_TEXT = 0xFFB8E0FF;

    private static final int COLOR_LOW = 0xFFFF6A5A;

    private static final int COLOR_BACKGROUND = 0x80000000;

    private static final int COLOR_BAR_BG = 0x60FFFFFF;

    private static final int BAR_WIDTH = 30;

    private static final float LOW = 6F;

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        if (
            player == null || hudHidden || !ThirstService.enabled() || player.isCreative() || player.isSpectator()
        ) {
            return;
        }
        var thirst = ThirstService.thirst(player);
        var font = minecraft.font;
        var width = minecraft.getWindow().getGuiScaledWidth();
        var height = minecraft.getWindow().getGuiScaledHeight();
        var label = Component.literal("H2O " + Math.round(thirst));
        var blockWidth = Math.max(font.width(label), BAR_WIDTH) + 6;
        var blockHeight = font.lineHeight + 8;
        var x = width / 2 + 91 + 4;
        var y = height - blockHeight - 2;
        if (x + blockWidth > width - 2) {
            x = width / 2 + 91 - blockWidth;
            y = height - 22 - blockHeight - 2;
        }
        if (!HudLayout.push(graphics, HudElement.THIRST, x, y, blockWidth, blockHeight)) {
            return;
        }
        graphics.fill(x, y, x + blockWidth, y + blockHeight, COLOR_BACKGROUND);
        graphics.text(font, label, x + 3, y + 3, thirst <= LOW ? COLOR_LOW : COLOR_TEXT, true);
        var fraction = Math.clamp(thirst / Thirst.MAX, 0F, 1F);
        var barY = y + blockHeight - 4;
        graphics.fill(x + 3, barY, x + 3 + BAR_WIDTH, barY + 2, COLOR_BAR_BG);
        graphics.fill(x + 3, barY, x + 3 + Math.round(BAR_WIDTH * fraction), barY + 2, COLOR);
        HudLayout.pop(graphics);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !ThirstService.dehydrated(player)) {
            return;
        }
        if (minecraft.options.keySprint.isDown()) {
            minecraft.options.keySprint.setDown(false);
        }
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.isSprinting() && ThirstService.dehydrated(player)) {
            player.setSprinting(false);
        }
    }
}

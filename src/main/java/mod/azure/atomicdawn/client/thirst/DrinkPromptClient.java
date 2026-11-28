package mod.azure.atomicdawn.client.thirst;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiRenderUtil;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.client.hud.fallout.FalloutHud;
import mod.azure.atomicdawn.network.DrinkSourcePayload;
import mod.azure.atomicdawn.thirst.DirtyWater;
import mod.azure.atomicdawn.thirst.SourceDrinking;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class DrinkPromptClient {

    private static final int COLOR_DIRTY = 0xFFFFB347;

    private static final int COLOR_IRRADIATED = 0xFFFF5A3C;

    @Nullable
    private static BlockPos target;

    private static boolean dirty;

    private static boolean irradiated;

    private static long lastSent = -1L;

    private DrinkPromptClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gui.screen() != null) {
            target = null;
            return;
        }
        target = SourceDrinking.target(player);
        if (target != null) {
            var fluid = minecraft.level.getFluidState(target);
            irradiated = DirtyWater.isIrradiated(fluid);
            dirty = !irradiated && DirtyWater.isDirty(minecraft.level, target, fluid);
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || target == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        var now = minecraft.level.getGameTime();
        if (lastSent < 0L || now < lastSent || now - lastSent >= SourceDrinking.cooldown()) {
            lastSent = now;
            ClientPacketDistributor.sendToServer(new DrinkSourcePayload(target));
        }
    }

    public static final class Layer implements ADHudLayer {

        @Override
        public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
            var minecraft = Minecraft.getInstance();
            if (target == null || minecraft.player == null || ADGuiRenderUtil.isHudHidden()) {
                return;
            }
            var font = minecraft.font;
            var key = Component.literal("[")
                .append(minecraft.options.keyUse.getTranslatedKeyMessage())
                .append("]");
            var action = Component.translatable(
                irradiated
                    ? "hud.atomicdawn.drink.irradiated"
                    : dirty ? "hud.atomicdawn.drink.dirty" : "hud.atomicdawn.drink.water"
            );
            var color = irradiated ? COLOR_IRRADIATED : dirty ? COLOR_DIRTY : FalloutHud.primary();
            var gap = 4;
            var width = font.width(key) + gap + font.width(action);
            var x = ADGuiRenderUtil.guiWidth() / 2 - width / 2;
            var y = ADGuiRenderUtil.guiHeight() / 2 + 16;
            graphics.fill(x - 4, y - 3, x + width + 4, y + font.lineHeight + 2, FalloutHud.shadow());
            graphics.fill(x - 4, y + font.lineHeight + 1, x + width + 4, y + font.lineHeight + 2, color);
            graphics.text(font, key, x, y, FalloutHud.primary(), true);
            graphics.text(font, action, x + font.width(key) + gap, y, color, true);
        }
    }
}

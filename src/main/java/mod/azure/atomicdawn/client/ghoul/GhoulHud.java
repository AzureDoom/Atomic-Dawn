package mod.azure.atomicdawn.client.ghoul;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiRenderUtil;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.client.hud.fallout.FalloutHud;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.client.limb.LimbHud;
import mod.azure.atomicdawn.ghoul.FeralTier;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.registry.ADSounds;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class GhoulHud implements ADHudLayer {

    private static final int COLOR_GLOW = 0xFF8CFF5A;

    private static final int COLOR_STABLE = 0xFFFFC04A;

    private static final int COLOR_FERAL = 0xFFFF4A3A;

    private static final int COLOR_TEXT = 0xFFD8F5C0;

    private static final int COLOR_BACKGROUND = 0x80000000;

    private static final int COLOR_BAR_BG = 0x60FFFFFF;

    private static final int BAR_WIDTH = 40;

    private static final int RADIATION_HUD_CLEARANCE = 21;

    private static float lastGlow = -1F;

    private static float pendingClicks;

    @Override
    public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = ADGuiRenderUtil.isHudHidden();
        if (
            player == null || hudHidden || player.isSpectator() || !AtomicDawn.CONFIG.client.effects.ghoul.GHOUL_HUD
                || !GhoulService.isGhoul(player)
        ) {
            return;
        }
        var font = minecraft.font;
        var glowLabel = Component.translatable("ghoul.atomicdawn.hud.glow");
        var stabilityLabel = Component.translatable("ghoul.atomicdawn.hud.stability");
        var labelW = Math.max(font.width(glowLabel), font.width(stabilityLabel));
        var blockW = labelW + 4 + BAR_WIDTH + 6;
        var blockH = 24;
        var width = minecraft.getWindow().getGuiScaledWidth();
        var height = minecraft.getWindow().getGuiScaledHeight();
        var x = width / 2 - 91 - 4 - blockW;
        var y = height - blockH - 2 - RADIATION_HUD_CLEARANCE;
        if (x < 2) {
            x = width / 2 - 91;
            y = height - 22 - blockH - 2 - RADIATION_HUD_CLEARANCE;
        }
        if (FalloutHud.healthDrawn(player)) {
            x = FalloutHud.healthRect(width, height).x() + LimbHud.WIDTH + 4;
            y = FalloutHud.leftStackTop(width, height) - blockH;
        }
        if (!HudLayout.push(graphics, HudElement.GHOUL, x, y, blockW, blockH)) {
            return;
        }
        graphics.fill(x, y, x + blockW, y + blockH, COLOR_BACKGROUND);
        row(graphics, font, glowLabel, x + 3, y + 3, labelW, GhoulService.glowFraction(player), COLOR_GLOW);
        var tier = GhoulService.tier(player);
        var color = tier.ordinal() >= FeralTier.AGITATED.ordinal() ? COLOR_FERAL : COLOR_STABLE;
        if (tier.ordinal() >= FeralTier.NEAR_FERAL.ordinal() && (player.tickCount / 10) % 2 == 0) {
            color = (color & 0x00FFFFFF) | 0x80000000;
        }
        row(graphics, font, stabilityLabel, x + 3, y + 13, labelW, GhoulService.stabilityFraction(player), color);
        HudLayout.pop(graphics);
    }

    private static void row(
        ADGuiGraphics graphics,
        Font font,
        Component label,
        int x,
        int y,
        int labelW,
        float fraction,
        int color
    ) {
        graphics.text(font, label, x, y, COLOR_TEXT, true);
        var barX = x + labelW + 4;
        var barY = y + 3;
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + 3, COLOR_BAR_BG);
        graphics.fill(barX, barY, barX + Math.round(BAR_WIDTH * Math.clamp(fraction, 0F, 1F)), barY + 3, color);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || !GhoulService.isGhoul(player)) {
            lastGlow = -1F;
            pendingClicks = 0F;
            return;
        }
        var glow = GhoulService.state(player).glow();
        if (lastGlow >= 0F && glow > lastGlow && AtomicDawn.CONFIG.client.effects.ghoul.GHOUL_GEIGER) {
            pendingClicks = Math.min(24F, pendingClicks + (glow - lastGlow) * 2F);
        }
        lastGlow = glow;
        var random = player.getRandom();
        if (pendingClicks >= 1F && random.nextFloat() < 0.6F) {
            pendingClicks -= 1F;
            player.playSound(ADSounds.GEIGER_CLICK.get(), 0.5F, 0.7F + random.nextFloat() * 0.3F);
        }
    }
}

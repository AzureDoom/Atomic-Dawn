package mod.azure.atomicdawn.client.hud.fallout;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiRenderUtil;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADMobEffects;

public final class FalloutHealthHud implements ADHudLayer {

    private static final int COLOR_ABSORPTION = 0xFFFFD94A;

    private static final int COLOR_POISON = 0xFF9BC53D;

    private static final int COLOR_WITHER = 0xFF7A7A7A;

    private static final int COLOR_FROZEN = 0xFF9CD8FF;

    private static final int COLOR_AIR = 0xFF8FD3FF;

    private static final int COLOR_RESIST = 0xFFE07A2A;

    @Override
    public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || ADGuiRenderUtil.isHudHidden() || !FalloutHud.healthDrawn(player)) {
            return;
        }
        var font = minecraft.font;
        var rect = FalloutHud.healthRect(ADGuiRenderUtil.guiWidth(), ADGuiRenderUtil.guiHeight());
        var x = rect.x();
        var y = rect.y();
        if (!HudLayout.push(graphics, HudElement.HEALTH, x, y, rect.width(), rect.height())) {
            return;
        }
        var barX = x + FalloutHud.LABEL_WIDTH + FalloutHud.LABEL_GAP;
        var barWidth = rect.width() - FalloutHud.LABEL_WIDTH - FalloutHud.LABEL_GAP;
        var rowY = y + FalloutHud.ROW + 2;
        var barY = rowY + 2;

        var health = player.getHealth();
        var max = Math.max(1F, player.getMaxHealth());
        var absorption = player.getAbsorptionAmount();
        var rads = player.getData(ADAttachments.RADIATION.get()).rads();
        var radLoss = FalloutHud.radiation() ? radiationLoss(player, rads) : 0F;
        var total = max + radLoss;

        var low = health / max <= 0.25F;
        var labelColor = low && FalloutHud.blink(player) ? FalloutHud.warn() : FalloutHud.primary();
        FalloutHud.text(graphics, font, Component.translatable("hud.atomicdawn.fallout.hp"), x, rowY, labelColor);

        FalloutHud.frame(graphics, barX, barY, barWidth, FalloutHud.BAR_HEIGHT);
        var radPixels = radLoss > 0F ? Math.max(1, Math.round(barWidth * radLoss / total)) : 0;
        var healthLimit = barWidth - radPixels;
        var healthPixels = Math.min(healthLimit, Math.round(barWidth * Math.clamp(health / total, 0F, 1F)));
        var absorbPixels = Math.min(
            healthLimit - healthPixels,
            Math.round(barWidth * Math.clamp(absorption / total, 0F, 1F))
        );
        FalloutHud.segment(graphics, barX, barY, 0, healthPixels, healthColor(player, low));
        FalloutHud.segment(graphics, barX, barY, healthPixels, healthPixels + absorbPixels, COLOR_ABSORPTION);
        if (radPixels > 0) {
            var radColor = FalloutHud.rads();
            FalloutHud.segment(graphics, barX, barY, barWidth - radPixels, barWidth, radColor);
            var dark = FalloutHud.scale(radColor, 0.55F, 0xFF);
            for (var px = barWidth - radPixels + 1; px < barWidth; px += 3) {
                graphics.fill(barX + px, barY, barX + px + 1, barY + FalloutHud.BAR_HEIGHT, dark);
            }
            graphics.fill(
                barX + barWidth - radPixels,
                barY - 1,
                barX + barWidth - radPixels + 1,
                barY + FalloutHud.BAR_HEIGHT + 1,
                FalloutHud.shadow()
            );
        }

        if (FalloutHud.air() && (player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply())) {
            var air = Math.clamp(player.getAirSupply() / (float) Math.max(1, player.getMaxAirSupply()), 0F, 1F);
            FalloutHud.icon(graphics, FalloutHud.ICON_AIR, x + 4, y + 1, COLOR_AIR);
            FalloutHud.thin(graphics, barX, y + FalloutHud.ROW, barWidth, air, COLOR_AIR);
        }

        var infoX = barX;
        if (FalloutHud.armor() && player.getArmorValue() > 0) {
            FalloutHud.icon(graphics, FalloutHud.ICON_ARMOR, infoX, y, FalloutHud.dim());
            infoX += FalloutHud.iconWidth(FalloutHud.ICON_ARMOR) + 3;
            FalloutHud.text(
                graphics,
                font,
                Component.literal(Integer.toString(player.getArmorValue())),
                infoX,
                y,
                FalloutHud.dim()
            );
        }
        if (FalloutHud.radiation() && rads >= 0.5F) {
            var resisting = player.hasEffect(ADMobEffects.RAD_RESISTANCE);
            FalloutHud.textRight(
                graphics,
                font,
                Component.literal("\u2622 " + Math.round(rads)),
                barX + barWidth,
                y,
                resisting ? COLOR_RESIST : FalloutHud.rads()
            );
        }
        HudLayout.pop(graphics);
    }

    private static int healthColor(LocalPlayer player, boolean low) {
        if (player.hasEffect(MobEffects.WITHER)) {
            return COLOR_WITHER;
        }
        if (player.hasEffect(MobEffects.POISON)) {
            return COLOR_POISON;
        }
        if (player.isFullyFrozen()) {
            return COLOR_FROZEN;
        }
        return low && FalloutHud.blink(player) ? FalloutHud.warn() : FalloutHud.primary();
    }

    public static float radiationLoss(LocalPlayer player, float rads) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            var modifier = attribute.getModifier(RadiationService.HEALTH_MODIFIER);
            if (modifier != null) {
                return (float) Math.max(0.0, -modifier.amount());
            }
        }
        if (rads <= 0F) {
            return 0F;
        }
        var fraction = Math.clamp(
            rads / RadiationService.maxRads()
                * (float) AtomicDawn.CONFIG.common.survival.radiation.sickness.RADS_MAX_HEALTH_LOSS,
            0F,
            0.95F
        );
        return player.getMaxHealth() * fraction / (1F - fraction);
    }
}

package mod.azure.atomicdawn.client.hud.fallout;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiRenderUtil;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorGauges;
import mod.azure.atomicdawn.client.vats.VatsClient;
import mod.azure.atomicdawn.config.ADColors;
import mod.azure.atomicdawn.vats.ActionPoints;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class FalloutActionPointsHud implements ADHudLayer {

    @Override
    public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (
            player == null || minecraft.level == null || ADGuiRenderUtil.isHudHidden() || player.isSpectator()
                || !FalloutHud.actionPoints() || PowerArmorGauges.active()
        ) {
            return;
        }
        var font = minecraft.font;
        var rect = FalloutHud.actionPointsRect(ADGuiRenderUtil.guiWidth(), ADGuiRenderUtil.guiHeight());
        var x = rect.x();
        var y = rect.y();
        if (!HudLayout.push(graphics, HudElement.ACTION_POINTS, x, y, rect.width(), rect.height())) {
            return;
        }
        var barWidth = rect.width() - FalloutHud.LABEL_WIDTH - FalloutHud.LABEL_GAP;
        var rowY = y + 3;
        var barY = rowY + 2;
        var ap = VatsClient.actionPoints(player);
        var max = Math.max(1F, VatsActions.maxActionPoints(player));
        var definition = Weapons.definition(minecraft.level, player.getMainHandItem());
        var cost = definition == null ? 0F : VatsActions.apCost(player, definition);
        var lacking = definition != null && !player.hasInfiniteMaterials() && ap.points() < cost;
        var color = lacking && FalloutHud.blink(player) ? FalloutHud.warn() : FalloutHud.primary();

        FalloutHud.bar(graphics, x, barY, barWidth, ap.points() / max, color, true);
        if (definition != null && VatsClient.isActive()) {
            var remaining = Math.clamp((ap.points() - cost) / max, 0F, 1F);
            var markX = x + barWidth - Math.round(barWidth * remaining);
            graphics.fill(markX, barY - 1, markX + 1, barY + FalloutHud.BAR_HEIGHT + 1, FalloutHud.warn());
        }
        FalloutHud.textRight(
            graphics,
            font,
            Component.translatable("hud.atomicdawn.fallout.ap"),
            rect.right(),
            rowY,
            color
        );
        if (definition != null && (ap.critical() > 0F || VatsClient.isActive())) {
            var critical = ADColors.argb(AtomicDawn.CONFIG.client.hud.vatsColors.VATS_COLOR_CRITICAL, 0xFFFFE066);
            var critColor = ap.criticalReady() ? critical : FalloutHud.scale(critical, 0.6F, 0xFF);
            FalloutHud.thin(graphics, x, y, barWidth, ap.critical() / ActionPoints.CRITICAL_FULL, critColor);
        }
        HudLayout.pop(graphics);
    }
}

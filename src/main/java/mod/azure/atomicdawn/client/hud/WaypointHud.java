package mod.azure.atomicdawn.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.client.party.PartyClient;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.pipboy.location.Waypoint;
import mod.azure.atomicdawn.registry.ADAttachments;

public final class WaypointHud implements GuiLayer {

    private static final float HALF_SPAN = 90F;

    private static final int HEIGHT = 12;

    private static final String[] CARDINALS = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var config = AtomicDawn.CONFIG.client.hud.waypointCompass;
        if (!config.WAYPOINT_HUD) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        if (
            player == null || minecraft.level == null || hudHidden || minecraft.gui.screen() instanceof PipBoyScreen
        ) {
            return;
        }
        var font = minecraft.font;
        var screenWidth = minecraft.getWindow().getGuiScaledWidth();
        var width = Math.min(config.WAYPOINT_HUD_WIDTH, screenWidth - 8);
        var left = (screenWidth - width) / 2;
        var top = config.WAYPOINT_HUD_Y;
        var centerX = left + width / 2;
        var yaw = player.getViewYRot(deltaTracker.getGameTimeDeltaPartialTick(true));

        if (!HudLayout.push(graphics, HudElement.COMPASS, left, top, width, HEIGHT)) {
            return;
        }
        var color = PipBoyTheme.text();
        graphics.fill(left, top, left + width, top + HEIGHT, PipBoyTheme.withAlpha(PipBoyTheme.background(), 0x90));
        graphics.fill(left, top + HEIGHT - 1, left + width, top + HEIGHT, PipBoyTheme.border());

        var firstTick = (int) Math.ceil((yaw - HALF_SPAN) / 15F) * 15;
        for (var angle = firstTick; angle <= yaw + HALF_SPAN; angle += 15) {
            var x = stripX(Mth.wrapDegrees(angle - yaw), centerX, width);
            if (Math.floorMod(angle, 45) == 0) {
                var label = Component.literal(CARDINALS[Math.floorMod(angle / 45, CARDINALS.length)]);
                var faded = Math.abs(x - centerX) > width / 2 - 6;
                graphics.text(
                    font,
                    label,
                    x - font.width(label) / 2,
                    top + 2,
                    faded ? PipBoyTheme.dim() : color,
                    false
                );
            } else {
                graphics.fill(x, top + HEIGHT - 4, x + 1, top + HEIGHT - 1, PipBoyTheme.dim());
            }
        }
        graphics.fill(centerX, top, centerX + 1, top + 2, color);

        var log = player.getData(ADAttachments.LOCATIONS.get());
        var dimension = minecraft.level.dimension();
        var maxDistance = config.WAYPOINT_HUD_MAX_DISTANCE;
        Waypoint focused = null;
        var focusedOffset = Float.MAX_VALUE;
        var focusedDistance = 0;
        for (var waypoint : log.waypoints()) {
            if (!waypoint.dimension().equals(dimension)) {
                continue;
            }
            var pos = waypoint.pos();
            var dx = pos.getX() + 0.5 - player.getX();
            var dz = pos.getZ() + 0.5 - player.getZ();
            var distance = Math.sqrt(dx * dx + dz * dz + Mth.square(pos.getY() + 0.5 - player.getY()));
            if (maxDistance > 0 && distance > maxDistance) {
                continue;
            }
            var bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
            var relative = Mth.wrapDegrees(bearing - yaw);
            if (Math.abs(relative) > HALF_SPAN) {
                continue;
            }
            var x = stripX(relative, centerX, width);
            marker(graphics, x, top + HEIGHT, waypoint.argb());
            if (Math.abs(relative) < focusedOffset) {
                focusedOffset = Math.abs(relative);
                focused = waypoint;
                focusedDistance = (int) Math.round(distance);
            }
        }
        if (AtomicDawn.CONFIG.client.hud.party.PARTY_ON_COMPASS) {
            var dimensionId = dimension.identifier();
            for (var member : PartyClient.others()) {
                if (!member.online() || !member.dimension().equals(dimensionId)) {
                    continue;
                }
                var dx = member.pos().getX() + 0.5 - player.getX();
                var dz = member.pos().getZ() + 0.5 - player.getZ();
                var relative = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
                if (Math.abs(relative) <= HALF_SPAN) {
                    var x = stripX(relative, centerX, width);
                    graphics.fill(x - 1, top + 3, x + 2, top + HEIGHT - 3, PipBoyTheme.text());
                    graphics.fill(x, top + 2, x + 1, top + HEIGHT - 2, PipBoyTheme.text());
                }
            }
        }
        if (focused != null && focusedOffset <= config.WAYPOINT_HUD_LABEL_ANGLE) {
            label(graphics, font, focused, focusedDistance, centerX, top + HEIGHT + 5, config.WAYPOINT_HUD_DISTANCE);
        }
        HudLayout.pop(graphics);
    }

    private static int stripX(float relative, int centerX, int width) {
        return centerX + Math.round(relative / HALF_SPAN * (width / 2F - 4));
    }

    private static void marker(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x - 2, y, x + 3, y + 1, color);
        graphics.fill(x - 1, y + 1, x + 2, y + 2, color);
        graphics.fill(x, y + 2, x + 1, y + 3, color);
    }

    private static void label(
        GuiGraphicsExtractor graphics,
        Font font,
        Waypoint waypoint,
        int distance,
        int centerX,
        int y,
        boolean showDistance
    ) {
        Component text = waypoint.name().isEmpty()
            ? Component.translatable("hud.atomicdawn.waypoint.unnamed")
            : Component.literal(waypoint.name());
        if (showDistance) {
            text = Component.translatable("hud.atomicdawn.waypoint.distance", text, distance);
        }
        var w = font.width(text);
        graphics.fill(centerX - w / 2 - 3, y - 2, centerX + w / 2 + 3, y + font.lineHeight, 0x80000000);
        graphics.text(font, text, centerX - w / 2, y, waypoint.argb(), true);
    }
}

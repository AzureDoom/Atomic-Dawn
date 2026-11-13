package mod.azure.atomicdawn.client.vats;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.camera.KillCam;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.config.ADColors;
import mod.azure.atomicdawn.vats.ActionPoints;
import mod.azure.atomicdawn.vats.BodyPart;
import mod.azure.atomicdawn.vats.PartPoint;
import mod.azure.atomicdawn.vats.VatsTargeting;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class VatsHud implements GuiLayer {

    private static int primary() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR, 0xFF7CFC9A);
    }

    private static int dim() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_DIM, 0xB07CFC9A);
    }

    private static int warn() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_WARN, 0xFFFFB347);
    }

    private static int critical() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_CRITICAL, 0xFFFFE066);
    }

    private static int miss() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_MISS, 0xFFFF5050);
    }

    private static int background() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_BACKGROUND, 0x80000000);
    }

    private static int barBackground() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_BAR_BACKGROUND, 0x60FFFFFF);
    }

    private static int tint() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_TINT, 0x2234FF6A);
    }

    private static int targetFill() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_TARGET_FILL, 0x3034FF6A);
    }

    private static int targetOutline() {
        return ADColors.argb(AtomicDawn.CONFIG.clientVats.VATS_COLOR_TARGET_OUTLINE, 0xFF34FF6A);
    }

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        if (player == null || minecraft.level == null || hudHidden || player.isSpectator()) {
            return;
        }
        var definition = Weapons.definition(minecraft.level, player.getMainHandItem());
        if (definition == null) {
            return;
        }
        var width = minecraft.getWindow().getGuiScaledWidth();
        var height = minecraft.getWindow().getGuiScaledHeight();
        var font = minecraft.font;
        var ap = VatsClient.actionPoints(player);
        var max = VatsActions.maxActionPoints(player);

        if (VatsClient.isActive()) {
            graphics.fill(0, 0, width, height, tint());
            if (!KillCam.isActive()) {
                renderTargeting(graphics, font, player, definition, ap, width, height);
            }
        }
        renderMeters(graphics, font, player, definition, ap, max, width, height);
    }

    private static void renderMeters(
        GuiGraphicsExtractor graphics,
        Font font,
        LocalPlayer player,
        WeaponDefinition definition,
        ActionPoints ap,
        float max,
        int width,
        int height
    ) {
        var labelWidth = Math.max(font.width("AP"), font.width("CRIT")) + 4;
        var blockWidth = labelWidth + 48 + 8;
        var blockHeight = 2 * font.lineHeight + 8;
        var x = width - blockWidth - 8;
        var y = height - 8 - 31 - 4 - blockHeight;
        if (!HudLayout.push(graphics, HudElement.VATS_METERS, x, y, blockWidth, blockHeight)) {
            return;
        }
        graphics.fill(x, y, x + blockWidth, y + blockHeight, background());

        var barX = x + 4 + labelWidth;
        var apY = y + 4;
        var cost = VatsActions.apCost(player, definition);
        var affordable = player.hasInfiniteMaterials() || ap.points() >= cost;
        graphics.text(font, Component.literal("AP"), x + 4, apY, affordable ? primary() : warn(), true);
        var apFill = Math.round(48 * Math.clamp(ap.points() / max, 0F, 1F));
        graphics.fill(barX, apY + 3, barX + 48, apY + 6, barBackground());
        graphics.fill(barX, apY + 3, barX + apFill, apY + 6, affordable ? primary() : warn());
        if (VatsClient.isActive()) {
            var costX = barX + Math.round(48 * Math.clamp((ap.points() - cost) / max, 0F, 1F));
            graphics.fill(costX, apY + 2, costX + 1, apY + 7, warn());
        }

        var critY = apY + font.lineHeight + 1;
        var ready = ap.criticalReady();
        var critColor = ready ? critical() : dim();
        graphics.text(font, Component.literal("CRIT"), x + 4, critY, critColor, true);
        var critFill = Math.round(48 * ap.critical() / ActionPoints.CRITICAL_FULL);
        graphics.fill(barX, critY + 3, barX + 48, critY + 6, barBackground());
        graphics.fill(barX, critY + 3, barX + critFill, critY + 6, critColor);
        HudLayout.pop(graphics);
    }

    private static void renderTargeting(
        GuiGraphicsExtractor graphics,
        Font font,
        LocalPlayer player,
        WeaponDefinition definition,
        ActionPoints ap,
        int width,
        int height
    ) {
        var centerX = width / 2;
        var now = player.level().getGameTime();
        var title = Component.translatable("hud.atomicdawn.vats");
        graphics.text(font, title, centerX - font.width(title) / 2, 6, primary(), true);

        var target = VatsClient.target();
        if (target == null) {
            var none = Component.translatable("hud.atomicdawn.vats_no_target");
            graphics.text(font, none, centerX - font.width(none) / 2, 18, warn(), true);
        } else {
            var name = target.getDisplayName();
            var info = Component.empty()
                .append(name)
                .append(
                    Component.literal(String.format("  %.0f/%.0f", target.getHealth(), target.getMaxHealth()))
                );
            graphics.text(font, info, centerX - font.width(info) / 2, 18, primary(), true);

            var healthWidth = 80;
            var healthX = centerX - healthWidth / 2;
            var fraction = Math.clamp(target.getHealth() / Math.max(1F, target.getMaxHealth()), 0F, 1F);
            graphics.fill(healthX, 29, healthX + healthWidth, 31, barBackground());
            graphics.fill(healthX, 29, healthX + Math.round(healthWidth * fraction), 31, miss());

            var pt = VatsClient.partialTick();
            var eye = player.getEyePosition(pt);
            var basis = Basis.of(player.getViewXRot(pt), player.getViewYRot(pt));
            var tanHalf = Math.tan(Math.toRadians(VatsClient.fov()) / 2.0);
            var aspect = width / (double) height;

            renderSelectedTargetHighlight(
                graphics,
                target.getBoundingBox(),
                eye,
                basis,
                tanHalf,
                aspect,
                width,
                height
            );

            var markers = Minecraft.getInstance().options.getCameraType().isFirstPerson();
            Map<BodyPart, PartPoint> parts = markers ? VatsClient.parts(target) : Map.of();
            for (var entry : parts.entrySet()) {
                var point = entry.getValue();
                var screen = project(point.position(), eye, basis, tanHalf, aspect, width, height);
                if (screen == null) {
                    continue;
                }
                var selected = entry.getKey() == VatsClient.part();
                var chance = VatsTargeting.hitChance(player, target, definition, point);
                var sx = (int) screen[0];
                var sy = (int) screen[1];
                var color = chance <= 0F
                    ? miss()
                    : selected ? (VatsClient.isCriticalArmed() ? critical() : primary()) : dim();
                var size = selected ? 5 : 3;
                outline(graphics, sx - size, sy - size, sx + size, sy + size, color);
                if (selected) {
                    var label = Component.translatable(point.part().translationKey())
                        .append(Component.literal(" " + Math.round(chance * 100F) + "%"));
                    var labelX = sx + size + 4;
                    var labelY = sy - font.lineHeight / 2;
                    graphics.fill(
                        labelX - 2,
                        labelY - 2,
                        labelX + font.width(label) + 2,
                        labelY + font.lineHeight,
                        background()
                    );
                    graphics.text(font, label, labelX, labelY, color, true);
                } else {
                    var percent = Component.literal(Math.round(chance * 100F) + "%");
                    graphics.text(font, percent, sx + size + 2, sy - font.lineHeight / 2, color, true);
                }
            }
        }

        var bottomY = height - 59;
        var cost = Component.translatable(
            "hud.atomicdawn.vats_cost",
            Math.round(VatsActions.apCost(player, definition))
        );
        graphics.text(font, cost, centerX - font.width(cost) / 2, bottomY, primary(), true);
        Component critLine;
        int critColor;
        if (VatsClient.isCriticalArmed()) {
            critLine = Component.translatable("hud.atomicdawn.vats_critical_armed");
            critColor = critical();
        } else if (ap.criticalReady() || player.hasInfiniteMaterials()) {
            critLine = Component.translatable("hud.atomicdawn.vats_critical_ready");
            critColor = critical();
        } else {
            critLine = Component.translatable("hud.atomicdawn.vats_hint");
            critColor = dim();
        }
        graphics.text(
            font,
            critLine,
            centerX - font.width(critLine) / 2,
            bottomY + font.lineHeight + 1,
            critColor,
            true
        );

        var shot = VatsClient.lastShot(now);
        if (shot != null) {
            Component feedback;
            int color;
            if (shot.critical()) {
                feedback = Component.translatable("hud.atomicdawn.vats_critical");
                color = critical();
            } else if (shot.hit()) {
                feedback = Component.translatable(
                    "hud.atomicdawn.vats_hit",
                    Component.translatable(shot.part().translationKey())
                );
                color = primary();
            } else {
                feedback = Component.translatable("hud.atomicdawn.vats_miss", shot.chancePercent());
                color = miss();
            }
            graphics.text(font, feedback, centerX - font.width(feedback) / 2, height / 2 + 22, color, true);
        }
    }

    private static void renderSelectedTargetHighlight(
        GuiGraphicsExtractor graphics,
        AABB bounds,
        Vec3 eye,
        Basis basis,
        double tanHalf,
        double aspect,
        int width,
        int height
    ) {
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    var corner = new Vec3(
                        x == 0 ? bounds.minX : bounds.maxX,
                        y == 0 ? bounds.minY : bounds.maxY,
                        z == 0 ? bounds.minZ : bounds.maxZ
                    );
                    if (corner.subtract(eye).dot(basis.forward()) <= 0.05D) {
                        return;
                    }
                    var screen = project(corner, eye, basis, tanHalf, aspect, width, height);
                    if (screen == null) {
                        var offset = corner.subtract(eye);
                        var distance = offset.dot(basis.forward());
                        var sx = width / 2.0 * (1 + offset.dot(basis.right()) / (distance * tanHalf * aspect));
                        var sy = height / 2.0 * (1 - offset.dot(basis.up()) / (distance * tanHalf));
                        screen = new double[] { sx, sy };
                    }
                    left = Math.min(left, screen[0]);
                    top = Math.min(top, screen[1]);
                    right = Math.max(right, screen[0]);
                    bottom = Math.max(bottom, screen[1]);
                }
            }
        }
        int x0 = Math.clamp((int) Math.floor(left) - 3, 0, width - 1);
        int y0 = Math.clamp((int) Math.floor(top) - 3, 0, height - 1);
        int x1 = Math.clamp((int) Math.ceil(right) + 3, 0, width - 1);
        int y1 = Math.clamp((int) Math.ceil(bottom) + 3, 0, height - 1);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        graphics.fill(x0, y0, x1 + 1, y1 + 1, targetFill());
        outline(graphics, x0, y0, x1, y1, targetOutline());
    }

    private static void outline(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1 + 1, y0 + 1, color);
        graphics.fill(x0, y1, x1 + 1, y1 + 1, color);
        graphics.fill(x0, y0, x0 + 1, y1 + 1, color);
        graphics.fill(x1, y0, x1 + 1, y1 + 1, color);
    }

    private record Basis(
        Vec3 forward,
        Vec3 right,
        Vec3 up
    ) {

        static Basis of(float xRot, float yRot) {
            var forward = Vec3.directionFromRotation(xRot, yRot);
            var right = forward.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, yRot - 90F) : right.normalize();
            var up = right.cross(forward).normalize();
            return new Basis(forward, right, up);
        }
    }

    private static double @Nullable [] project(
        Vec3 world,
        Vec3 eye,
        Basis basis,
        double tanHalf,
        double aspect,
        int width,
        int height
    ) {
        var d = world.subtract(eye);
        var z = d.dot(basis.forward());
        if (z <= 0.05) {
            return null;
        }
        var ndcX = d.dot(basis.right()) / (z * tanHalf * aspect);
        var ndcY = d.dot(basis.up()) / (z * tanHalf);
        if (Math.abs(ndcX) > 1.1 || Math.abs(ndcY) > 1.1) {
            return null;
        }
        return new double[] { width / 2.0 * (1 + ndcX), height / 2.0 * (1 - ndcY) };
    }
}

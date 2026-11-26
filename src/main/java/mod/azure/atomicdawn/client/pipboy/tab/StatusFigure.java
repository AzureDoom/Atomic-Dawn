package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyDraw;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.limb.LimbService;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;
import mod.azure.atomicdawn.powerarmor.PowerCoreItem;
import mod.azure.atomicdawn.vats.BodyPart;

final class StatusFigure {

    private StatusFigure() {}

    private record Callout(
        boolean left,
        int attachX,
        int attachY,
        Component name,
        ItemStack stack,
        float condition
    ) {}

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect panel, Player player) {
        var millis = System.currentTimeMillis();
        var bob = (int) Math.round(Math.sin(millis / 650.0) * 0.6);
        if (player.getVehicle() instanceof PowerArmorEntity armor) {
            powerArmor(graphics, screen, panel, armor, bob);
        } else {
            normal(graphics, screen, panel, player, bob);
        }
    }

    private static void normal(ADGuiGraphics graphics, PipBoyScreen screen, Rect panel, Player player, int bob) {
        var u = Math.clamp(Math.min((panel.height() - 34) / 32, (panel.width() - 130) / 16), 2, 4);
        var w = 16 * u;
        var ox = panel.x() + (panel.width() - w) / 2;
        var oy = panel.y() + 4 + bob;
        var grid = new Grid(ox, oy, u);

        var head = player.getItemBySlot(EquipmentSlot.HEAD);
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        var legs = player.getItemBySlot(EquipmentSlot.LEGS);
        var feet = player.getItemBySlot(EquipmentSlot.FEET);

        var skin = PipBoyTheme.phosphor(0.5F, 0xFF);
        limb(graphics, grid.r(4, 0, 8, 8), player, BodyPart.HEAD, skin);
        limb(graphics, grid.r(4, 8, 8, 12), player, BodyPart.TORSO, skin);
        limb(graphics, grid.r(0, 8, 4, 12), player, BodyPart.RIGHT_ARM, skin);
        limb(graphics, grid.r(12, 8, 4, 12), player, BodyPart.LEFT_ARM, skin);
        limb(graphics, grid.r(4, 20, 4, 12), player, BodyPart.RIGHT_LEG, skin);
        limb(graphics, grid.r(8, 20, 4, 12), player, BodyPart.LEFT_LEG, skin);
        if (head.isEmpty()) {
            PipBoyDraw.fill(graphics, grid.r(5, 4, 2, 1), PipBoyTheme.text());
            PipBoyDraw.fill(graphics, grid.r(9, 4, 2, 1), PipBoyTheme.text());
            PipBoyDraw.fill(graphics, grid.r(6, 6, 4, 1), PipBoyTheme.phosphor(0.35F, 0xFF));
        }

        if (!chest.isEmpty()) {
            var c = color(condition(chest));
            plate(graphics, grid.r(4, 8, 8, 12).inset(-1), c);
            plate(graphics, grid.r(0, 8, 4, 6).inset(-1), c);
            plate(graphics, grid.r(12, 8, 4, 6).inset(-1), c);
            PipBoyDraw.fill(graphics, grid.r(7, 10, 2, 7), PipBoyTheme.scaled(c, 0.75F, 0xFF));
        }
        if (!legs.isEmpty()) {
            var c = color(condition(legs));
            plate(graphics, grid.r(4, 18, 8, 3).inset(-1), c);
            plate(graphics, grid.r(4, 20, 4, 9).inset(-1), c);
            plate(graphics, grid.r(8, 20, 4, 9).inset(-1), c);
        }
        if (!feet.isEmpty()) {
            var c = color(condition(feet));
            plate(graphics, grid.r(4, 29, 4, 3).inset(-1), c);
            plate(graphics, grid.r(8, 29, 4, 3).inset(-1), c);
        }
        if (!head.isEmpty()) {
            var c = color(condition(head));
            plate(graphics, grid.r(4, 0, 8, 8).inset(-1), c);
            PipBoyDraw.fill(graphics, grid.r(5, 4, 6, 1), PipBoyTheme.glass());
            PipBoyDraw.fill(graphics, grid.r(5, 4, 6, 1).inset(1), PipBoyTheme.phosphor(0.2F, 0xFF));
        }

        var callouts = List.of(
            new Callout(true, grid.x(4) - 2, grid.y(4), name(head, "head"), head, condition(head)),
            new Callout(false, grid.x(16) + 2, grid.y(11), name(chest, "chest"), chest, condition(chest)),
            new Callout(true, grid.x(4) - 2, grid.y(24), name(legs, "legs"), legs, condition(legs)),
            new Callout(false, grid.x(12) + 2, grid.y(30), name(feet, "feet"), feet, condition(feet))
        );
        callouts(graphics, screen, panel, callouts, ox - 10, ox + w + 10);

        readout(
            graphics,
            screen,
            panel,
            grid.y(32) + 6,
            Component.literal(String.valueOf(player.getArmorValue())),
            Component.translatable("pipboy.atomicdawn.status.figure.armor")
        );
    }

    private static void powerArmor(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect panel,
        PowerArmorEntity armor,
        int bob
    ) {
        var u = Math.clamp(Math.min((panel.height() - 34) / 36, (panel.width() - 130) / 24), 2, 4);
        var w = 24 * u;
        var ox = panel.x() + (panel.width() - w) / 2;
        var oy = panel.y() + 4 + bob;
        var g = new Grid(ox, oy, u);

        var helmet = armor.getPiece(PowerArmorSlot.HELMET);
        var torso = armor.getPiece(PowerArmorSlot.TORSO);
        var rightArm = armor.getPiece(PowerArmorSlot.RIGHT_ARM);
        var leftArm = armor.getPiece(PowerArmorSlot.LEFT_ARM);
        var rightLeg = armor.getPiece(PowerArmorSlot.RIGHT_LEG);
        var leftLeg = armor.getPiece(PowerArmorSlot.LEFT_LEG);

        var skin = PipBoyTheme.phosphor(0.45F, 0xFF);
        body(graphics, g.r(9, 2, 6, 6), skin);
        body(graphics, g.r(8, 9, 8, 12), skin);
        body(graphics, g.r(3, 10, 3, 12), skin);
        body(graphics, g.r(18, 10, 3, 12), skin);
        body(graphics, g.r(8, 21, 3, 13), skin);
        body(graphics, g.r(13, 21, 3, 13), skin);

        var frame = PipBoyTheme.phosphor(0.6F, 0xFF);
        PipBoyDraw.outline(graphics, g.r(6, 9, 12, 12), frame);
        graphics.fill(g.x(12), g.y(9), g.x(12) + 1, g.y(21), frame);
        PipBoyDraw.outline(graphics, g.r(2, 9, 20, 2), frame);
        for (var x : new int[] { 2, 21 }) {
            graphics.fill(g.x(x), g.y(9), g.x(x) + 2, g.y(25), frame);
        }
        for (var x : new int[] { 7, 15 }) {
            graphics.fill(g.x(x), g.y(21), g.x(x) + 2, g.y(36), frame);
        }
        PipBoyDraw.outline(graphics, g.r(6, 34, 5, 2), frame);
        PipBoyDraw.outline(graphics, g.r(13, 34, 5, 2), frame);

        if (!rightLeg.isEmpty()) {
            var c = color(PowerArmorPieceItem.condition(rightLeg));
            plate(graphics, g.r(6, 22, 5, 11), c);
            plate(graphics, g.r(5, 26, 7, 3), c);
            plate(graphics, g.r(5, 33, 7, 3), c);
        }
        if (!leftLeg.isEmpty()) {
            var c = color(PowerArmorPieceItem.condition(leftLeg));
            plate(graphics, g.r(13, 22, 5, 11), c);
            plate(graphics, g.r(12, 26, 7, 3), c);
            plate(graphics, g.r(12, 33, 7, 3), c);
        }
        if (!torso.isEmpty()) {
            var c = color(PowerArmorPieceItem.condition(torso));
            plate(graphics, g.r(5, 9, 14, 13), c);
            plate(graphics, g.r(8, 11, 8, 5), c);
            plate(graphics, g.r(6, 19, 12, 2), c);
            var vent = PipBoyTheme.scaled(c, 0.3F, 0xFF);
            for (var i = 0; i < 3; i++) {
                PipBoyDraw.fill(graphics, new Rect(g.x(9), g.y(17) + i * 2, 6 * u, 1), vent);
            }
        }
        if (!rightArm.isEmpty()) {
            var c = color(PowerArmorPieceItem.condition(rightArm));
            plate(graphics, g.r(1, 13, 5, 9), c);
            plate(graphics, g.r(1, 22, 5, 3), c);
            plate(graphics, g.r(0, 8, 7, 5), c);
        }
        if (!leftArm.isEmpty()) {
            var c = color(PowerArmorPieceItem.condition(leftArm));
            plate(graphics, g.r(18, 13, 5, 9), c);
            plate(graphics, g.r(18, 22, 5, 3), c);
            plate(graphics, g.r(17, 8, 7, 5), c);
        }
        if (!helmet.isEmpty()) {
            var c = color(PowerArmorPieceItem.condition(helmet));
            plate(graphics, g.r(7, 0, 10, 9), c);

            PipBoyDraw.fill(graphics, g.r(8, 3, 8, 2), PipBoyTheme.glass());
            PipBoyDraw.fill(
                graphics,
                g.r(8, 3, 8, 2).inset(1),
                armor.isFunctioning() ? PipBoyTheme.text() : PipBoyTheme.faint()
            );
            var grille = PipBoyTheme.scaled(c, 0.3F, 0xFF);
            for (var i = 0; i < 3; i++) {
                PipBoyDraw.fill(graphics, new Rect(g.x(10), g.y(6) + i * 2, 4 * u, 1), grille);
            }
        }

        var callouts = new ArrayList<Callout>();
        callouts.add(paCallout(true, g.x(7) - 2, g.y(4), helmet, PowerArmorSlot.HELMET));
        callouts.add(paCallout(false, g.x(19) + 2, g.y(14), torso, PowerArmorSlot.TORSO));
        callouts.add(paCallout(true, g.x(0) - 2, g.y(17), rightArm, PowerArmorSlot.RIGHT_ARM));
        callouts.add(paCallout(false, g.x(24) + 2, g.y(17), leftArm, PowerArmorSlot.LEFT_ARM));
        callouts.add(paCallout(true, g.x(5) - 2, g.y(29), rightLeg, PowerArmorSlot.RIGHT_LEG));
        callouts.add(paCallout(false, g.x(19) + 2, g.y(29), leftLeg, PowerArmorSlot.LEFT_LEG));
        callouts(graphics, screen, panel, callouts, ox - 10, ox + w + 10);

        var core = armor.getCore();
        Component value = core.isEmpty()
            ? Component.translatable("pipboy.atomicdawn.status.figure.no_core")
            : Component.literal(Math.round(PowerCoreItem.fraction(core) * 100F) + "%");
        readout(
            graphics,
            screen,
            panel,
            g.y(36) + 6,
            value,
            Component.translatable("pipboy.atomicdawn.status.figure.core")
        );
    }

    private static Callout paCallout(boolean left, int x, int y, ItemStack stack, PowerArmorSlot slot) {
        var name = stack.isEmpty() ? Component.translatable(slot.translationKey()) : stack.getHoverName();
        return new Callout(left, x, y, name, stack, PowerArmorPieceItem.condition(stack));
    }

    private static void callouts(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect panel,
        List<Callout> callouts,
        int leftEdge,
        int rightEdge
    ) {
        var font = screen.font();
        int[] nextY = { panel.y(), panel.y() };
        for (var callout : callouts) {
            var side = callout.left() ? 0 : 1;
            var y = Math.max(callout.attachY() - 6, nextY[side]);
            nextY[side] = y + 17;
            var present = !callout.stack().isEmpty();
            var textColor = present ? PipBoyTheme.text() : PipBoyTheme.disabled();
            var maxW = callout.left() ? leftEdge - panel.x() - 2 : panel.right() - rightEdge - 2;
            if (maxW < 16) {
                continue;
            }
            var w = Math.min(font.width(callout.name()), maxW);
            var x = callout.left() ? leftEdge - w : rightEdge;
            screen.fitted(graphics, callout.name(), x, y, maxW, textColor);

            var barW = Math.min(44, maxW);
            var barX = callout.left() ? leftEdge - barW : rightEdge;
            if (present && callout.stack().isDamageableItem()) {
                var color = PipBoyTheme.scaled(color(callout.condition()), 1F, 0xFF);
                PipBoyDraw.fill(graphics, new Rect(barX, y + 10, barW, 3), PipBoyTheme.faint());
                PipBoyDraw.fill(
                    graphics,
                    new Rect(barX, y + 10, Math.round(barW * Math.clamp(callout.condition(), 0F, 1F)), 3),
                    color
                );
            } else if (!present) {
                PipBoyDraw.fill(graphics, new Rect(barX, y + 11, barW, 1), PipBoyTheme.faint());
            }

            var lineY = y + 4;
            var from = callout.left() ? leftEdge + 3 : rightEdge - 3;
            var line = present ? PipBoyTheme.border() : PipBoyTheme.faint();
            if (lineY == callout.attachY()) {
                var x0 = Math.min(from, callout.attachX());
                var x1 = Math.max(from, callout.attachX());
                graphics.fill(x0, lineY, x1, lineY + 1, line);
            } else {
                PipBoyDraw.line(graphics, from, lineY, callout.attachX(), callout.attachY(), 1, line);
            }
            graphics.fill(
                callout.attachX() - 1,
                callout.attachY() - 1,
                callout.attachX() + 1,
                callout.attachY() + 1,
                line
            );
        }
    }

    private static void readout(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect panel,
        int y,
        Component value,
        Component caption
    ) {
        if (y + 22 > panel.bottom()) {
            y = panel.bottom() - 22;
        }
        var font = screen.font();
        var cx = panel.x() + panel.width() / 2;
        var scale = 1.5F;
        var vw = font.width(value) * scale;
        graphics.push();
        graphics.translate(cx - vw / 2F, y);
        graphics.scale(scale, scale);
        screen.text(graphics, value, 0, 0, PipBoyTheme.text());
        graphics.pop();
        screen.text(graphics, caption, cx - font.width(caption) / 2, y + 14, PipBoyTheme.dim());
    }

    private static void limb(ADGuiGraphics graphics, Rect r, Player player, BodyPart part, int skin) {
        var condition = LimbService.state(player).get(part);
        if (condition >= 1F) {
            body(graphics, r, skin);
            return;
        }
        var rim = condition <= 0F ? PipBoyTheme.bad() : condition < 0.5F ? PipBoyTheme.warn() : skin;
        body(graphics, r, rim);
        if (condition <= 0F) {
            var pulse = 0.25F + 0.15F * (float) Math.sin(System.currentTimeMillis() / 220.0);
            PipBoyDraw.fill(graphics, r.inset(1), PipBoyTheme.scaled(PipBoyTheme.bad() & 0xFFFFFF, pulse, 0xFF));
        }
    }

    private static void body(ADGuiGraphics graphics, Rect r, int rim) {
        PipBoyDraw.fill(graphics, r, PipBoyTheme.phosphor(0.14F, 0xFF));
        PipBoyDraw.outline(graphics, r, rim);
    }

    private static void plate(ADGuiGraphics graphics, Rect r, int color) {
        PipBoyDraw.fill(graphics, r, PipBoyTheme.scaled(color, 0.38F, 0xFF));
        PipBoyDraw.outline(graphics, r, PipBoyTheme.scaled(color, 0.85F, 0xFF));
        if (r.width() > 3 && r.height() > 3) {
            graphics.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.y() + 2, PipBoyTheme.scaled(color, 1F, 0xFF));
            graphics.fill(
                r.x() + 1,
                r.bottom() - 2,
                r.right() - 1,
                r.bottom() - 1,
                PipBoyTheme.scaled(color, 0.25F, 0xFF)
            );
        }
    }

    private static float condition(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return 1F;
        }
        return 1F - stack.getDamageValue() / (float) stack.getMaxDamage();
    }

    private static int color(float condition) {
        var argb = condition > 0.5F ? PipBoyTheme.text() : condition > 0.25F ? PipBoyTheme.warn() : PipBoyTheme.bad();
        return argb & 0xFFFFFF;
    }

    private static Component name(ItemStack stack, String slot) {
        return stack.isEmpty()
            ? Component.translatable("pipboy.atomicdawn.status.slot." + slot)
            : stack.getHoverName();
    }

    private record Grid(
        int ox,
        int oy,
        int u
    ) {

        int x(int units) {
            return ox + units * u;
        }

        int y(int units) {
            return oy + units * u;
        }

        Rect r(int x, int y, int w, int h) {
            return new Rect(ox + x * u, oy + y * u, w * u, h * u);
        }
    }
}

package mod.azure.atomicdawn.client.powerarmor;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;
import mod.azure.atomicdawn.powerarmor.PowerCoreItem;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorBonuses;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorEffectType;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.weapon.action.VatsActions;

public final class PowerArmorHud implements GuiLayer {

    private static final int PANEL_W = 150;

    private static final int PANEL_H = 86;

    private static final int AP_H = 15;

    private static final int AP_GAP = 3;

    private static final long AP_LINGER_TICKS = 20L;

    private static final long AP_REFILL_TICKS = 100L;

    private static final int[][] SILHOUETTE = {
        { 11, 0, 8, 7 },
        { 9, 8, 12, 13 },
        { 22, 8, 6, 12 },
        { 2, 8, 6, 12 },
        { 16, 22, 6, 14 },
        { 8, 22, 6, 14 } };

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (
            player == null || minecraft.gui.hud.isHidden() || !(player.getVehicle() instanceof PowerArmorEntity armor)
        ) {
            return;
        }
        renderPanel(graphics, minecraft, player, armor);
        renderJetPackAp(graphics, minecraft, player, armor);
    }

    private static void renderJetPackAp(
        GuiGraphicsExtractor graphics,
        Minecraft minecraft,
        LocalPlayer player,
        PowerArmorEntity armor
    ) {
        if (!PowerArmorBonuses.of(armor).has(PowerArmorEffectType.JET_PACK)) {
            return;
        }
        var since = armor.level().getGameTime() - PowerArmorClient.lastJetPackTime();
        var ap = VatsActions.actionPoints(player);
        var max = VatsActions.maxActionPoints(player);
        var full = ap.points() >= max - 0.01F;
        if (since > AP_REFILL_TICKS || (since > AP_LINGER_TICKS && full)) {
            return;
        }
        var height = minecraft.getWindow().getGuiScaledHeight();
        var x = 6;
        var y = height - PANEL_H - 6 - AP_GAP - AP_H;
        if (!HudLayout.push(graphics, HudElement.POWER_ARMOR_AP, x, y, PANEL_W, AP_H)) {
            return;
        }
        graphics.fill(x, y, x + PANEL_W, y + AP_H, 0xB0000000);
        graphics.fill(x, y, x + PANEL_W, y + 1, PipBoyTheme.border());
        var cost = (float) AtomicDawn.CONFIG.powerArmor.POWER_ARMOR_JETPACK_AP_COST;
        var empty = !player.hasInfiniteMaterials() && ap.points() < cost;
        var blink = (player.tickCount / 5) % 2 == 0;
        bar(
            graphics,
            minecraft.font,
            x + 4,
            y + 1,
            PANEL_W - 8,
            Component.translatable("power_armor.atomicdawn.hud.ap"),
            max <= 0F ? 0F : ap.points() / max,
            empty && blink ? PipBoyTheme.bad() : PipBoyTheme.text(),
            Math.round(ap.points()) + "/" + Math.round(max)
        );
        HudLayout.pop(graphics);
    }

    private static void renderPanel(
        GuiGraphicsExtractor graphics,
        Minecraft minecraft,
        LocalPlayer player,
        PowerArmorEntity armor
    ) {
        var font = minecraft.font;
        var height = minecraft.getWindow().getGuiScaledHeight();
        var x = 6;
        var y = height - PANEL_H - 6;
        if (!HudLayout.push(graphics, HudElement.POWER_ARMOR, x, y, PANEL_W, PANEL_H)) {
            return;
        }
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xB0000000);
        graphics.fill(x, y, x + PANEL_W, y + 1, PipBoyTheme.border());

        var blink = (player.tickCount / 5) % 2 == 0;
        var sx = x + 6;
        var sy = y + 8;
        for (var slot : PowerArmorSlot.all()) {
            var box = SILHOUETTE[slot.ordinal()];
            var piece = armor.getPiece(slot);
            int color;
            if (piece.isEmpty()) {
                color = 0xFF2A2A2A;
            } else if (PowerArmorPieceItem.isBroken(piece)) {
                color = blink ? PipBoyTheme.bad() : 0xFF5A1A14;
            } else {
                color = conditionColor(PowerArmorPieceItem.condition(piece));
            }
            graphics.fill(sx + box[0], sy + box[1], sx + box[0] + box[2], sy + box[1] + box[3], color);
        }

        var bx = x + 44;
        var by = y + 6;
        var barW = PANEL_W - 52;
        bar(
            graphics,
            font,
            bx,
            by,
            barW,
            Component.translatable("power_armor.atomicdawn.hud.hp"),
            player.getHealth() / player.getMaxHealth(),
            PipBoyTheme.text(),
            Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth())
        );
        var rads = player.getData(ADAttachments.RADIATION.get()).rads();
        bar(
            graphics,
            font,
            bx,
            by + 16,
            barW,
            Component.translatable("power_armor.atomicdawn.hud.rads"),
            rads / RadiationService.maxRads(),
            PipBoyTheme.warn(),
            String.valueOf(Math.round(rads))
        );

        var core = armor.getCore();
        var coreFraction = PowerCoreItem.fraction(core);
        var coreText = core.isEmpty()
            ? Component.translatable("power_armor.atomicdawn.hud.no_core").getString()
            : Math.round(coreFraction * 100F) + "%";
        bar(
            graphics,
            font,
            bx,
            by + 32,
            barW,
            Component.translatable("power_armor.atomicdawn.hud.core"),
            coreFraction,
            coreFraction < 0.15F && blink ? PipBoyTheme.bad() : PipBoyTheme.text(),
            coreText
        );

        var drain = armor.drainPerSecond();
        Component status;
        if (core.isEmpty() || !armor.isFunctioning()) {
            status = Component.translatable("power_armor.atomicdawn.hud.no_power");
        } else if (armor.landingTicks() > 0) {
            status = Component.translatable("power_armor.atomicdawn.hud.landing");
        } else if (drain > 0.01F) {
            var seconds = (int) (PowerCoreItem.charge(core) / drain);
            status = Component.translatable(
                "power_armor.atomicdawn.hud.drain",
                String.format(Locale.ROOT, "%.1f", drain),
                String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
            );
        } else {
            status = Component.translatable("power_armor.atomicdawn.hud.idle");
        }
        graphics.text(font, status, bx, by + 50, armor.isFunctioning() ? PipBoyTheme.dim() : PipBoyTheme.bad(), false);

        var missing = 0;
        var broken = 0;
        for (var slot : PowerArmorSlot.all()) {
            var piece = armor.getPiece(slot);
            if (piece.isEmpty()) {
                missing++;
            } else if (PowerArmorPieceItem.isBroken(piece)) {
                broken++;
            }
        }
        if (missing > 0 || broken > 0) {
            var warning = Component.translatable("power_armor.atomicdawn.hud.pieces", missing, broken);
            graphics.text(font, warning, sx, y + PANEL_H - 11, PipBoyTheme.warn(), false);
        }
        HudLayout.pop(graphics);
    }

    private static int conditionColor(float condition) {
        if (condition > 0.6F) {
            return PipBoyTheme.text();
        }
        if (condition > 0.25F) {
            return PipBoyTheme.warn();
        }
        return PipBoyTheme.bad();
    }

    private static void bar(
        GuiGraphicsExtractor graphics,
        Font font,
        int x,
        int y,
        int width,
        Component label,
        float fraction,
        int color,
        String value
    ) {
        graphics.text(font, label, x, y, PipBoyTheme.text(), false);
        var valueW = font.width(value);
        graphics.text(font, Component.literal(value), x + width - valueW, y, PipBoyTheme.dim(), false);
        graphics.fill(x, y + 10, x + width, y + 13, PipBoyTheme.faint());
        graphics.fill(x, y + 10, x + Math.round(width * Math.clamp(fraction, 0F, 1F)), y + 13, color);
    }
}

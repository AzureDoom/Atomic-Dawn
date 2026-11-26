package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.ghoul.GhoulClient;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.ghoul.GhoulState;
import mod.azure.atomicdawn.ghoul.GhoulTreatmentService;
import mod.azure.atomicdawn.network.GhoulActionPayload;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.special.SpecialStatResolver;

final class GhoulPage {

    private static final int ROW = 11;

    private static final int BUTTON_H = 13;

    private static int snapshot = Integer.MIN_VALUE;

    private GhoulPage() {}

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        snapshot = snapshot(player);
        if (!GhoulService.isGhoul(player)) {
            return;
        }
        var state = GhoulService.state(player);
        var y = area.bottom() - BUTTON_H;
        var x = area.x() + 4;
        if (AtomicDawn.CONFIG.common.progression.ghoul.general.ALLOW_APPEARANCE_VARIANTS) {
            var variant = state.appearanceVariant();
            screen.button(Component.literal("<"), x, y, 14, BUTTON_H, true, () -> sendVariant(variant - 1));
            var label = Component.translatable("pipboy.atomicdawn.ghoul.variant", variant + 1);
            var labelW = screen.font().width(label) + 10;
            screen.button(label, x + 16, y, labelW, BUTTON_H, false, () -> {});
            screen.button(
                Component.literal(">"),
                x + 18 + labelW,
                y,
                14,
                BUTTON_H,
                true,
                () -> sendVariant(variant + 1)
            );
            x += 40 + labelW;
        }
        var visible = state.cosmeticVisible();
        var toggle = Component.translatable(
            visible ? "pipboy.atomicdawn.ghoul.cosmetics_shown" : "pipboy.atomicdawn.ghoul.cosmetics_hidden"
        );
        screen.button(
            toggle,
            x,
            y,
            screen.font().width(toggle) + 10,
            BUTTON_H,
            true,
            () -> GhoulClient.send(GhoulActionPayload.Action.SET_COSMETIC_VISIBLE, visible ? 0 : 1)
        );
    }

    private static void sendVariant(int variant) {
        GhoulClient.send(
            GhoulActionPayload.Action.SET_VARIANT,
            Math.floorMod(variant, GhoulState.APPEARANCE_VARIANTS)
        );
    }

    private static int snapshot(Player player) {
        var state = GhoulService.state(player);
        var hash = GhoulService.isGhoul(player) ? 1 : 0;
        hash = hash * 31 + state.appearanceVariant();
        return hash * 31 + (state.cosmeticVisible() ? 1 : 0);
    }

    static void tick(PipBoyScreen screen) {
        var player = Minecraft.getInstance().player;
        if (player != null && snapshot(player) != snapshot) {
            screen.refresh();
        }
    }

    static void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var x = area.x() + 4;
        var y = area.y() + 4;
        var width = area.width() - 8;
        var bottom = area.bottom() - BUTTON_H - 4;
        if (!GhoulService.enabled()) {
            screen.text(graphics, Component.translatable("pipboy.atomicdawn.ghoul.disabled"), x, y, PipBoyTheme.dim());
            return;
        }
        var state = GhoulService.state(player);
        var ghoul = GhoulService.isGhoul(player);
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "form",
            Component.translatable(state.form().translationKey()),
            PipBoyTheme.text()
        );
        var remaining = GhoulService.cooldownRemainingMillis(player, System.currentTimeMillis());
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "cooldown",
            remaining > 0L
                ? Component.literal(GhoulTreatmentService.formatDuration(remaining))
                : Component.translatable("ghoul.atomicdawn.cooldown.ready"),
            remaining > 0L ? PipBoyTheme.warn() : PipBoyTheme.text()
        );

        if (!ghoul) {
            y += 4;
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.ghoul.human_guidance"),
                x,
                y,
                width,
                bottom,
                PipBoyTheme.dim()
            );
            return;
        }

        var maxGlow = GhoulService.maxGlow(player);
        y = gauge(
            graphics,
            screen,
            x,
            y,
            width,
            "glow",
            Math.round(state.glow()) + "/" + Math.round(maxGlow),
            GhoulService.glowFraction(player),
            PipBoyTheme.text()
        );
        var tier = GhoulService.tier(player);
        y = gauge(
            graphics,
            screen,
            x,
            y,
            width,
            "stability",
            Math.round(state.stability()) + "/" + Math.round(GhoulService.maxStability()),
            GhoulService.stabilityFraction(player),
            tier.penalized() ? PipBoyTheme.warn() : PipBoyTheme.text()
        );
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "tier",
            Component.translatable(tier.translationKey()),
            tier.penalized() ? PipBoyTheme.warn() : PipBoyTheme.text()
        );

        y += 3;
        y = effect(graphics, screen, x, y, width, "melee", GhoulService.meleeMultiplier(player) - 1F);
        y = effect(graphics, screen, x, y, width, "ranged", GhoulService.rangedAccuracyMultiplier(player) - 1F);
        y = effect(graphics, screen, x, y, width, "vats", GhoulService.vatsHitMultiplier(player) - 1F);
        y = effect(graphics, screen, x, y, width, "ap_regen", GhoulService.apRegenMultiplier(player) - 1F);
        y = effect(graphics, screen, x, y, width, "damage_taken", GhoulService.incomingDamageMultiplier(player) - 1F);
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "health_bonus",
            Component.literal("+" + Math.round(GhoulService.glowHealthBonus(player))),
            PipBoyTheme.text()
        );

        y += 3;
        var stats = SpecialStatResolver.resolve(player);
        var resist = 1F - RadiationService.resistanceMultiplier(player) * stats.radiationMultiplier();
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "conversion",
            Component.literal(percent(GhoulService.conversion(player))),
            PipBoyTheme.text()
        );
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "resistance",
            Component.literal(percent(resist)),
            PipBoyTheme.text()
        );
        y += 4;
        screen.wrapped(
            graphics,
            Component.translatable("pipboy.atomicdawn.ghoul.guidance"),
            x,
            y,
            width,
            bottom,
            PipBoyTheme.dim()
        );
    }

    private static String percent(float value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100F);
    }

    private static int pair(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        int x,
        int y,
        int width,
        String key,
        Component value,
        int color
    ) {
        screen.text(graphics, Component.translatable("pipboy.atomicdawn.ghoul." + key), x, y, PipBoyTheme.dim());
        screen.text(graphics, value, x + width - screen.font().width(value), y, color);
        return y + ROW;
    }

    private static int effect(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        int x,
        int y,
        int width,
        String key,
        float delta
    ) {
        if (Math.abs(delta) < 0.005F) {
            return y;
        }
        var value = Component.literal(String.format(Locale.ROOT, "%+.0f%%", delta * 100F));
        return pair(
            graphics,
            screen,
            x,
            y,
            width,
            "effect." + key,
            value,
            PipBoyTheme.warn()
        );
    }

    private static int gauge(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        int x,
        int y,
        int width,
        String key,
        String value,
        float fraction,
        int color
    ) {
        var label = Component.translatable("pipboy.atomicdawn.ghoul." + key);
        var text = Component.literal(value);
        var labelW = Math.max(60, screen.font().width(label) + 6);
        var valueW = screen.font().width(text);
        screen.text(graphics, label, x, y, PipBoyTheme.text());
        var barW = Math.max(16, width - labelW - valueW - 6);
        screen.bar(graphics, x + labelW, y + 2, barW, 5, fraction, color);
        screen.text(graphics, text, x + width - valueW, y, PipBoyTheme.text());
        return y + ROW + 1;
    }
}

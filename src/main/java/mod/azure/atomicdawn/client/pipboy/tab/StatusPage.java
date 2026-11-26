package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.limb.LimbService;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.special.SpecialXp;
import mod.azure.atomicdawn.vats.ActionPoints;
import mod.azure.atomicdawn.vats.BodyPart;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.instance.Weapons;

final class StatusPage {

    private StatusPage() {}

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        var stats = SpecialStatResolver.resolve(player);
        var progress = SpecialStatResolver.progress(player);
        var ap = player.getData(ADAttachments.ACTION_POINTS.get());
        var rads = player.getData(ADAttachments.RADIATION.get()).rads();
        var maxAp = VatsActions.maxActionPoints(player);
        var ghoul = GhoulService.isGhoul(player);
        var radKey = ghoul ? "glow" : "rads";

        var figureW = Math.max(170, area.width() * 55 / 100);
        StatusFigure.render(graphics, screen, area.left(figureW), player);
        graphics.fill(area.x() + figureW + 3, area.y(), area.x() + figureW + 4, area.bottom(), PipBoyTheme.faint());

        var x = area.x() + figureW + 10;
        var right = area.right() - 2;
        var bottom = area.bottom();
        var labelW = 4 + Math.max(
            Math.max(label(screen, "hp"), label(screen, radKey)),
            Math.max(Math.max(label(screen, "ap"), label(screen, "crit")), label(screen, "xp"))
        );
        var xpInto = progress.xp() - SpecialXp.totalForLevel(progress.level());
        var values = new String[] {
            Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()),
            ghoul
                ? Math.round(GhoulService.state(player).glow()) + "/" + Math.round(GhoulService.maxGlow(player))
                : Math.round(rads) + "/" + Math.round(RadiationService.maxRads()),
            Math.round(ap.points()) + "/" + Math.round(maxAp),
            ap.criticalReady()
                ? Component.translatable("pipboy.atomicdawn.status.ready").getString()
                : Math.round(ap.critical()) + "%",
            xpInto + "/" + SpecialXp.requiredXp(progress.level())
        };
        var valueW = 0;
        for (var value : values) {
            valueW = Math.max(valueW, screen.font().width(value));
        }
        var barW = Math.max(16, right - x - labelW - valueW - 4);
        var y = area.y() + 2;
        y = gauge(
            graphics,
            screen,
            x,
            y,
            labelW,
            barW,
            "hp",
            player.getHealth() / player.getMaxHealth(),
            values[0],
            PipBoyTheme.text()
        );
        y = gauge(
            graphics,
            screen,
            x,
            y,
            labelW,
            barW,
            radKey,
            ghoul ? GhoulService.glowFraction(player) : rads / RadiationService.maxRads(),
            values[1],
            ghoul ? PipBoyTheme.text() : PipBoyTheme.warn()
        );
        y = gauge(graphics, screen, x, y, labelW, barW, "ap", ap.points() / maxAp, values[2], PipBoyTheme.text());
        y = gauge(
            graphics,
            screen,
            x,
            y,
            labelW,
            barW,
            "crit",
            ap.critical() / ActionPoints.CRITICAL_FULL,
            values[3],
            PipBoyTheme.text()
        );
        y = gauge(
            graphics,
            screen,
            x,
            y,
            labelW,
            barW,
            "xp",
            SpecialXp.fraction(progress.xp(), progress.level()),
            values[4],
            PipBoyTheme.text()
        );

        y += 3;
        var maxW = right - x;
        y = line(
            graphics,
            screen,
            Component.translatable("pipboy.atomicdawn.status.level", progress.level()),
            x,
            y,
            maxW,
            bottom,
            PipBoyTheme.text()
        );
        var unspent = progress.unspentPoints(
            AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_STARTING_POINTS
        );
        if (unspent > 0) {
            y = line(
                graphics,
                screen,
                Component.translatable("pipboy.atomicdawn.status.unspent", unspent),
                x,
                y,
                maxW,
                bottom,
                PipBoyTheme.warn()
            );
        }
        if (progress.perkPicks() > 0) {
            y = line(
                graphics,
                screen,
                Component.translatable("pipboy.atomicdawn.status.picks", progress.perkPicks()),
                x,
                y,
                maxW,
                bottom,
                PipBoyTheme.warn()
            );
        }

        y += 4;
        y = line(
            graphics,
            screen,
            Component.translatable("pipboy.atomicdawn.status.resistances"),
            x,
            y,
            maxW,
            bottom,
            PipBoyTheme.text()
        );
        y = line(
            graphics,
            screen,
            Component.translatable("pipboy.atomicdawn.status.armor", player.getArmorValue()),
            x,
            y,
            maxW,
            bottom,
            PipBoyTheme.dim()
        );
        var radResist = 1F - RadiationService.resistanceMultiplier(player) * stats.radiationMultiplier();
        y = line(
            graphics,
            screen,
            Component.translatable("pipboy.atomicdawn.status.rad_resist", percent(radResist)),
            x,
            y,
            maxW,
            bottom,
            PipBoyTheme.dim()
        );

        if (AtomicDawn.CONFIG.common.combat.limbs.general.LIMBS_ENABLED && LimbService.enabled(player)) {
            y += 4;
            y = line(
                graphics,
                screen,
                Component.translatable("pipboy.atomicdawn.status.limbs"),
                x,
                y,
                maxW,
                bottom,
                PipBoyTheme.text()
            );
            var limbs = LimbService.state(player);
            var crippled = Component.empty();
            var any = false;
            for (var part : BodyPart.values()) {
                if (limbs.crippled(part)) {
                    if (any) {
                        crippled.append(", ");
                    }
                    crippled.append(Component.translatable(part.translationKey()));
                    any = true;
                }
            }
            y = line(
                graphics,
                screen,
                any
                    ? Component.translatable("pipboy.atomicdawn.status.limbs.crippled", crippled)
                    : Component.translatable(
                        limbs.healthy()
                            ? "pipboy.atomicdawn.status.limbs.healthy"
                            : "pipboy.atomicdawn.status.limbs.injured"
                    ),
                x,
                y,
                maxW,
                bottom,
                any ? PipBoyTheme.bad() : limbs.healthy() ? PipBoyTheme.dim() : PipBoyTheme.warn()
            );
        }

        var held = player.getMainHandItem();
        var definition = Weapons.definition(minecraft.level, held);
        if (definition != null) {
            y += 4;
            y = line(graphics, screen, held.getHoverName(), x, y, maxW, bottom, PipBoyTheme.text());
            if (definition.condition().enabled() && y + 9 <= bottom) {
                var condition = Weapons.condition(held, definition);
                var conditionLabel = Component.translatable("pipboy.atomicdawn.status.condition");
                screen.text(graphics, conditionLabel, x, y, PipBoyTheme.dim());
                var cx = x + screen.font().width(conditionLabel) + 4;
                screen.bar(
                    graphics,
                    cx,
                    y + 2,
                    Math.max(16, right - cx),
                    4,
                    condition.fraction(),
                    condition.fraction() < 0.25F ? PipBoyTheme.bad() : PipBoyTheme.text()
                );
                y += 11;
            }
            line(
                graphics,
                screen,
                Component.translatable(
                    "pipboy.atomicdawn.status.vats_cost",
                    Math.round(VatsActions.apCost(player, definition))
                ),
                x,
                y,
                maxW,
                bottom,
                PipBoyTheme.dim()
            );
        }
    }

    private static int label(PipBoyScreen screen, String key) {
        return screen.font().width(Component.translatable("pipboy.atomicdawn.status." + key));
    }

    private static int line(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Component text,
        int x,
        int y,
        int maxW,
        int bottom,
        int color
    ) {
        if (y + 9 > bottom) {
            return y;
        }
        screen.fitted(graphics, text, x, y, maxW, color);
        return y + 11;
    }

    private static int gauge(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        int x,
        int y,
        int labelW,
        int barW,
        String key,
        float fraction,
        String value,
        int color
    ) {
        screen.text(graphics, Component.translatable("pipboy.atomicdawn.status." + key), x, y, PipBoyTheme.text());
        screen.bar(graphics, x + labelW, y + 2, barW, 5, fraction, color);
        screen.text(graphics, Component.literal(value), x + labelW + barW + 4, y, PipBoyTheme.dim());
        return y + 12;
    }

    static String percent(float value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100F);
    }
}

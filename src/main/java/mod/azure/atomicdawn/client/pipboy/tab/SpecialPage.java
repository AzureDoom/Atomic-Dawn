package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.network.SpecialActionPayload;
import mod.azure.atomicdawn.perks.PerkCatalog;
import mod.azure.atomicdawn.perks.PerkDefinition;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialProgress;
import mod.azure.atomicdawn.special.SpecialStatResolver;

final class SpecialPage {

    private SpecialPage() {}

    private static int[] current(SpecialProgress progress) {
        var values = new int[SpecialAttribute.COUNT];
        for (var attribute : SpecialAttribute.all()) {
            values[attribute.ordinal()] = progress.base(attribute);
        }
        return values;
    }

    static int[] pending(SpecialProgress progress) {
        if (PipBoyClientState.pendingSpecial == null || PipBoyClientState.pendingRevision != progress.revision()) {
            PipBoyClientState.pendingSpecial = current(progress);
            PipBoyClientState.pendingRevision = progress.revision();
        }
        return PipBoyClientState.pendingSpecial;
    }

    private static int budget(SpecialProgress progress) {
        return AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_STARTING_POINTS * SpecialAttribute.COUNT
            + progress.earnedPoints();
    }

    private static int rowHeight(Rect area) {
        return Math.clamp((area.height() - 18) / SpecialAttribute.COUNT, 11, 18);
    }

    private static int leftWidth(Rect area) {
        return Math.max(150, area.width() * 11 / 20);
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var progress = SpecialStatResolver.progress(player);
        var pending = pending(progress);
        var current = current(progress);
        var unspent = budget(progress) - Arrays.stream(pending).sum();
        int maxBase = AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_MAX_BASE;
        var rh = rowHeight(area);
        var leftW = leftWidth(area);

        for (var attribute : SpecialAttribute.all()) {
            var i = attribute.ordinal();
            var y = area.y() + i * rh;
            var selected = attribute == PipBoyClientState.selectedAttribute;
            var label = Component.literal(attribute.letter() + "  ")
                .append(Component.translatable(attribute.translationKey()));
            screen.row(
                selected ? Component.literal("> ").append(label) : label,
                area.x(),
                y,
                92,
                rh - 1,
                selected,
                () -> {
                    PipBoyClientState.selectedAttribute = attribute;
                    screen.refresh();
                }
            );
            var value = pending[i];
            var minusTooltip = value <= current[i]
                ? Component.translatable("pipboy.atomicdawn.special.respec_hint")
                : null;
            screen.button(Component.literal("-"), area.x() + leftW - 30, y, 14, rh - 1, value > 1, minusTooltip, () -> {
                pending[i]--;
                screen.refresh();
            });
            var plusTooltip = Component.translatable("pipboy.atomicdawn.special.next_point")
                .append("\n")
                .append(baseline(attribute, value + 1));
            screen.button(
                Component.literal("+"),
                area.x() + leftW - 15,
                y,
                14,
                rh - 1,
                unspent > 0 && value < maxBase,
                plusTooltip,
                () -> {
                    pending[i]++;
                    screen.refresh();
                }
            );
        }

        var changed = !Arrays.equals(pending, current);
        var decreased = false;
        for (var i = 0; i < SpecialAttribute.COUNT; i++) {
            decreased |= pending[i] < current[i];
        }
        var footerY = area.y() + SpecialAttribute.COUNT * rh + 3;
        var confirmTooltip = decreased ? Component.translatable("pipboy.atomicdawn.special.respec_hint") : null;
        screen.button(
            Component.translatable("pipboy.atomicdawn.special.confirm"),
            area.x() + leftW - 120,
            footerY,
            60,
            13,
            changed,
            confirmTooltip,
            () -> {
                var list = new ArrayList<Integer>();
                for (var v : pending) {
                    list.add(v);
                }
                ClientPacketDistributor.sendToServer(SpecialActionPayload.allocate(progress.revision(), list));
            }
        );
        screen.button(
            Component.translatable("pipboy.atomicdawn.special.reset"),
            area.x() + leftW - 58,
            footerY,
            58,
            13,
            changed,
            () -> {
                PipBoyClientState.pendingSpecial = current;
                screen.refresh();
            }
        );
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var progress = SpecialStatResolver.progress(player);
        var stats = SpecialStatResolver.resolve(player);
        var pending = pending(progress);
        var rh = rowHeight(area);
        var leftW = leftWidth(area);
        var access = player.level().registryAccess();
        var legendary = SpecialStatResolver.legendarySpecial(access, progress, GhoulService.form(player));

        var selectedRow = PipBoyClientState.selectedAttribute.ordinal();
        graphics.fill(
            area.x(),
            area.y() + selectedRow * rh - 1,
            area.x() + leftW,
            area.y() + selectedRow * rh + rh - 1,
            PipBoyTheme.highlight()
        );

        for (var attribute : SpecialAttribute.all()) {
            var i = attribute.ordinal();
            var y = area.y() + i * rh + (rh - 8) / 2;
            var base = progress.base(attribute);
            var valueX = area.x() + 96;
            var value = Component.literal(Integer.toString(pending[i]));
            if (pending[i] != base) {
                value = Component.literal(base + "→" + pending[i]);
            }
            screen.text(graphics, value, valueX, y, pending[i] != base ? PipBoyTheme.warn() : PipBoyTheme.text());
            var temp = stats.temporary(attribute);
            if (temp != 0) {
                screen.text(
                    graphics,
                    Component.literal(String.format(Locale.ROOT, "(%+d)", temp)),
                    valueX + 30,
                    y,
                    temp > 0 ? PipBoyTheme.text() : PipBoyTheme.bad()
                );
            }
            var used = SpecialStatResolver.capacityUsed(access, progress, progress.activeLoadout(), attribute);
            var cap = Math.max(
                pending[i],
                Math.min(
                    AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_MAX_BASE,
                    pending[i] + legendary[i]
                )
            );
            var capacity = Component.translatable("pipboy.atomicdawn.special.capacity", used, cap);
            var capX = area.x() + leftW - 34 - screen.font().width(capacity);
            screen.text(graphics, capacity, capX, y, used > cap ? PipBoyTheme.bad() : PipBoyTheme.dim());
        }

        var unspent = budget(progress) - Arrays.stream(pending).sum();
        var footerY = area.y() + SpecialAttribute.COUNT * rh + 6;
        screen.text(
            graphics,
            Component.translatable("pipboy.atomicdawn.special.unspent", unspent),
            area.x() + 2,
            footerY,
            unspent > 0 ? PipBoyTheme.warn() : PipBoyTheme.dim()
        );

        renderDetail(graphics, screen, area.rightOf(leftW, 8), player, progress, pending);
    }

    private static void renderDetail(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect area,
        LocalPlayer player,
        SpecialProgress progress,
        int[] pending
    ) {
        graphics.fill(area.x() - 4, area.y(), area.x() - 3, area.bottom(), PipBoyTheme.faint());
        var attribute = PipBoyClientState.selectedAttribute;
        var x = area.x();
        var y = area.y() + 2;
        screen.text(
            graphics,
            Component.translatable(attribute.translationKey()).withStyle(s -> s.withBold(true)),
            x,
            y,
            PipBoyTheme.text()
        );
        y += 12;
        y = screen.wrapped(
            graphics,
            Component.translatable(attribute.descriptionKey()),
            x,
            y,
            area.width() - 2,
            area.bottom(),
            PipBoyTheme.dim()
        );
        y += 4;
        screen.text(graphics, Component.translatable("pipboy.atomicdawn.special.current"), x, y, PipBoyTheme.text());
        y += 10;
        y = screen.wrapped(
            graphics,
            baseline(attribute, pending[attribute.ordinal()]),
            x + 4,
            y,
            area.width() - 6,
            area.bottom(),
            PipBoyTheme.text()
        );
        y += 4;
        screen.text(graphics, Component.translatable("pipboy.atomicdawn.special.cards"), x, y, PipBoyTheme.text());
        y += 10;
        var cards = PerkCatalog.byCategory(player.level().registryAccess(), attribute);
        for (var card : cards) {
            if (y + 9 > area.bottom()) {
                break;
            }
            var id = card.key().identifier();
            var owned = progress.ownedRank(id);
            var equipped = progress.equippedRank(id);
            MutableComponent line = Component.literal(equipped > 0 ? "[E] " : owned > 0 ? "[O] " : "[ ] ")
                .append(Component.translatable(PerkDefinition.translationKey(id)));
            if (owned > 0) {
                line.append(" " + owned + "/" + card.value().maxRank());
            }
            screen.fitted(
                graphics,
                line,
                x + 4,
                y,
                area.width() - 6,
                owned > 0 ? PipBoyTheme.text() : PipBoyTheme.dim()
            );
            y += 10;
        }
    }

    static Component baseline(SpecialAttribute attribute, int value) {
        var above = Math.max(
            0,
            value - AtomicDawn.CONFIG.common.progression.special.attributes.SPECIAL_STARTING_POINTS
        );
        var key = "pipboy.atomicdawn.special.effect." + attribute.getSerializedName();
        return switch (attribute) {
            case STRENGTH -> Component.translatable(
                key,
                pct(
                    above
                        * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_STRENGTH_MELEE_PER_POINT
                )
            );
            case PERCEPTION -> Component.translatable(
                key,
                pct(
                    above
                        * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_PERCEPTION_HIT_CHANCE_PER_POINT
                )
            );
            case ENDURANCE -> Component.translatable(
                key,
                num(
                    above
                        * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_ENDURANCE_HEALTH_PER_POINT
                )
            );
            case CHARISMA -> Component.translatable(key, value);
            case INTELLIGENCE -> Component.translatable(
                key,
                pct(
                    above
                        * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_INTELLIGENCE_CONDITION_PER_POINT
                )
            );
            case AGILITY -> Component.translatable(
                key,
                num(above * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_AGILITY_AP_PER_POINT)
            );
            case LUCK -> Component.translatable(
                key,
                pct(
                    above
                        * AtomicDawn.CONFIG.common.progression.special.attributeBonuses.SPECIAL_LUCK_CRIT_GAIN_PER_POINT
                )
            );
        };
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0).replace(".0%", "%");
    }

    private static String num(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }
}

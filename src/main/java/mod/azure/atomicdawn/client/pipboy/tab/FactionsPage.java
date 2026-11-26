package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.story.CompanionProfile;
import mod.azure.atomicdawn.story.StoryService;

final class FactionsPage {

    private static final int ROW_H = 24;

    private static final int HEADER_H = 20;

    private static int scroll;

    private FactionsPage() {}

    private record Row(
        Component name,
        Component rank,
        int color,
        int value,
        int min,
        int max,
        boolean header
    ) {}

    static void init(PipBoyScreen screen, Rect area) {
        scroll = 0;
    }

    private static List<Row> rows() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var rows = new ArrayList<Row>();
        if (player == null) {
            return rows;
        }
        var access = player.level().registryAccess();
        var state = StoryService.state(player);
        var factions = new ArrayList<Row>();
        for (var holder : StoryService.factions(access)) {
            var faction = holder.value();
            var id = holder.key().identifier();
            var value = StoryService.reputation(player, id);
            if (faction.hidden() && !state.reputation().containsKey(id)) {
                continue;
            }
            var rank = faction.rank(value);
            factions.add(
                new Row(
                    Component.translatableWithFallback(faction.name(), faction.name()),
                    Component.translatableWithFallback(rank.name(), rank.name()),
                    0xFF000000 | faction.color(),
                    value,
                    faction.min(),
                    faction.max(),
                    false
                )
            );
        }
        if (!factions.isEmpty()) {
            rows.add(header("pipboy.atomicdawn.factions.factions"));
            rows.addAll(factions);
        }
        var companions = new ArrayList<Row>();
        ADRegistryUtil.optionalRegistry(access, ADRegistries.COMPANION)
            .ifPresent(registry -> registry.listElements().forEach(holder -> {
                var profile = holder.value();
                Identifier npc = profile.npc();
                if (!state.affinity().containsKey(npc)) {
                    return;
                }
                var value = state.affinity().get(npc);
                companions.add(
                    new Row(
                        Component.translatableWithFallback(profile.name(), profile.name()),
                        Component.translatable(CompanionProfile.tier(value)),
                        PipBoyTheme.text(),
                        value,
                        CompanionProfile.MIN,
                        CompanionProfile.MAX,
                        false
                    )
                );
            }));
        if (!companions.isEmpty()) {
            rows.add(header("pipboy.atomicdawn.factions.companions"));
            rows.addAll(companions);
        }
        return rows;
    }

    private static Row header(String key) {
        return new Row(Component.translatable(key), Component.empty(), 0, 0, 0, 0, true);
    }

    private static int height(Row row) {
        return row.header() ? HEADER_H : ROW_H;
    }

    private static int maxScroll(List<Row> rows, int available) {
        var used = 0;
        for (var i = rows.size() - 1; i >= 0; i--) {
            used += height(rows.get(i));
            if (used > available) {
                return i + 1;
            }
        }
        return 0;
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var rows = rows();
        if (rows.isEmpty()) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.factions.empty"),
                area.x() + 4,
                area.y() + 4,
                PipBoyTheme.dim()
            );
            return;
        }
        var available = area.height() - 4;
        var max = maxScroll(rows, available);
        scroll = Math.clamp(scroll, 0, max);
        var y = area.y() + 2;
        var font = screen.font();
        var x = area.x() + 4;
        var w = area.width() - (max > 0 ? 14 : 8);
        for (var i = scroll; i < rows.size(); i++) {
            var row = rows.get(i);
            if (y + height(row) > area.bottom()) {
                break;
            }
            if (row.header()) {
                screen.text(graphics, row.name(), x, y + 4, PipBoyTheme.text());
                graphics.fill(x, y + 14, x + w, y + 15, PipBoyTheme.border());
                y += HEADER_H;
                continue;
            }
            screen.text(graphics, row.name(), x, y, row.color());
            var standing = row.rank().copy().append(" (" + row.value() + ")");
            screen.text(graphics, standing, x + w - font.width(standing), y, PipBoyTheme.text());
            var barY = y + 12;
            graphics.fill(x, barY, x + w, barY + 5, PipBoyTheme.faint());
            var span = Math.max(1, row.max() - row.min());
            var zero = Math.clamp(x + (int) ((long) (0 - row.min()) * w / span), x, x + w);
            var marker = Math.clamp(x + (int) ((long) (row.value() - row.min()) * w / span), x, x + w);
            var color = row.value() >= 0 ? PipBoyTheme.text() : PipBoyTheme.bad();
            graphics.fill(Math.min(zero, marker), barY, Math.max(zero, marker) + 1, barY + 5, color);
            graphics.fill(zero, barY - 1, zero + 1, barY + 6, PipBoyTheme.dim());
            y += ROW_H;
        }
        if (max > 0) {
            var trackX = area.right() - 6;
            var trackTop = area.y() + 2;
            var trackH = area.height() - 4;
            graphics.fill(trackX, trackTop, trackX + 2, trackTop + trackH, PipBoyTheme.faint());
            var thumbH = Math.max(10, trackH * (rows.size() - max) / rows.size());
            var thumbY = trackTop + (trackH - thumbH) * scroll / max;
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, PipBoyTheme.text());
        }
    }

    static boolean scroll(int direction, Rect area) {
        var before = scroll;
        scroll = Math.max(0, scroll + direction);
        return before != scroll;
    }
}

package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.bestiary.BestiaryEntry;
import mod.azure.atomicdawn.registry.ADAttachments;

final class BestiaryPage {

    private static final String SCROLL = "data.bestiary";

    @Nullable
    private static Identifier selected;

    private BestiaryPage() {}

    private static List<Holder.Reference<BestiaryEntry>> entries(LocalPlayer player) {
        return BestiaryEntry.all(player.level().registryAccess())
            .stream()
            .filter(holder -> BuiltInRegistries.ENTITY_TYPE.containsKey(holder.value().entity()))
            .toList();
    }

    private static Component name(Identifier entity) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entity)
            .<Component>map(EntityType::getDescription)
            .orElseGet(() -> Component.literal(PipBoyNames.pretty(entity.getPath())));
    }

    static boolean scroll(int direction, Rect area) {
        var player = Minecraft.getInstance().player;
        return player != null && PageList.scroll(
            SCROLL,
            direction,
            entries(player).size(),
            PageList.rows(PageList.list(area, false))
        );
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var log = player.getData(ADAttachments.BESTIARY.get());
        var list = PageList.list(area, false);
        var entries = entries(player);
        var rows = PageList.rows(list);
        var first = PageList.first(SCROLL, entries.size(), rows);
        for (var r = 0; r < rows && first + r < entries.size(); r++) {
            var holder = entries.get(first + r);
            var id = holder.key().identifier();
            var entity = holder.value().entity();
            MutableComponent label;
            if (!log.encountered(entity)) {
                label = Component.literal("  ???");
            } else {
                var studied = log.killCount(entity) >= holder.value().studyKills();
                label = Component.literal(studied ? "[S] " : "    ").append(name(entity));
            }
            screen.row(
                label,
                list.x(),
                list.y() + r * PageList.ROW,
                list.width(),
                PageList.ROW - 1,
                id.equals(selected),
                () -> {
                    selected = id;
                    screen.refresh();
                }
            );
        }
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var log = player.getData(ADAttachments.BESTIARY.get());
        var list = PageList.list(area, false);
        var detail = PageList.detail(area);
        var entries = entries(player);
        PageList.decorate(graphics, list, detail, SCROLL, entries.size());
        var known = entries.stream().filter(holder -> log.encountered(holder.value().entity())).count();
        var header = Component.translatable("pipboy.atomicdawn.bestiary.progress", known, entries.size());
        Holder.Reference<BestiaryEntry> holder = null;
        for (var candidate : entries) {
            if (candidate.key().identifier().equals(selected)) {
                holder = candidate;
            }
        }
        var x = detail.x();
        var y = detail.y() + 1;
        if (holder == null) {
            screen.text(graphics, header, x, y, PipBoyTheme.dim());
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.bestiary.select"),
                x,
                y + 14,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var entry = holder.value();
        var entity = entry.entity();
        if (!log.encountered(entity)) {
            screen.text(graphics, Component.literal("???"), x, y, PipBoyTheme.text());
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.bestiary.unknown"),
                x,
                y + 14,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var kills = log.killCount(entity);
        var studied = kills >= entry.studyKills();
        var icon = icon(entry);
        if (!icon.isEmpty()) {
            graphics.item(icon, x, y);
        }
        screen.fitted(graphics, name(entity), x + 20, y + 4, detail.width() - 20, PipBoyTheme.text());
        y += 20;
        screen.text(
            graphics,
            Component.translatable(
                "pipboy.atomicdawn.bestiary.category",
                PipBoyNames.fromKey("bestiary.category." + entry.category())
            ),
            x,
            y,
            PipBoyTheme.dim()
        );
        y += 11;
        screen.text(
            graphics,
            Component.translatable(
                "pipboy.atomicdawn.bestiary.threat",
                "☢".repeat(entry.threat()) + "·".repeat(5 - entry.threat())
            ),
            x,
            y,
            PipBoyTheme.warn()
        );
        y += 11;
        screen.text(
            graphics,
            Component.translatable("pipboy.atomicdawn.bestiary.kills", kills),
            x,
            y,
            PipBoyTheme.dim()
        );
        y += 13;
        if (!studied) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.bestiary.study", entry.studyKills() - kills),
                x,
                y,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.warn()
            );
            return;
        }
        y = screen.wrapped(
            graphics,
            Component.translatableWithFallback(PipBoyNames.key("bestiary", holder.key().identifier()), ""),
            x,
            y,
            detail.width(),
            detail.bottom(),
            PipBoyTheme.dim()
        ) + 3;
        y = traits(graphics, screen, "weaknesses", entry.weaknesses(), x, y, detail);
        traits(graphics, screen, "resistances", entry.resistances(), x, y, detail);
    }

    private static int traits(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        String key,
        List<String> values,
        int x,
        int y,
        Rect detail
    ) {
        if (values.isEmpty() || y + 9 > detail.bottom()) {
            return y;
        }
        var line = Component.empty();
        for (var i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(", ");
            }
            line.append(PipBoyNames.fromKey("bestiary.trait." + values.get(i)));
        }
        screen.text(graphics, Component.translatable("pipboy.atomicdawn.bestiary." + key), x, y, PipBoyTheme.text());
        return screen.wrapped(graphics, line, x + 4, y + 10, detail.width() - 4, detail.bottom(), PipBoyTheme.dim())
            + 2;
    }

    private static ItemStack icon(BestiaryEntry entry) {
        var id = entry.icon()
            .orElseGet(
                () -> ADRegistryUtil.id(
                    entry.entity().getNamespace(),
                    entry.entity().getPath() + "_spawn_egg"
                )
            );
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}

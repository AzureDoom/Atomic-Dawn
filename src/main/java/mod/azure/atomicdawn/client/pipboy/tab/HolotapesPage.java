package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.client.pipboy.radio.HolotapePlayer;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.holotape.Holotape;
import mod.azure.atomicdawn.registry.ADAttachments;

final class HolotapesPage {

    private static final String SCROLL = "data.holotapes";

    @Nullable
    private static Identifier selected;

    private HolotapesPage() {}

    private static List<Holder.Reference<Holotape>> collected(LocalPlayer player) {
        var log = player.getData(ADAttachments.HOLOTAPES.get());
        return Holotape.all(player.level().registryAccess())
            .stream()
            .filter(holder -> log.has(holder.key().identifier()))
            .toList();
    }

    static boolean scroll(int direction, Rect area) {
        var player = Minecraft.getInstance().player;
        return player != null && PageList.scroll(
            SCROLL,
            direction,
            collected(player).size(),
            PageList.rows(PageList.list(area, false))
        );
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, false);
        var tapes = collected(player);
        var rows = PageList.rows(list);
        var first = PageList.first(SCROLL, tapes.size(), rows);
        var playing = HolotapePlayer.current();
        for (var r = 0; r < rows && first + r < tapes.size(); r++) {
            var id = tapes.get(first + r).key().identifier();
            var label = Component.literal(id.equals(playing) ? "> " : "  ").append(PipBoyNames.holotape(id));
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
        if (selected == null || tapes.stream().noneMatch(holder -> holder.key().identifier().equals(selected))) {
            return;
        }
        var detail = PageList.detail(area);
        var id = selected;
        var isPlaying = id.equals(playing);
        var play = Component.translatable(
            isPlaying ? "pipboy.atomicdawn.holotapes.stop" : "pipboy.atomicdawn.holotapes.play"
        );
        screen.button(play, detail.x(), detail.bottom() - 13, screen.font().width(play) + 10, 13, true, () -> {
            if (isPlaying) {
                HolotapePlayer.stop();
            } else {
                HolotapePlayer.play(id);
            }
            screen.refresh();
        });
    }

    static void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, false);
        var detail = PageList.detail(area);
        var tapes = collected(player);
        PageList.decorate(graphics, list, detail, SCROLL, tapes.size());
        if (tapes.isEmpty()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.data.holotapes.empty"),
                list.x() + 2,
                list.y() + 3,
                list.width() - 4,
                list.bottom(),
                PipBoyTheme.dim()
            );
        }
        var holotape = selected == null ? null : Holotape.get(player.level().registryAccess(), selected).orElse(null);
        if (holotape == null || tapes.stream().noneMatch(holder -> holder.key().identifier().equals(selected))) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.holotapes.select"),
                detail.x(),
                detail.y() + 2,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var x = detail.x();
        var y = detail.y() + 1;
        screen.fitted(graphics, PipBoyNames.holotape(selected), x, y, detail.width(), PipBoyTheme.text());
        y += 12;
        if (holotape.author().isPresent()) {
            screen.text(
                graphics,
                Component.translatable(
                    "pipboy.atomicdawn.holotapes.author",
                    PipBoyNames.fromKey(holotape.author().get())
                ),
                x,
                y,
                PipBoyTheme.dim()
            );
            y += 11;
        }
        if (holotape.lengthSeconds() > 0) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.holotapes.length", time(holotape.lengthSeconds())),
                x,
                y,
                PipBoyTheme.dim()
            );
            y += 11;
        }
        y += 2;
        var bottom = detail.bottom() - (selected.equals(HolotapePlayer.current()) ? 54 : 22);
        screen.wrapped(
            graphics,
            Component.translatableWithFallback(PipBoyNames.key("holotape", selected) + ".desc", ""),
            x,
            y,
            detail.width(),
            bottom,
            PipBoyTheme.dim()
        );
        if (selected.equals(HolotapePlayer.current())) {
            var played = HolotapePlayer.playedTicks() / 20;
            var length = holotape.lengthSeconds();
            var barY = detail.bottom() - 46;
            screen.bar(
                graphics,
                x,
                barY,
                detail.width() - 2,
                4,
                length > 0 ? played / (float) length : 0F,
                PipBoyTheme.text()
            );
            var label = HolotapePlayer.isPlaying()
                ? time(played) + (length > 0 ? " / " + time(length) : "")
                : Component.translatable("pipboy.atomicdawn.radio.tuning").getString();
            screen.text(graphics, Component.literal(label), x, barY + 7, PipBoyTheme.dim());
        }
    }

    private static String time(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}

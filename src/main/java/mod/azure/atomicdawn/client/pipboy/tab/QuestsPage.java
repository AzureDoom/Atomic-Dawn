package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.network.PipBoyActionPayload;
import mod.azure.atomicdawn.network.SpecialActionPayload;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.quest.QuestDefinition;
import mod.azure.atomicdawn.quest.QuestLog;
import mod.azure.atomicdawn.quest.QuestProgress;
import mod.azure.atomicdawn.quest.QuestReward;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.story.StoryService;

final class QuestsPage {

    private static final String SCROLL = "data.quests";

    private static boolean showCompleted;

    @Nullable
    private static Identifier selected;

    private QuestsPage() {}

    private static QuestLog log(LocalPlayer player) {
        return player.getData(ADAttachments.QUESTS.get());
    }

    private static List<Holder.Reference<QuestDefinition>> visible(LocalPlayer player) {
        var log = log(player);
        return QuestDefinition.all(player.level().registryAccess())
            .stream()
            .filter(holder -> log.get(holder.key().identifier()).map(p -> p.completed() == showCompleted).orElse(false))
            .toList();
    }

    static boolean scroll(int direction, Rect area) {
        var player = Minecraft.getInstance().player;
        return player != null && PageList.scroll(
            SCROLL,
            direction,
            visible(player).size(),
            PageList.rows(PageList.list(area, true))
        );
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var active = Component.translatable("pipboy.atomicdawn.quests.active");
        var activeW = screen.font().width(active) + 10;
        screen.option(active, area.x(), area.y(), activeW, 12, !showCompleted, () -> {
            showCompleted = false;
            screen.refresh();
        });
        var done = Component.translatable("pipboy.atomicdawn.quests.completed");
        screen.option(
            done,
            area.x() + activeW + 2,
            area.y(),
            screen.font().width(done) + 10,
            12,
            showCompleted,
            () -> {
                showCompleted = true;
                screen.refresh();
            }
        );

        var list = PageList.list(area, true);
        var quests = visible(player);
        var rows = PageList.rows(list);
        var first = PageList.first(SCROLL, quests.size(), rows);
        var tracked = log(player).tracked().orElse(null);
        for (var r = 0; r < rows && first + r < quests.size(); r++) {
            var holder = quests.get(first + r);
            var id = holder.key().identifier();
            var marker = id.equals(tracked) ? "* " : "  ";
            var label = Component.literal(marker + "[" + holder.value().category().getSerializedName().charAt(0) + "] ")
                .append(PipBoyNames.quest(id));
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

        var detail = PageList.detail(area.below(PageList.TOOLBAR));
        var progress = selected == null ? null : log(player).get(selected).orElse(null);
        if (progress != null && !progress.completed()) {
            var isTracked = selected.equals(tracked);
            var label = Component.translatable(
                isTracked ? "pipboy.atomicdawn.quests.untrack" : "pipboy.atomicdawn.quests.track"
            );
            var target = isTracked ? SpecialActionPayload.NONE : selected;
            screen.button(
                label,
                detail.x(),
                detail.bottom() - 13,
                screen.font().width(label) + 10,
                13,
                true,
                () -> ClientPacketDistributor.sendToServer(
                    new PipBoyActionPayload(PipBoyActionPayload.Action.TRACK_QUEST, target)
                )
            );
        }
    }

    static void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, true);
        var detail = PageList.detail(area.below(PageList.TOOLBAR));
        var quests = visible(player);
        PageList.decorate(graphics, list, detail, SCROLL, quests.size());
        if (quests.isEmpty()) {
            screen.text(
                graphics,
                Component.translatable(
                    showCompleted ? "pipboy.atomicdawn.quests.none_completed" : "pipboy.atomicdawn.quests.none"
                ),
                list.x() + 2,
                list.y() + 3,
                PipBoyTheme.dim()
            );
        }
        if (selected == null) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.quests.select"),
                detail.x(),
                detail.y() + 2,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var access = player.level().registryAccess();
        var definition = QuestDefinition.get(access, selected).orElse(null);
        var progress = log(player).get(selected).orElse(null);
        if (definition == null || progress == null) {
            return;
        }
        var x = detail.x();
        var y = detail.y() + 1;
        var bottom = detail.bottom() - (progress.completed() ? 0 : 15);
        screen.fitted(graphics, PipBoyNames.quest(selected), x, y, detail.width(), PipBoyTheme.text());
        y += 11;
        screen.text(graphics, Component.translatable(definition.category().translationKey()), x, y, PipBoyTheme.dim());
        y += 12;
        y = screen.wrapped(
            graphics,
            Component.translatableWithFallback(PipBoyNames.key("quest", selected) + ".desc", ""),
            x,
            y,
            detail.width(),
            bottom,
            PipBoyTheme.dim()
        ) + 3;
        y = objectives(graphics, screen, definition, progress, x, y, detail.width(), bottom) + 3;
        var outcomeName = player == null ? null : StoryService.outcome(player, selected).orElse(null);
        var outcome = outcomeName == null ? null : definition.outcomes().get(outcomeName);
        if (progress.completed() && outcome != null && !outcome.description().isEmpty()) {
            y = screen.wrapped(
                graphics,
                Component.translatable(
                    "pipboy.atomicdawn.quest.outcome",
                    Component.translatableWithFallback(outcome.description(), outcome.description())
                ),
                x,
                y,
                detail.width(),
                bottom,
                PipBoyTheme.text()
            ) + 3;
        }
        rewards(graphics, screen, definition.rewards(), x, y, detail.width(), bottom);
    }

    private static int objectives(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        QuestDefinition definition,
        QuestProgress progress,
        int x,
        int y,
        int width,
        int bottom
    ) {
        var objectives = definition.objectives();
        for (var i = 0; i < objectives.size() && y + 9 <= bottom; i++) {
            var objective = objectives.get(i);
            var count = progress.count(i);
            var complete = count >= objective.count();
            var hidden = definition.sequential() && i > progress.stage() && !progress.completed();
            if (hidden) {
                screen.text(graphics, Component.literal("[ ] ???"), x, y, PipBoyTheme.faint());
                y += 10;
                continue;
            }
            MutableComponent line = Component.literal(complete ? "[x] " : "[ ] ").append(objective.describe());
            if (objective.count() > 1 && !complete) {
                line.append(" (" + count + "/" + objective.count() + ")");
            }
            var color = complete ? PipBoyTheme.dim() : PipBoyTheme.text();
            for (var part : screen.font().split(line, width)) {
                if (y + 9 > bottom) {
                    break;
                }
                graphics.text(screen.font(), part, x, y, color, false);
                y += 10;
            }
        }
        return y;
    }

    private static void rewards(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        QuestReward reward,
        int x,
        int y,
        int width,
        int bottom
    ) {
        var parts = Component.empty();
        var any = false;
        if (reward.xp() > 0) {
            parts.append(Component.translatable("pipboy.atomicdawn.quests.reward.xp", reward.xp()));
            any = true;
        }
        if (reward.perkPicks() > 0) {
            parts.append(any ? ", " : "")
                .append(Component.translatable("pipboy.atomicdawn.quests.reward.picks", reward.perkPicks()));
            any = true;
        }
        for (var item : reward.items()) {
            var value = BuiltInRegistries.ITEM.getOptional(item.item()).orElse(null);
            if (value == null) {
                continue;
            }
            parts.append(any ? ", " : "").append(item.count() + "x ").append(new ItemStack(value).getHoverName());
            any = true;
        }
        for (var holotape : reward.holotapes()) {
            parts.append(any ? ", " : "")
                .append(Component.translatable("item.atomicdawn.holotape.named", PipBoyNames.holotape(holotape)));
            any = true;
        }
        if (!any || y + 9 > bottom) {
            return;
        }
        screen.text(graphics, Component.translatable("pipboy.atomicdawn.quests.rewards"), x, y, PipBoyTheme.text());
        screen.wrapped(graphics, parts, x + 4, y + 10, width - 4, bottom, PipBoyTheme.dim());
    }
}

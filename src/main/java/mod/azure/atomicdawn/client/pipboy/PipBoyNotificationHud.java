package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.PipBoyNotifications;
import mod.azure.atomicdawn.quest.QuestDefinition;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADSounds;

public final class PipBoyNotificationHud implements GuiLayer {

    private static final int MAX_VISIBLE = 4;

    private static final class Entry {

        final PipBoyNotifications.Kind kind;

        final String detail;

        int value;

        int repeats = 1;

        long expires;

        Entry(PipBoyNotifications.Kind kind, String detail, int value, long expires) {
            this.kind = kind;
            this.detail = detail;
            this.value = value;
            this.expires = expires;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    public static void push(PipBoyNotifications.Kind kind, int value) {
        push(kind, value, "");
    }

    public static void push(PipBoyNotifications.Kind kind, int value, String detail) {
        var duration = AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_NOTIFICATION_TICKS;
        if (duration <= 0) {
            return;
        }
        var expires = now() + duration;
        for (var entry : ENTRIES) {
            if (entry.kind == kind && entry.detail.equals(detail)) {
                entry.value = value;
                entry.repeats++;
                entry.expires = expires;
                return;
            }
        }
        ENTRIES.add(new Entry(kind, detail, value, expires));
        var player = Minecraft.getInstance().player;
        if (player != null && AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_SOUNDS) {
            player.playSound(ADSounds.PIPBOY_NOTIFICATION.get(), 1F, 1F);
        }
    }

    private static int renderTracked(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gui.screen() != null) {
            return 4;
        }
        var log = player.getData(ADAttachments.QUESTS.get());
        var id = log.tracked().orElse(null);
        if (id == null) {
            return 4;
        }
        var progress = log.get(id).orElse(null);
        var definition = QuestDefinition.get(minecraft.level.registryAccess(), id).orElse(null);
        if (progress == null || progress.completed() || definition == null) {
            return 4;
        }
        var font = minecraft.font;
        var title = PipBoyNames.quest(id);
        Component objectiveLine = null;
        var objectives = definition.objectives();
        for (var i = 0; i < objectives.size(); i++) {
            var objective = objectives.get(i);
            if (progress.count(i) >= objective.count() || (definition.sequential() && i != progress.stage())) {
                continue;
            }
            var line = Component.literal("› ").append(objective.describe());
            if (objective.count() > 1) {
                line.append(" (" + progress.count(i) + "/" + objective.count() + ")");
            }
            objectiveLine = line;
            break;
        }
        var width = Math.max(font.width(title), objectiveLine == null ? 0 : font.width(objectiveLine)) + 10;
        var height = objectiveLine == null ? 14 : 25;
        if (HudLayout.push(graphics, HudElement.QUEST_TRACKER, 4, 4, width, height)) {
            graphics.fill(4, 4, 4 + width, 4 + height, PipBoyTheme.background());
            graphics.fill(4, 4, 6, 4 + height, PipBoyTheme.warn());
            graphics.text(font, title, 9, 7, PipBoyTheme.text(), false);
            if (objectiveLine != null) {
                graphics.text(font, objectiveLine, 9, 18, PipBoyTheme.dim(), false);
            }
            HudLayout.pop(graphics);
        }
        return HudLayout.customized(HudElement.QUEST_TRACKER) ? 4 : 4 + height + 3;
    }

    public static void clear() {
        ENTRIES.clear();
    }

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden) {
            return;
        }
        var now = now();
        ENTRIES.removeIf(entry -> entry.expires <= now);
        var font = minecraft.font;
        var y = renderTracked(graphics, minecraft);
        var texts = new ArrayList<Component>();
        var maxWidth = 0;
        for (var entry : ENTRIES) {
            if (texts.size() >= MAX_VISIBLE) {
                break;
            }
            var text = Component.translatable(
                entry.kind.translationKey(),
                entry.value,
                PipBoyNames.fromKey(entry.detail)
            );
            if (entry.repeats > 1 && entry.kind == PipBoyNotifications.Kind.CARDS_UNEQUIPPED) {
                text.append(" ×" + entry.repeats);
            }
            texts.add(text);
            maxWidth = Math.max(maxWidth, font.width(text) + 10);
        }
        if (texts.isEmpty()) {
            return;
        }
        if (!HudLayout.push(graphics, HudElement.NOTIFICATIONS, 4, y, maxWidth, texts.size() * 16 - 2)) {
            return;
        }
        for (var text : texts) {
            var w = font.width(text) + 10;
            graphics.fill(4, y, 4 + w, y + 14, PipBoyTheme.background());
            graphics.fill(4, y, 6, y + 14, PipBoyTheme.text());
            graphics.text(font, text, 9, y + 3, PipBoyTheme.text(), false);
            y += 16;
        }
        HudLayout.pop(graphics);
    }
}

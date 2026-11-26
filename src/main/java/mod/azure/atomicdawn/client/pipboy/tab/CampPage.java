package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mod.azure.atomicdawn.camp.CampScrapbox;
import mod.azure.atomicdawn.client.camp.CampClientState;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.network.CampActionPayload;
import mod.azure.atomicdawn.network.CampSyncPayload;

final class CampPage {

    private static final int ROW = 11;

    private static final int SLOT_ROW = 14;

    private static final int BUTTON_H = 13;

    private static final int SMALL_H = 11;

    private static final long CONFIRM_TICKS = 100L;

    private static final UUID NONE = new UUID(0L, 0L);

    private static int revision = Integer.MIN_VALUE;

    private static String confirmKey = "";

    private static long confirmUntil = -1L;

    private CampPage() {}

    private static boolean confirming(PipBoyScreen screen, String key) {
        return confirmKey.equals(key) && confirmUntil >= screen.ticks();
    }

    private static void confirmOr(PipBoyScreen screen, String key, Runnable action) {
        if (confirming(screen, key)) {
            confirmKey = "";
            confirmUntil = -1L;
            action.run();
            return;
        }
        confirmKey = key;
        confirmUntil = screen.ticks() + CONFIRM_TICKS;
        screen.refresh();
    }

    private static int slotsTop(Rect area) {
        return area.y() + 4 + ROW + 4;
    }

    private static Optional<CampSyncPayload.SlotView> slot(CampSyncPayload state, int index) {
        return state.slots().stream().filter(slot -> slot.slotIndex() == index).findFirst();
    }

    private static Component deleteLabel(PipBoyScreen screen, CampSyncPayload.SlotView view) {
        var confirm = confirming(screen, "delete:" + view.campId());
        return Component.translatable(confirm ? "pipboy.atomicdawn.camp.confirm" : "pipboy.atomicdawn.camp.delete");
    }

    private static boolean deletable(Optional<CampSyncPayload.SlotView> view) {
        return view.isPresent() && !view.get().deployed();
    }

    private static boolean selectable(CampSyncPayload state, int index, Optional<CampSyncPayload.SlotView> view) {
        return state.selectedSlot() != index && !(view.isPresent() && view.get().deployed());
    }

    private static int reserved(PipBoyScreen screen, CampSyncPayload state, int index) {
        var view = slot(state, index);
        var total = 0;
        if (deletable(view)) {
            total += screen.font().width(deleteLabel(screen, view.get())) + 8 + 3;
        }
        if (selectable(state, index, view)) {
            total += screen.font().width(Component.translatable("pipboy.atomicdawn.camp.select")) + 8 + 3;
        }
        return total;
    }

    private record Action(
        Component label,
        boolean active,
        Runnable run
    ) {}

    private static List<Action> actions(PipBoyScreen screen, CampSyncPayload state) {
        var actions = new ArrayList<Action>();
        var camp = state.camp();
        if (camp.isEmpty()) {
            return actions;
        }
        var view = camp.get();
        actions.add(
            new Action(
                Component.translatable("pipboy.atomicdawn.camp.pin"),
                sameDimension(view),
                () -> CampClientState.send(CampActionPayload.Action.PIN, view.campId(), 0)
            )
        );
        var storeKey = "store:" + view.campId();
        actions.add(
            new Action(
                Component.translatable(
                    confirming(screen, storeKey) ? "pipboy.atomicdawn.camp.confirm" : "pipboy.atomicdawn.camp.store"
                ),
                true,
                () -> confirmOr(
                    screen,
                    storeKey,
                    () -> CampClientState.send(CampActionPayload.Action.STORE, view.campId(), 0)
                )
            )
        );
        var stored = slot(state, view.slotIndex()).map(CampSyncPayload.SlotView::stored).orElse(0);
        if (stored > 0) {
            actions.add(
                new Action(
                    Component.translatable("pipboy.atomicdawn.camp.rebuild"),
                    true,
                    () -> CampClientState.send(CampActionPayload.Action.REBUILD, view.campId(), 0)
                )
            );
            var partialKey = "partial:" + view.campId();
            actions.add(
                new Action(
                    Component.translatable(
                        confirming(screen, partialKey)
                            ? "pipboy.atomicdawn.camp.confirm"
                            : "pipboy.atomicdawn.camp.rebuild_partial"
                    ),
                    true,
                    () -> confirmOr(
                        screen,
                        partialKey,
                        () -> CampClientState.send(CampActionPayload.Action.REBUILD, view.campId(), 1)
                    )
                )
            );
        }
        return actions;
    }

    private static int actionRows(PipBoyScreen screen, CampSyncPayload state, Rect area) {
        var rows = 1;
        var x = 0;
        var limit = area.width() - 8;
        for (var action : actions(screen, state)) {
            var w = screen.font().width(action.label()) + 10;
            if (x > 0 && x + w > limit) {
                rows++;
                x = 0;
            }
            x += w + 4;
        }
        return state.camp().isPresent() ? rows : 0;
    }

    static void init(PipBoyScreen screen, Rect area) {
        revision = CampClientState.revision();
        var state = CampClientState.get();
        if (state == null) {
            CampClientState.request();
            return;
        }
        if (!state.enabled()) {
            return;
        }
        var right = area.x() + area.width() - 4;
        var y = slotsTop(area);
        for (var index = 0; index < state.slotsMax(); index++) {
            var slotIndex = index;
            var view = slot(state, index);
            var x = right;
            if (deletable(view)) {
                var key = "delete:" + view.get().campId();
                var label = deleteLabel(screen, view.get());
                var w = screen.font().width(label) + 8;
                x -= w;
                var campId = view.get().campId();
                screen.button(
                    label,
                    x,
                    y,
                    w,
                    SMALL_H,
                    true,
                    () -> confirmOr(screen, key, () -> CampClientState.send(CampActionPayload.Action.DELETE, campId, 0))
                );
                x -= 3;
            }
            if (selectable(state, index, view)) {
                var label = Component.translatable("pipboy.atomicdawn.camp.select");
                var w = screen.font().width(label) + 8;
                x -= w;
                screen.button(
                    label,
                    x,
                    y,
                    w,
                    SMALL_H,
                    true,
                    () -> CampClientState.send(CampActionPayload.Action.SELECT, NONE, slotIndex)
                );
            }
            y += SLOT_ROW;
        }
        var rows = actionRows(screen, state, area);
        var by = area.bottom() - rows * (BUTTON_H + 2) + 2;
        var bx = area.x() + 4;
        var limit = area.x() + area.width() - 4;
        for (var action : actions(screen, state)) {
            var w = screen.font().width(action.label()) + 10;
            if (bx > area.x() + 4 && bx + w > limit) {
                bx = area.x() + 4;
                by += BUTTON_H + 2;
            }
            screen.button(action.label(), bx, by, w, BUTTON_H, action.active(), action.run());
            bx += w + 4;
        }
    }

    static void tick(PipBoyScreen screen) {
        if (CampClientState.revision() != revision) {
            screen.refresh();
            return;
        }
        if (confirmUntil >= 0L && confirmUntil < screen.ticks()) {
            confirmUntil = -1L;
            confirmKey = "";
            screen.refresh();
        }
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var x = area.x() + 4;
        var y = area.y() + 4;
        var width = area.width() - 8;
        var state = CampClientState.get();
        var bottom = area.bottom() - 4 - (state == null ? 0 : actionRows(screen, state, area) * (BUTTON_H + 2));
        if (state == null) {
            screen.text(graphics, Component.translatable("pipboy.atomicdawn.camp.syncing"), x, y, PipBoyTheme.dim());
            return;
        }
        if (!state.enabled()) {
            screen.text(graphics, Component.translatable("pipboy.atomicdawn.camp.disabled"), x, y, PipBoyTheme.dim());
            return;
        }
        var player = Minecraft.getInstance().player;
        var scrap = player == null ? 0 : CampScrapbox.of(player).total();
        screen.text(
            graphics,
            Component.translatable("pipboy.atomicdawn.camp.slots_header", state.slotsUsed(), state.slotsMax()),
            x,
            y,
            PipBoyTheme.text()
        );
        var scrapText = Component.translatable("pipboy.atomicdawn.camp.scrapbox", scrap);
        screen.text(graphics, scrapText, x + width - screen.font().width(scrapText), y, PipBoyTheme.dim());
        y = slotsTop(area);
        for (var index = 0; index < state.slotsMax(); index++) {
            var view = slot(state, index);
            var selected = state.selectedSlot() == index;
            var label = Component.literal((selected ? "> " : "  ") + (index + 1) + ". ");
            Component status;
            int color;
            if (view.isEmpty()) {
                status = Component.translatable("pipboy.atomicdawn.camp.slot.empty");
                color = PipBoyTheme.faint();
            } else if (view.get().deployed()) {
                status = Component.translatable(
                    "pipboy.atomicdawn.camp.slot.deployed",
                    view.get().objects() - view.get().stored(),
                    view.get().budgetUsed(),
                    view.get().budgetLimit()
                );
                color = PipBoyTheme.text();
            } else {
                status = Component.translatable(
                    "pipboy.atomicdawn.camp.slot.stored",
                    view.get().stored(),
                    view.get().budgetUsed(),
                    view.get().budgetLimit()
                );
                color = PipBoyTheme.dim();
            }
            var available = width - reserved(screen, state, index) - 4;
            var line = label.copy().append(status);
            if (screen.font().width(line) > available) {
                var trimmed = screen.font().plainSubstrByWidth(line.getString(), Math.max(0, available - 8));
                line = Component.literal(trimmed + "...");
            }
            screen.text(graphics, line, x, y + 2, color);
            y += SLOT_ROW;
        }
        y += 4;
        var camp = state.camp();
        if (camp.isEmpty()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.camp.guidance", state.radius(), state.spacing()),
                x,
                y,
                width,
                bottom,
                PipBoyTheme.dim()
            );
            return;
        }
        var view = camp.get();
        var origin = view.origin();
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "location",
            Component.literal(origin.getX() + ", " + origin.getY() + ", " + origin.getZ()),
            PipBoyTheme.text()
        );
        y = pair(graphics, screen, x, y, width, "distance", distance(view), PipBoyTheme.dim());
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "claim",
            Component.translatable("pipboy.atomicdawn.camp.claim_value", state.radius(), state.up(), state.down()),
            PipBoyTheme.dim()
        );
        y = pair(
            graphics,
            screen,
            x,
            y,
            width,
            "budget",
            Component.literal(view.budgetUsed() + " / " + view.budgetLimit()),
            PipBoyTheme.text()
        );
        var ratio = view.budgetLimit() <= 0 ? 0F : Math.clamp(view.budgetUsed() / (float) view.budgetLimit(), 0F, 1F);
        graphics.fill(x, y, x + width, y + 4, PipBoyTheme.faint());
        var barColor = ratio >= 0.9F ? PipBoyTheme.warn() : PipBoyTheme.text();
        graphics.fill(x, y, x + Math.round(width * ratio), y + 4, barColor);
        y += 8;
        if (confirmKey.startsWith("store:") && confirmUntil >= screen.ticks()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.camp.store_warning"),
                x,
                y,
                width,
                bottom,
                PipBoyTheme.warn()
            );
        } else if (confirmKey.startsWith("partial:") && confirmUntil >= screen.ticks()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.camp.partial_warning"),
                x,
                y,
                width,
                bottom,
                PipBoyTheme.warn()
            );
        }
    }

    private static boolean sameDimension(CampSyncPayload.CampView view) {
        var level = Minecraft.getInstance().level;
        return level != null && view.dimension().equals(level.dimension().identifier().toString());
    }

    private static Component distance(CampSyncPayload.CampView view) {
        var player = Minecraft.getInstance().player;
        if (player == null || !sameDimension(view)) {
            return Component.translatable("pipboy.atomicdawn.camp.distance_other");
        }
        var meters = (int) Math.sqrt(player.blockPosition().distSqr(view.origin()));
        return Component.translatable("pipboy.atomicdawn.camp.distance_value", meters);
    }

    private static int pair(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        int x,
        int y,
        int width,
        String key,
        Component value,
        int color
    ) {
        screen.text(graphics, Component.translatable("pipboy.atomicdawn.camp." + key), x, y, PipBoyTheme.dim());
        screen.text(graphics, value, x + width - screen.font().width(value), y, color);
        return y + ROW;
    }
}

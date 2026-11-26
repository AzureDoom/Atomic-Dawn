package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.ghoul.GhoulPerks;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.network.SpecialActionPayload;
import mod.azure.atomicdawn.perks.LegendaryPerkCatalog;
import mod.azure.atomicdawn.perks.LegendaryPerkDefinition;
import mod.azure.atomicdawn.perks.PerkDefinition;
import mod.azure.atomicdawn.special.LegendaryProgress;
import mod.azure.atomicdawn.special.SpecialProgress;
import mod.azure.atomicdawn.special.SpecialService;
import mod.azure.atomicdawn.special.SpecialStatResolver;

final class LegendaryPage {

    private static final String SCROLL = "legendary";

    private static final int ROW = 13;

    private static final int TOP = 32;

    private LegendaryPage() {}

    private static List<Holder.Reference<LegendaryPerkDefinition>> cards(LocalPlayer player) {
        return LegendaryPerkCatalog.all(player.level().registryAccess());
    }

    private static int listWidth(Rect area) {
        return Math.max(130, area.width() * 9 / 20);
    }

    private static int rows(Rect area) {
        return Math.max(1, (area.height() - TOP) / ROW);
    }

    static boolean scroll(int direction) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        var count = cards(player).size();
        var next = Math.clamp(PipBoyClientState.scroll(SCROLL) + direction, 0, Math.max(0, count - 1));
        if (next == PipBoyClientState.scroll(SCROLL)) {
            return false;
        }
        PipBoyClientState.setScroll(SCROLL, next);
        return true;
    }

    private static Component slotLabel(SpecialProgress progress, int index, int unlocked) {
        var thresholds = AtomicDawn.CONFIG.common.progression.perks.legendary.LEGENDARY_SLOT_LEVELS;
        if (index >= unlocked) {
            var level = thresholds != null && index < thresholds.length ? thresholds[index] : -1;
            return level > 0
                ? Component.translatable("pipboy.atomicdawn.legendary.slot_locked", index + 1, level)
                : Component.translatable("pipboy.atomicdawn.legendary.slot_unavailable", index + 1);
        }
        var id = progress.legendary().slot(index).orElse(null);
        if (id == null) {
            return Component.translatable("pipboy.atomicdawn.legendary.slot_empty", index + 1);
        }
        return Component.literal((index + 1) + ": ")
            .append(Component.translatable(PerkDefinition.translationKey(id)))
            .append(" R" + progress.legendary().ownedRank(id));
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var progress = SpecialStatResolver.progress(player);
        var legendary = progress.legendary();
        var revision = progress.revision();
        var unlocked = SpecialService.unlockedLegendarySlots(progress.level());
        PipBoyClientState.legendarySlot = Math.clamp(
            PipBoyClientState.legendarySlot,
            0,
            LegendaryProgress.MAX_SLOTS - 1
        );

        for (var i = 0; i < LegendaryProgress.MAX_SLOTS; i++) {
            var index = i;
            var row = i / 3;
            var col = i % 3;
            var w = (area.width() - 4) / 3;
            screen.option(
                slotLabel(progress, i, unlocked),
                area.x() + col * (w + 2),
                area.y() + row * 14,
                w,
                13,
                i == PipBoyClientState.legendarySlot,
                () -> {
                    PipBoyClientState.legendarySlot = index;
                    legendary.slot(index).ifPresent(id -> PipBoyClientState.selectedLegendary = id);
                    screen.refresh();
                }
            );
        }

        var list = cards(player);
        var listW = listWidth(area);
        var first = Math.clamp(PipBoyClientState.scroll(SCROLL), 0, Math.max(0, list.size() - 1));
        var form = GhoulService.form(player);
        for (var r = 0; r < rows(area) && first + r < list.size(); r++) {
            var holder = list.get(first + r);
            var id = holder.key().identifier();
            var definition = holder.value();
            var owned = legendary.ownedRank(id);
            var marker = !GhoulPerks.eligible(definition.tags(), form)
                ? "[X]"
                : legendary.equipped(id) ? "[E]" : owned > 0 ? "[O]" : "[ ]";
            var label = Component.literal(marker + " ")
                .append(Component.translatable(PerkDefinition.translationKey(id)));
            if (owned > 0) {
                label.append(" " + owned + "/" + definition.maxRank());
            }
            screen.row(
                label,
                area.x(),
                area.y() + TOP + r * ROW,
                listW,
                ROW - 1,
                id.equals(PipBoyClientState.selectedLegendary),
                () -> {
                    PipBoyClientState.selectedLegendary = id;
                    screen.refresh();
                }
            );
        }

        var selectedId = PipBoyClientState.selectedLegendary;
        var selected = selectedId == null
            ? null
            : LegendaryPerkCatalog.get(player.level().registryAccess(), selectedId).orElse(null);
        if (selected == null) {
            return;
        }
        var detail = area.below(TOP).rightOf(listW, 8);
        var by = detail.bottom() - 13;
        var bx = detail.x();
        var owned = legendary.ownedRank(selectedId);
        var eligible = GhoulPerks.eligible(selected.tags(), form);
        var slot = PipBoyClientState.legendarySlot;

        if (legendary.equipped(selectedId)) {
            var label = Component.translatable("pipboy.atomicdawn.perks.unequip");
            var w = screen.font().width(label) + 10;
            screen.button(
                label,
                bx,
                by,
                w,
                13,
                true,
                () -> ClientPacketDistributor.sendToServer(
                    SpecialActionPayload.perk(SpecialActionPayload.Action.LEGENDARY_UNEQUIP, revision, selectedId, 0)
                )
            );
            bx += w + 2;
        }
        if (legendary.slotOf(selectedId) != slot) {
            var levelOk = selected.rank(1).minLevel() <= progress.level();
            var slotOk = slot < unlocked;
            var label = Component.translatable("pipboy.atomicdawn.legendary.equip", slot + 1);
            var w = screen.font().width(label) + 10;
            Component tooltip = !eligible
                ? Component.translatable("special.atomicdawn.result.wrong_form")
                : !slotOk
                    ? Component.translatable("special.atomicdawn.result.no_slot")
                    : !levelOk
                        ? Component.translatable("pipboy.atomicdawn.perks.requires_level", selected.rank(1).minLevel())
                        : null;
            screen.button(
                label,
                bx,
                by,
                w,
                13,
                eligible && slotOk && levelOk,
                tooltip,
                () -> ClientPacketDistributor.sendToServer(
                    SpecialActionPayload.perk(SpecialActionPayload.Action.LEGENDARY_EQUIP, revision, selectedId, slot)
                )
            );
            bx += w + 2;
        }
        if (owned > 0 && owned < selected.maxRank()) {
            var target = owned + 1;
            var cost = selected.upgradeCost(target);
            var levelOk = selected.rank(target).minLevel() <= progress.level();
            var affordable = legendary.coins() >= cost;
            var label = Component.translatable("pipboy.atomicdawn.legendary.upgrade", target, cost);
            var w = screen.font().width(label) + 10;
            if (bx + w <= detail.right()) {
                Component tooltip = !affordable
                    ? Component.translatable("special.atomicdawn.result.no_coins")
                    : !levelOk
                        ? Component.translatable(
                            "pipboy.atomicdawn.perks.requires_level",
                            selected.rank(target).minLevel()
                        )
                        : null;
                screen.button(
                    label,
                    bx,
                    by,
                    w,
                    13,
                    affordable && levelOk,
                    tooltip,
                    () -> ClientPacketDistributor.sendToServer(
                        SpecialActionPayload.perk(
                            SpecialActionPayload.Action.LEGENDARY_UPGRADE,
                            revision,
                            selectedId,
                            target
                        )
                    )
                );
            }
        }
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var progress = SpecialStatResolver.progress(player);
        var legendary = progress.legendary();
        var list = cards(player);
        var listW = listWidth(area);
        var detail = area.below(TOP).rightOf(listW, 8);

        if (!AtomicDawn.CONFIG.common.progression.perks.legendary.LEGENDARY_ENABLED) {
            screen.text(
                graphics,
                Component.translatable("special.atomicdawn.result.disabled"),
                area.x() + 4,
                area.y() + TOP + 2,
                PipBoyTheme.dim()
            );
            return;
        }
        if (list.isEmpty()) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.perks.empty"),
                area.x() + 4,
                area.y() + TOP + 2,
                PipBoyTheme.dim()
            );
        } else if (list.size() > rows(area)) {
            var first = PipBoyClientState.scroll(SCROLL);
            var trackTop = area.y() + TOP;
            var trackH = area.height() - TOP;
            var thumbH = Math.max(8, trackH * rows(area) / list.size());
            var thumbY = trackTop + (trackH - thumbH) * Math.min(first, list.size() - 1) / Math.max(1, list.size() - 1);
            graphics.fill(area.x() + listW + 2, trackTop, area.x() + listW + 4, trackTop + trackH, PipBoyTheme.faint());
            graphics.fill(area.x() + listW + 2, thumbY, area.x() + listW + 4, thumbY + thumbH, PipBoyTheme.text());
        }

        var x = detail.x();
        var y = detail.y() + 1;
        var bottom = detail.bottom() - 15;
        var coins = Component.translatable("pipboy.atomicdawn.legendary.coins", legendary.coins());
        screen.text(graphics, coins, x, y, legendary.coins() > 0 ? PipBoyTheme.warn() : PipBoyTheme.dim());
        y += 11;
        var next = SpecialService.nextLegendarySlotLevel(progress.level());
        if (next > 0) {
            screen.fitted(
                graphics,
                Component.translatable("pipboy.atomicdawn.legendary.next_slot", next),
                x,
                y,
                detail.width(),
                PipBoyTheme.dim()
            );
            y += 11;
        }

        var selectedId = PipBoyClientState.selectedLegendary;
        var definition = selectedId == null
            ? null
            : LegendaryPerkCatalog.get(player.level().registryAccess(), selectedId).orElse(null);
        if (definition == null) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.legendary.select"),
                x,
                y + 2,
                detail.width(),
                bottom,
                PipBoyTheme.dim()
            );
            return;
        }
        var owned = legendary.ownedRank(selectedId);
        screen.fitted(
            graphics,
            Component.translatable(PerkDefinition.translationKey(selectedId)),
            x,
            y,
            detail.width(),
            PipBoyTheme.text()
        );
        y += 11;
        var pips = new StringBuilder();
        for (var r = 1; r <= definition.maxRank(); r++) {
            pips.append(r <= owned ? "■" : "□");
        }
        var eligible = GhoulPerks.eligible(definition.tags(), GhoulService.form(player));
        var state = !eligible
            ? Component.translatable("pipboy.atomicdawn.legendary.state.wrong_form")
            : legendary.equipped(selectedId)
                ? Component.translatable("pipboy.atomicdawn.legendary.state.equipped", legendary.slotOf(selectedId) + 1)
                : owned > 0
                    ? Component.translatable("pipboy.atomicdawn.perks.state.owned")
                    : Component.translatable("pipboy.atomicdawn.legendary.state.free");
        screen.fitted(
            graphics,
            Component.literal(pips + "  ").append(state),
            x,
            y,
            detail.width(),
            eligible ? PipBoyTheme.dim() : PipBoyTheme.bad()
        );
        y += 12;
        y = screen.wrapped(
            graphics,
            Component.translatable(PerkDefinition.translationKey(selectedId) + ".desc"),
            x,
            y,
            detail.width(),
            bottom,
            PipBoyTheme.dim()
        );
        y += 3;
        for (var r = 1; r <= definition.maxRank() && y + 9 <= bottom; r++) {
            var rank = definition.rank(r);
            var color = r == owned ? PipBoyTheme.text() : r < owned ? PipBoyTheme.dim() : PipBoyTheme.faint();
            if (r == owned + 1 && owned > 0) {
                color = PipBoyTheme.warn();
            }
            var header = Component.translatable("pipboy.atomicdawn.legendary.rank_line", r, rank.coinCost());
            if (rank.minLevel() > 1) {
                header.append(Component.translatable("pipboy.atomicdawn.perks.rank_level", rank.minLevel()));
            }
            screen.fitted(graphics, header, x, y, detail.width(), color);
            y += 10;
            for (var effect : rank.effects()) {
                if (y + 9 > bottom) {
                    break;
                }
                screen.fitted(
                    graphics,
                    Component.literal("   ").append(effect.describe()),
                    x,
                    y,
                    detail.width(),
                    color
                );
                y += 10;
            }
        }
    }
}

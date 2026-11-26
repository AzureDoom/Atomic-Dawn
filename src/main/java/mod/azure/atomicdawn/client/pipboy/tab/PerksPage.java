package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.ghoul.GhoulPerks;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.network.SpecialActionPayload;
import mod.azure.atomicdawn.perks.PerkCatalog;
import mod.azure.atomicdawn.perks.PerkDefinition;
import mod.azure.atomicdawn.perks.PerkEffect;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialLoadout;
import mod.azure.atomicdawn.special.SpecialProgress;
import mod.azure.atomicdawn.special.SpecialStatResolver;

final class PerksPage {

    private static final String SCROLL = "perks";

    private static final int ROW = 13;

    private static final int TOP = 32;

    @Nullable
    private static EditBox renameBox;

    private PerksPage() {}

    enum CardState {

        EQUIPPED("[E]"),
        UPGRADABLE("[+]"),
        OWNED("[O]"),
        AVAILABLE("[+]"),
        LOCKED("[L]");

        final String marker;

        CardState(String marker) {
            this.marker = marker;
        }
    }

    static CardState state(SpecialProgress progress, Identifier id, PerkDefinition definition) {
        var owned = progress.ownedRank(id);
        var canLearn = progress.perkPicks() > 0 && owned < definition.maxRank()
            && definition.rank(owned + 1).minLevel() <= progress.level();
        if (progress.equippedRank(id) > 0) {
            return CardState.EQUIPPED;
        }
        if (owned > 0) {
            return canLearn ? CardState.UPGRADABLE : CardState.OWNED;
        }
        return canLearn ? CardState.AVAILABLE : CardState.LOCKED;
    }

    private static List<Holder.Reference<PerkDefinition>> visible(LocalPlayer player, SpecialProgress progress) {
        return PerkCatalog.all(player.level().registryAccess())
            .stream()
            .filter(
                holder -> PipBoyClientState.perkCategory == null || holder.value()
                    .special() == PipBoyClientState.perkCategory
            )
            .filter(holder -> {
                var id = holder.key().identifier();
                var state = state(progress, id, holder.value());
                return switch (PipBoyClientState.perkFilter) {
                    case ALL -> true;
                    case OWNED -> progress.ownedRank(id) > 0;
                    case EQUIPPED -> state == CardState.EQUIPPED;
                    case AVAILABLE -> state == CardState.AVAILABLE || state == CardState.UPGRADABLE;
                    case LOCKED -> state == CardState.LOCKED;
                };
            })
            .toList();
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
        var count = visible(player, SpecialStatResolver.progress(player)).size();
        var next = Math.clamp(PipBoyClientState.scroll(SCROLL) + direction, 0, Math.max(0, count - 1));
        if (next == PipBoyClientState.scroll(SCROLL)) {
            return false;
        }
        PipBoyClientState.setScroll(SCROLL, next);
        return true;
    }

    private static Component loadoutName(SpecialLoadout loadout, int index) {
        return loadout.name().isEmpty()
            ? Component.translatableWithFallback("pipboy.atomicdawn.loadout.default." + index, "Loadout " + (index + 1))
            : Component.literal(loadout.name());
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var progress = SpecialStatResolver.progress(player);
        var revision = progress.revision();

        var x = area.x();
        for (var i = 0; i < progress.loadouts().size(); i++) {
            var index = i;
            var active = i == progress.active();
            var label = Component.literal((active ? "> " : "") + (i + 1) + ": ")
                .append(loadoutName(progress.loadouts().get(i), i));
            var w = Math.min(90, screen.font().width(label) + 10);
            screen.option(
                label,
                x,
                area.y(),
                w,
                13,
                active,
                Component.translatable("pipboy.atomicdawn.loadout.switch_hint"),
                () -> ClientPacketDistributor.sendToServer(
                    SpecialActionPayload.loadout(SpecialActionPayload.Action.SWITCH_LOADOUT, revision, index, "")
                )
            );
            x += w + 2;
        }
        var boxW = 74;
        var box = new EditBox(
            screen.font(),
            x + 4,
            area.y() + 1,
            boxW,
            11,
            Component.translatable("pipboy.atomicdawn.loadout.rename")
        );
        box.setMaxLength(SpecialLoadout.MAX_NAME_LENGTH);
        box.setValue(progress.activeLoadout().name());
        renameBox = screen.widget(box);
        screen.button(
            Component.translatable("pipboy.atomicdawn.loadout.rename"),
            x + 6 + boxW,
            area.y(),
            48,
            13,
            true,
            () -> {
                if (renameBox != null) {
                    ClientPacketDistributor.sendToServer(
                        SpecialActionPayload.loadout(
                            SpecialActionPayload.Action.RENAME_LOADOUT,
                            revision,
                            progress.active(),
                            renameBox.getValue()
                        )
                    );
                }
            }
        );

        var fx = area.x();
        var fy = area.y() + 16;
        for (var filter : PipBoyClientState.PerkFilter.values()) {
            var selected = filter == PipBoyClientState.perkFilter;
            var label = Component.translatable(
                "pipboy.atomicdawn.perks.filter." + filter.name().toLowerCase(Locale.ROOT)
            );
            var w = screen.font().width(label) + 8;
            screen.option(label, fx, fy, w, 12, selected, () -> {
                PipBoyClientState.perkFilter = filter;
                PipBoyClientState.setScroll(SCROLL, 0);
                screen.refresh();
            });
            fx += w + 2;
        }
        var category = PipBoyClientState.perkCategory;
        var categoryLabel = Component.translatable(
            "pipboy.atomicdawn.perks.category",
            category == null
                ? Component.translatable("pipboy.atomicdawn.perks.filter.all")
                : Component.literal(String.valueOf(category.letter()))
        );
        screen.button(categoryLabel, fx, fy, screen.font().width(categoryLabel) + 10, 12, true, () -> {
            var current = PipBoyClientState.perkCategory;
            PipBoyClientState.perkCategory = current == null
                ? SpecialAttribute.STRENGTH
                : current.ordinal() + 1 >= SpecialAttribute.COUNT
                    ? null
                    : SpecialAttribute.byIndex(current.ordinal() + 1);
            PipBoyClientState.setScroll(SCROLL, 0);
            screen.refresh();
        });

        var cards = visible(player, progress);
        var listW = listWidth(area);
        var first = Math.clamp(cards.size() - 1, 0, PipBoyClientState.scroll(SCROLL));
        var rows = rows(area);
        for (var r = 0; r < rows && first + r < cards.size(); r++) {
            var holder = cards.get(first + r);
            var id = holder.key().identifier();
            var definition = holder.value();
            var state = state(progress, id, definition);
            var owned = progress.ownedRank(id);
            MutableComponent label = Component.literal(state.marker + " " + definition.special().letter() + " ")
                .append(Component.translatable(PerkDefinition.translationKey(id)));
            if (owned > 0) {
                label.append(" " + owned + "/" + definition.maxRank());
            }
            var selected = id.equals(PipBoyClientState.selectedPerk);
            screen.row(label, area.x(), area.y() + TOP + r * ROW, listW, ROW - 1, selected, () -> {
                PipBoyClientState.selectedPerk = id;
                screen.refresh();
            });
        }

        var selectedId = PipBoyClientState.selectedPerk;
        var selected = selectedId == null
            ? null
            : PerkCatalog.get(player.level().registryAccess(), selectedId).orElse(null);
        if (selected == null) {
            return;
        }
        var detail = area.below(TOP).rightOf(listW, 8);
        var owned = progress.ownedRank(selectedId);
        var equipped = progress.equippedRank(selectedId);
        var by = detail.bottom() - 13;
        var bx = detail.x();

        if (owned < selected.maxRank()) {
            var next = selected.rank(owned + 1);
            var canLearn = progress.perkPicks() > 0 && next.minLevel() <= progress.level();
            Component tooltip = progress.perkPicks() <= 0
                ? Component.translatable("special.atomicdawn.result.no_perk_picks")
                : next.minLevel() > progress.level()
                    ? Component.translatable("pipboy.atomicdawn.perks.requires_level", next.minLevel())
                    : null;
            var label = Component.translatable(
                owned == 0 ? "pipboy.atomicdawn.perks.learn" : "pipboy.atomicdawn.perks.rank_up",
                owned + 1
            );
            var w = screen.font().width(label) + 10;
            screen.button(
                label,
                bx,
                by,
                w,
                13,
                canLearn,
                tooltip,
                () -> ClientPacketDistributor.sendToServer(
                    SpecialActionPayload.perk(SpecialActionPayload.Action.LEARN_PERK, revision, selectedId, owned + 1)
                )
            );
            bx += w + 2;
        }
        if (equipped > 0) {
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
                    SpecialActionPayload.perk(SpecialActionPayload.Action.UNEQUIP_PERK, revision, selectedId, 0)
                )
            );
            bx += w + 2;
        }
        var access = player.level().registryAccess();
        var form = GhoulService.form(player);
        var eligible = GhoulPerks.eligible(selected, form);
        if (owned > 0 && AtomicDawn.CONFIG.common.progression.perks.legendary.LEGENDARY_ENABLED) {
            var keep = owned - 1;
            var locked = false;
            for (var loadout : progress.loadouts()) {
                locked |= loadout.equipped().getOrDefault(selectedId, 0) > keep;
            }
            var coins = AtomicDawn.CONFIG.common.progression.perks.legendary.LEGENDARY_COINS_PER_SCRAPPED_RANK;
            var label = Component.translatable("pipboy.atomicdawn.perks.scrap", coins);
            var w = screen.font().width(label) + 10;
            if (bx + w <= detail.right()) {
                screen.button(
                    label,
                    bx,
                    by,
                    w,
                    13,
                    !locked,
                    locked ? Component.translatable("special.atomicdawn.result.protected") : null,
                    () -> ClientPacketDistributor.sendToServer(
                        SpecialActionPayload.perk(SpecialActionPayload.Action.SCRAP_PERK, revision, selectedId, 1)
                    )
                );
                bx += w + 2;
            }
        }
        for (var rank = 1; rank <= owned; rank++) {
            if (rank == equipped) {
                continue;
            }
            var targetRank = rank;
            var candidate = progress.activeLoadout().equip(selectedId, rank);
            var used = SpecialStatResolver.capacityUsed(access, progress, candidate, selected.special());
            var capacity = SpecialStatResolver.capacity(access, progress, candidate, selected.special(), form);
            var fits = used <= capacity;
            var levelOk = selected.rank(rank).minLevel() <= progress.level();
            var label = Component.translatable("pipboy.atomicdawn.perks.equip", rank);
            var w = screen.font().width(label) + 10;
            if (bx + w > detail.right()) {
                break;
            }
            Component tooltip = !eligible
                ? Component.translatable("special.atomicdawn.result.wrong_form")
                : !fits
                    ? Component.translatable("pipboy.atomicdawn.perks.no_capacity", used, capacity)
                    : !levelOk
                        ? Component.translatable(
                            "pipboy.atomicdawn.perks.requires_level",
                            selected.rank(rank).minLevel()
                        )
                        : null;
            screen.button(
                label,
                bx,
                by,
                w,
                13,
                eligible && fits && levelOk,
                tooltip,
                () -> ClientPacketDistributor.sendToServer(
                    SpecialActionPayload.perk(SpecialActionPayload.Action.EQUIP_PERK, revision, selectedId, targetRank)
                )
            );
            bx += w + 2;
        }
    }

    static void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var progress = SpecialStatResolver.progress(player);
        var picks = Component.translatable("pipboy.atomicdawn.perks.picks", progress.perkPicks());
        screen.text(
            graphics,
            picks,
            area.right() - screen.font().width(picks) - 2,
            area.y() + 3,
            progress.perkPicks() > 0 ? PipBoyTheme.warn() : PipBoyTheme.dim()
        );

        var cards = visible(player, progress);
        var listW = listWidth(area);
        if (cards.isEmpty()) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.perks.empty"),
                area.x() + 4,
                area.y() + TOP + 2,
                PipBoyTheme.dim()
            );
        } else if (cards.size() > rows(area)) {
            var first = PipBoyClientState.scroll(SCROLL);
            var trackTop = area.y() + TOP;
            var trackH = area.height() - TOP;
            var thumbH = Math.max(8, trackH * rows(area) / cards.size());
            var thumbY = trackTop + (trackH - thumbH) * Math.min(first, cards.size() - 1) / Math.max(
                1,
                cards.size() - 1
            );
            graphics.fill(area.x() + listW + 2, trackTop, area.x() + listW + 4, trackTop + trackH, PipBoyTheme.faint());
            graphics.fill(area.x() + listW + 2, thumbY, area.x() + listW + 4, thumbY + thumbH, PipBoyTheme.text());
        }

        var selectedId = PipBoyClientState.selectedPerk;
        var detail = area.below(TOP).rightOf(listW, 8);
        if (selectedId == null) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.perks.select"),
                detail.x(),
                detail.y() + 2,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var access = player.level().registryAccess();
        var definition = PerkCatalog.get(access, selectedId).orElse(null);
        if (definition == null) {
            return;
        }
        var owned = progress.ownedRank(selectedId);
        var equipped = progress.equippedRank(selectedId);
        var x = detail.x();
        var y = detail.y() + 1;
        var bottom = detail.bottom() - 15;
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
        var special = definition.special();
        var capacity = SpecialStatResolver.capacityUsed(access, progress, progress.activeLoadout(), special);
        screen.fitted(
            graphics,
            Component.translatable(
                "pipboy.atomicdawn.perks.header",
                pips.toString(),
                Component.translatable(special.translationKey()),
                capacity,
                SpecialStatResolver.capacity(
                    access,
                    progress,
                    progress.activeLoadout(),
                    special,
                    GhoulService.form(player)
                )
            ),
            x,
            y,
            detail.width(),
            PipBoyTheme.dim()
        );
        y += 11;
        var stateKey = equipped > 0
            ? Component.translatable("pipboy.atomicdawn.perks.state.equipped", equipped)
            : owned > 0
                ? Component.translatable("pipboy.atomicdawn.perks.state.owned")
                : Component.translatable("pipboy.atomicdawn.perks.state.locked");
        screen.text(graphics, stateKey, x, y, equipped > 0 ? PipBoyTheme.text() : PipBoyTheme.dim());
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
            var color = r == equipped ? PipBoyTheme.text() : r <= owned ? PipBoyTheme.dim() : PipBoyTheme.faint();
            if (r > owned && r == owned + 1) {
                color = PipBoyTheme.warn();
            }
            var header = Component.translatable("pipboy.atomicdawn.perks.rank_line", r, definition.cost(r));
            if (rank.minLevel() > 1) {
                header.append(Component.translatable("pipboy.atomicdawn.perks.rank_level", rank.minLevel()));
            }
            screen.fitted(
                graphics,
                Component.literal(r == equipped ? "> " : "  ").append(header),
                x,
                y,
                detail.width(),
                color
            );
            y += 10;
            for (PerkEffect effect : rank.effects()) {
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

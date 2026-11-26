package mod.azure.atomicdawn.client.camp;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.camp.CampMaterials;
import mod.azure.atomicdawn.camp.CampWorkshopService;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiRenderUtil;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.network.CampSyncPayload;

public final class CampWorkshopHud implements ADHudLayer {

    private static final int MIN_WIDTH = 170;

    private static final int ROW = 10;

    private static final int ICON_ROW = 18;

    private static final int PADDING = 4;

    private record Line(
        Component text,
        int color,
        @Nullable ItemStack icon,
        int gapBefore
    ) {}

    @Override
    public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = ADGuiRenderUtil.isHudHidden();
        if (player == null || hudHidden || minecraft.gui.screen() instanceof PipBoyScreen) {
            return;
        }
        var font = minecraft.font;
        var window = minecraft.getWindow();
        var screenWidth = window.getGuiScaledWidth();
        var screenHeight = window.getGuiScaledHeight();
        renderToast(graphics, font, screenWidth, screenHeight);
        if (!CampWorkshopClient.active()) {
            return;
        }
        var camp = CampWorkshopClient.workshopCamp(player);
        if (camp == null) {
            return;
        }
        var lines = new ArrayList<Line>();
        if (CampWorkshopClient.movingModule()) {
            moduleLines(lines);
        } else if (CampWorkshopClient.selecting()) {
            selectionLines(lines);
        } else if (CampWorkshopClient.placingBlueprint()) {
            blueprintLines(lines, player);
        } else {
            buildableLines(lines, player, camp);
        }
        lines.add(hintLine("place", 4));
        lines.add(hintLine("scrap", 0));
        lines.add(hintLine("copy", 0));
        lines.add(keyLine("catalog", ADKeyMappings.WORKSHOP_CATALOG.getTranslatedKeyMessage()));
        lines.add(keyLine("rotate", ADKeyMappings.WORKSHOP_ROTATE.getTranslatedKeyMessage()));
        lines.add(keyLine("mode", ADKeyMappings.WORKSHOP_MODE.getTranslatedKeyMessage()));
        lines.add(keyLine("blueprint", ADKeyMappings.WORKSHOP_BLUEPRINT.getTranslatedKeyMessage()));
        lines.add(keyLine("move", ADKeyMappings.WORKSHOP_MOVE.getTranslatedKeyMessage()));
        lines.add(keyLine("exit", ADKeyMappings.WORKSHOP.getTranslatedKeyMessage()));

        var maxWidth = Math.max(MIN_WIDTH, screenWidth * 2 / 5);
        var title = Component.translatable("camp.atomicdawn.workshop.title");
        var budget = Component.literal(camp.budgetUsed() + " / " + camp.budgetLimit());
        var content = font.width(title) + font.width(budget) + 12;
        for (var line : lines) {
            content = Math.max(content, font.width(line.text()) + (line.icon() != null ? 20 : 0));
        }
        var width = Math.clamp(content, MIN_WIDTH, maxWidth);
        var height = ROW + 7;
        for (var line : lines) {
            var lineRows = rows(font, line, width) * ROW;
            height += line.gapBefore() + (line.icon() != null ? Math.max(ICON_ROW, 4 + lineRows) : lineRows);
        }

        var x = screenWidth - width - PADDING - 2;
        var y = PADDING + 2;
        var background = PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xE8);
        graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING, background);
        graphics.fill(x - PADDING, y - PADDING, x - PADDING + 1, y + height + PADDING, PipBoyTheme.border());
        graphics.text(font, title, x, y, PipBoyTheme.text(), true);
        graphics.text(font, budget, x + width - font.width(budget), y, PipBoyTheme.text(), true);
        y += ROW;
        var limitValue = camp.budgetLimit();
        var ratio = limitValue <= 0 ? 0F : Math.clamp(camp.budgetUsed() / (float) limitValue, 0F, 1F);
        graphics.fill(x, y, x + width, y + 3, PipBoyTheme.faint());
        var barColor = ratio >= 0.9F ? PipBoyTheme.warn() : PipBoyTheme.text();
        graphics.fill(x, y, x + Math.round(width * ratio), y + 3, barColor);
        y += 7;
        for (var line : lines) {
            y += line.gapBefore();
            if (line.icon() != null) {
                graphics.item(line.icon(), x, y);
                var textWidth = width - 20;
                var textY = y + 4;
                for (var part : font.split(line.text(), textWidth)) {
                    graphics.text(font, part, x + 20, textY, line.color(), true);
                    textY += ROW;
                }
                y += Math.max(ICON_ROW, textY - y);
                continue;
            }
            for (var part : font.split(line.text(), width)) {
                graphics.text(font, part, x, y, line.color(), true);
                y += ROW;
            }
        }
    }

    private static void buildableLines(
        ArrayList<Line> lines,
        LocalPlayer player,
        CampSyncPayload.CampView camp
    ) {
        var definition = CampWorkshopClient.definition();
        if (definition.isEmpty()) {
            lines.add(
                new Line(
                    Component.translatable(
                        "camp.atomicdawn.workshop.select_first",
                        ADKeyMappings.WORKSHOP_CATALOG.getTranslatedKeyMessage()
                    ),
                    PipBoyTheme.text(),
                    null,
                    0
                )
            );
        } else {
            var buildable = definition.get();
            lines.add(new Line(buildable.name(), PipBoyTheme.text(), buildable.icon(), 0));
            for (var ingredient : buildable.cost()) {
                var item = ingredient.ingredient().items().findFirst().orElse(null);
                if (item == null) {
                    continue;
                }
                var have = player.hasInfiniteMaterials() ? ingredient.count() : CampMaterials.count(player, ingredient);
                var text = Component.literal(have + "/" + ingredient.count() + " ")
                    .append(new ItemStack(item).getHoverName());
                var color = have >= ingredient.count() ? PipBoyTheme.text() : PipBoyTheme.bad();
                lines.add(new Line(text, color, null, 0));
            }
            var selected = CampWorkshopClient.selected();
            var limit = buildable.limit() > 0 && selected != null
                ? Component.translatable("camp.atomicdawn.workshop.limit", camp.count(selected), buildable.limit())
                : Component.translatable("camp.atomicdawn.workshop.unlimited");
            lines.add(
                new Line(
                    Component.translatable("camp.atomicdawn.workshop.cost", buildable.budget())
                        .append(" | ")
                        .append(limit),
                    PipBoyTheme.text(),
                    null,
                    0
                )
            );
        }
        lines.add(
            new Line(
                Component.translatable(
                    "camp.atomicdawn.workshop.mode_facing",
                    CampWorkshopClient.mode().label(),
                    Component.translatable(
                        "camp.atomicdawn.facing." + CampWorkshopClient.facing().getSerializedName()
                    )
                ),
                PipBoyTheme.text(),
                null,
                0
            )
        );
        var target = CampWorkshopClient.target();
        if (definition.isPresent() && target != null) {
            var failure = CampWorkshopClient.failure(target);
            var status = failure.map(CampWorkshopService.Failure::message)
                .orElse(Component.translatable("camp.atomicdawn.workshop.ready"));
            lines.add(new Line(status, failure.isPresent() ? PipBoyTheme.bad() : PipBoyTheme.text(), null, 0));
        }
    }

    private static void selectionLines(ArrayList<Line> lines) {
        var first = CampWorkshopClient.cornerA();
        var second = CampWorkshopClient.cornerB();
        Component status;
        if (first == null) {
            status = Component.translatable("camp.atomicdawn.workshop.selection_none");
        } else if (second == null) {
            status = Component.translatable("camp.atomicdawn.workshop.selection_half");
        } else {
            status = Component.translatable(
                "camp.atomicdawn.workshop.selection",
                Math.abs(first.getX() - second.getX()) + 1,
                Math.abs(first.getY() - second.getY()) + 1,
                Math.abs(first.getZ() - second.getZ()) + 1
            );
        }
        lines.add(new Line(Component.translatable("camp.atomicdawn.workshop.selecting"), PipBoyTheme.text(), null, 0));
        lines.add(new Line(status, PipBoyTheme.text(), null, 0));
        if (CampWorkshopClient.hasSelection()) {
            lines.add(
                new Line(
                    Component.translatable(
                        "camp.atomicdawn.workshop.selection_save",
                        ADKeyMappings.WORKSHOP_CATALOG.getTranslatedKeyMessage()
                    ),
                    PipBoyTheme.dim(),
                    null,
                    0
                )
            );
        }
    }

    private static void moduleLines(ArrayList<Line> lines) {
        lines.add(
            new Line(Component.translatable("camp.atomicdawn.workshop.moving_module"), PipBoyTheme.text(), null, 0)
        );
        var target = CampWorkshopClient.target();
        var valid = target != null && CampWorkshopClient.moduleMoveValid(target);
        lines.add(
            new Line(
                Component.translatable(
                    valid ? "camp.atomicdawn.workshop.ready" : "camp.atomicdawn.module.move_invalid"
                ),
                valid ? PipBoyTheme.text() : PipBoyTheme.bad(),
                null,
                0
            )
        );
    }

    private static void blueprintLines(ArrayList<Line> lines, LocalPlayer player) {
        var blueprint = CampWorkshopClient.blueprint();
        if (blueprint.isEmpty()) {
            var key = CampWorkshopClient.selectedStored() != null
                ? "camp.atomicdawn.storage.waiting"
                : "camp.atomicdawn.blueprint.unknown";
            lines.add(new Line(Component.translatable(key), PipBoyTheme.dim(), null, 0));
            return;
        }
        var access = player.level().registryAccess();
        var stored = CampWorkshopClient.selectedStored() != null;
        lines.add(
            new Line(
                Component.translatable(
                    stored ? "camp.atomicdawn.workshop.stored" : "camp.atomicdawn.workshop.blueprint",
                    CampWorkshopClient.placementName()
                ),
                PipBoyTheme.text(),
                null,
                0
            )
        );
        if (stored) {
            lines.add(
                new Line(
                    Component.translatable("camp.atomicdawn.workshop.cost", blueprint.get().budget(access)),
                    PipBoyTheme.text(),
                    null,
                    0
                )
            );
        } else {
            lines.add(
                new Line(
                    Component.translatable(
                        "camp.atomicdawn.workshop.blueprint_info",
                        blueprint.get().kind().label(),
                        blueprint.get().entries().size(),
                        blueprint.get().budget(access)
                    ),
                    PipBoyTheme.text(),
                    null,
                    0
                )
            );
        }
        var cost = stored ? List.<SizedIngredient>of() : blueprint.get().cost(access);
        for (var ingredient : cost) {
            var item = ingredient.ingredient().items().findFirst().orElse(null);
            if (item == null) {
                continue;
            }
            var have = player.hasInfiniteMaterials() ? ingredient.count() : CampMaterials.count(player, ingredient);
            var text = Component.literal(have + "/" + ingredient.count() + " ")
                .append(new ItemStack(item).getHoverName());
            var color = have >= ingredient.count() ? PipBoyTheme.text() : PipBoyTheme.bad();
            lines.add(new Line(text, color, null, 0));
        }
        lines.add(
            new Line(
                Component.translatable(
                    "camp.atomicdawn.workshop.facing",
                    Component.translatable(
                        "camp.atomicdawn.facing." + CampWorkshopClient.facing().getSerializedName()
                    )
                ),
                PipBoyTheme.text(),
                null,
                0
            )
        );
        var failure = CampWorkshopClient.blueprintFailure();
        var status = failure.map(CampWorkshopService.Failure::message)
            .orElse(Component.translatable("camp.atomicdawn.workshop.ready"));
        lines.add(new Line(status, failure.isPresent() ? PipBoyTheme.bad() : PipBoyTheme.text(), null, 0));
    }

    private static int rows(Font font, Line line, int width) {
        var available = line.icon() != null ? width - 20 : width;
        return Math.max(1, font.split(line.text(), available).size());
    }

    private static Line hintLine(String key, int gap) {
        return new Line(Component.translatable("camp.atomicdawn.workshop.hint." + key), PipBoyTheme.dim(), null, gap);
    }

    private static Line keyLine(String key, Component bound) {
        var text = Component.translatable("camp.atomicdawn.workshop.hint." + key, bound);
        return new Line(text, PipBoyTheme.dim(), null, 0);
    }

    private static void renderToast(ADGuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
        var toast = CampWorkshopClient.toast();
        if (toast == null) {
            return;
        }
        var lines = font.split(toast, Math.max(120, screenWidth - 40));
        var w = 0;
        for (var line : lines) {
            w = Math.max(w, font.width(line));
        }
        var ty = screenHeight - 104 - (lines.size() - 1) * ROW;
        var tx = screenWidth / 2 - w / 2;
        var background = PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xE8);
        graphics.fill(tx - 4, ty - 3, tx + w + 4, ty + lines.size() * ROW, background);
        for (var line : lines) {
            graphics.text(font, line, screenWidth / 2 - font.width(line) / 2, ty, PipBoyTheme.text(), true);
            ty += ROW;
        }
    }
}

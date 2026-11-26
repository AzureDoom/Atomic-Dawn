package mod.azure.atomicdawn.client.ghoul;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.ghoul.GhoulTreatmentService;
import mod.azure.atomicdawn.network.GhoulActionPayload;

public final class GhoulConfirmScreen extends ADScreen {

    private static final int PANEL_WIDTH = 300;

    private static final int LINE_HEIGHT = 10;

    private final GhoulTreatmentService.Kind kind;

    private final List<FormattedCharSequence> lines = new ArrayList<>();

    private boolean answered;

    private int panelTop;

    private int panelBottom;

    public GhoulConfirmScreen(GhoulTreatmentService.Kind kind) {
        super(Component.translatable("ghoul.atomicdawn.confirm." + key(kind) + ".title"));
        this.kind = kind;
    }

    private static String key(GhoulTreatmentService.Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void init() {
        lines.clear();
        for (var body : body()) {
            lines.addAll(font.split(body, PANEL_WIDTH - 20));
            lines.add(FormattedCharSequence.EMPTY);
        }
        var contentHeight = 20 + lines.size() * LINE_HEIGHT + 30;
        panelTop = Math.max(4, (height - contentHeight) / 2);
        panelBottom = panelTop + contentHeight;
        var centerX = width / 2;
        var buttonY = panelBottom - 26;
        addRenderableWidget(
            Button.builder(Component.translatable("ghoul.atomicdawn.confirm.accept"), ignored -> answer(true))
                .pos(centerX - 104, buttonY)
                .size(100, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, ignored -> answer(false))
                .pos(centerX + 4, buttonY)
                .size(100, 20)
                .build()
        );
    }

    private List<Component> body() {
        var result = new ArrayList<Component>();
        var config = AtomicDawn.CONFIG.common.progression.ghoul;
        if (kind == GhoulTreatmentService.Kind.GHOULIFY) {
            result.add(
                Component.translatable(
                    "ghoul.atomicdawn.confirm.ghoulify.radiation",
                    Math.round(config.glow.GLOW_CONVERSION * 100.0)
                )
            );
            result.add(Component.translatable("ghoul.atomicdawn.confirm.ghoulify.feral"));
            result.add(Component.translatable("ghoul.atomicdawn.confirm.ghoulify.keep"));
            if (!config.general.ALLOW_REVERSION) {
                result.add(Component.translatable("ghoul.atomicdawn.confirm.no_reversion"));
            } else {
                result.add(cooldownLine());
            }
        } else {
            result.add(Component.translatable("ghoul.atomicdawn.confirm.restore.human"));
            result.add(Component.translatable("ghoul.atomicdawn.confirm.restore.keep"));
            result.add(cooldownLine());
        }
        return result;
    }

    private static Component cooldownLine() {
        var hours = AtomicDawn.CONFIG.common.progression.ghoul.general.FORM_CHANGE_COOLDOWN_HOURS;
        if (hours <= 0.0) {
            return Component.translatable("ghoul.atomicdawn.confirm.cooldown.none");
        }
        return Component.translatable("ghoul.atomicdawn.confirm.cooldown", String.format(Locale.ROOT, "%.0f", hours));
    }

    private void answer(boolean accept) {
        if (answered) {
            return;
        }
        answered = true;
        if (accept) {
            GhoulClient.send(
                kind == GhoulTreatmentService.Kind.GHOULIFY
                    ? GhoulActionPayload.Action.CONFIRM_TRANSFORM
                    : GhoulActionPayload.Action.CONFIRM_REVERT,
                0
            );
        } else {
            GhoulClient.send(GhoulActionPayload.Action.CANCEL, 0);
        }
        onClose();
    }

    @Override
    public void removed() {
        if (!answered) {
            answered = true;
            GhoulClient.send(GhoulActionPayload.Action.CANCEL, 0);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void drawBackground(ADGuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.drawBackground(graphics, mouseX, mouseY, partialTick);
        var left = width / 2 - PANEL_WIDTH / 2;
        var right = left + PANEL_WIDTH;
        graphics.fill(left - 1, panelTop - 1, right + 1, panelBottom + 1, PipBoyTheme.border());
        graphics.fill(left, panelTop, right, panelBottom, PipBoyTheme.panel());
        graphics.text(font, title, width / 2 - font.width(title) / 2, panelTop + 6, PipBoyTheme.text(), false);
        var y = panelTop + 20;
        for (var line : lines) {
            graphics.text(font, line, left + 10, y, PipBoyTheme.dim(), false);
            y += LINE_HEIGHT;
        }
    }
}

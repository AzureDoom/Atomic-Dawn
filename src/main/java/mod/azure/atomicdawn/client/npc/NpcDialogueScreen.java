package mod.azure.atomicdawn.client.npc;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiInput;
import mod.azure.atomicdawn.client.helper.ADScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.network.NpcDialogueChoicePayload;
import mod.azure.atomicdawn.network.NpcDialoguePayload;

public final class NpcDialogueScreen extends ADScreen {

    private static final int MAX_WIDTH = 420;

    private static final int LINE = 10;

    private static final int OPTION_H = 18;

    private static final int OPTION_GAP = 2;

    private NpcDialoguePayload dialogue;

    private List<FormattedCharSequence> lines = List.of();

    private boolean closedByServer;

    private boolean sentClose;

    private int panelLeft;

    private int panelTop;

    private int panelWidth;

    private int panelBottom;

    public NpcDialogueScreen(NpcDialoguePayload dialogue) {
        super(dialogue.name());
        this.dialogue = dialogue;
    }

    public int entityId() {
        return dialogue.entityId();
    }

    public void update(NpcDialoguePayload payload) {
        this.dialogue = payload;
        rebuildWidgets();
    }

    public void closeFromServer() {
        closedByServer = true;
        onClose();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, width - 20);
        panelLeft = (width - panelWidth) / 2;
        lines = font.split(dialogue.text(), panelWidth - 16);
        var options = dialogue.options();
        var optionsHeight = options.size() * (OPTION_H + OPTION_GAP);
        var header = dialogue.title().getString().isEmpty() ? 16 : 26;
        var height = header + lines.size() * LINE + 8 + optionsHeight + 6;
        panelBottom = this.height - 10;
        panelTop = Math.max(4, panelBottom - height);
        var y = panelTop + header + lines.size() * LINE + 8;
        for (var i = 0; i < options.size(); i++) {
            var index = i;
            var label = Component.literal((i + 1) + ". ").append(options.get(i));
            addRenderableWidget(
                Button.builder(label, ignored -> choose(index))
                    .pos(panelLeft + 8, y)
                    .size(panelWidth - 16, OPTION_H)
                    .build()
            );
            y += OPTION_H + OPTION_GAP;
        }
    }

    private void choose(int index) {
        NpcClient.choose(dialogue.entityId(), index);
    }

    @Override
    public boolean keyPressed(ADGuiInput.Key event) {
        var key = event.key();
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
            var index = key - InputConstants.KEY_1;
            if (index < dialogue.options().size()) {
                choose(index);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        if (!closedByServer && !sentClose) {
            sentClose = true;
            NpcClient.choose(dialogue.entityId(), NpcDialogueChoicePayload.CLOSE);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void drawBackground(ADGuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + panelWidth + 1, panelBottom + 1, PipBoyTheme.border());
        graphics.fill(
            panelLeft,
            panelTop,
            panelLeft + panelWidth,
            panelBottom,
            PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xE0)
        );
        var y = panelTop + 5;
        graphics.text(font, dialogue.name(), panelLeft + 8, y, PipBoyTheme.text(), true);
        if (!dialogue.title().getString().isEmpty()) {
            y += LINE;
            graphics.text(font, dialogue.title(), panelLeft + 8, y, PipBoyTheme.dim(), false);
        }
        y += LINE + 6;
        for (var line : lines) {
            graphics.text(font, line, panelLeft + 8, y, PipBoyTheme.text(), false);
            y += LINE;
        }
    }
}

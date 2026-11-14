package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import mod.azure.atomicdawn.camp.CampAccess;
import mod.azure.atomicdawn.client.camp.CampClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.network.CampActionPayload;
import mod.azure.atomicdawn.network.CampTrustPayload;

final class CampAccessPage {

    private static final int ROW = 14;

    private static final int BUTTON_H = 12;

    private static final UUID NONE = new UUID(0L, 0L);

    private static int revision = Integer.MIN_VALUE;

    @Nullable
    private static EditBox nameBox;

    private CampAccessPage() {}

    private static int flagsTop(Rect area) {
        return area.y() + 4 + 12;
    }

    private static int trustTop(Rect area) {
        return flagsTop(area) + CampAccess.Flag.values().length * ROW + 10;
    }

    static void init(PipBoyScreen screen, Rect area) {
        revision = CampClientState.revision();
        var state = CampClientState.get();
        if (state == null) {
            CampClientState.request();
            return;
        }
        var access = state.access();
        if (access.isEmpty()) {
            return;
        }
        var right = area.x() + area.width() - 4;
        var y = flagsTop(area);
        for (var flag : CampAccess.Flag.values()) {
            var on = access.get().flag(flag.ordinal());
            var label = Component.translatable(on ? "pipboy.atomicdawn.camp.on" : "pipboy.atomicdawn.camp.off");
            var w = Math.max(30, screen.font().width(label) + 10);
            screen.button(
                label,
                right - w,
                y,
                w,
                BUTTON_H,
                true,
                () -> CampClientState.send(CampActionPayload.Action.TOGGLE_ACCESS, NONE, flag.ordinal())
            );
            y += ROW;
        }
        y = trustTop(area) + 12;
        var trustLabel = Component.translatable("pipboy.atomicdawn.camp.trust");
        var trustW = screen.font().width(trustLabel) + 10;
        var boxW = Math.max(60, area.width() - 8 - trustW - 4);
        var previous = nameBox == null ? "" : nameBox.getValue();
        var box = new EditBox(
            screen.font(),
            area.x() + 4,
            y,
            boxW,
            BUTTON_H,
            Component.translatable("pipboy.atomicdawn.camp.trust_name")
        );
        box.setMaxLength(16);
        box.setValue(previous);
        nameBox = screen.widget(box);
        screen.button(trustLabel, area.x() + 8 + boxW, y, trustW, BUTTON_H, true, () -> {
            if (nameBox != null && !nameBox.getValue().isBlank()) {
                ClientPacketDistributor.sendToServer(new CampTrustPayload(nameBox.getValue().trim()));
                nameBox.setValue("");
            }
        });
        y += ROW + 4;
        var removeLabel = Component.translatable("pipboy.atomicdawn.camp.untrust");
        var removeW = screen.font().width(removeLabel) + 10;
        for (var trusted : access.get().trusted()) {
            if (y + BUTTON_H > area.bottom()) {
                break;
            }
            screen.button(
                removeLabel,
                right - removeW,
                y,
                removeW,
                BUTTON_H,
                true,
                () -> CampClientState.send(CampActionPayload.Action.UNTRUST, trusted.id(), 0)
            );
            y += ROW;
        }
    }

    static void tick(PipBoyScreen screen) {
        if (CampClientState.revision() != revision) {
            screen.refresh();
        }
    }

    static void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect area) {
        var x = area.x() + 4;
        var y = area.y() + 4;
        var width = area.width() - 8;
        var state = CampClientState.get();
        if (state == null) {
            screen.text(graphics, Component.translatable("pipboy.atomicdawn.camp.syncing"), x, y, PipBoyTheme.dim());
            return;
        }
        var access = state.access();
        if (access.isEmpty()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.camp.access_none"),
                x,
                y,
                width,
                area.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var title = Component.translatable("pipboy.atomicdawn.camp.access_title");
        screen.text(graphics, title, x, y, PipBoyTheme.text());
        y = flagsTop(area);
        for (var flag : CampAccess.Flag.values()) {
            screen.text(graphics, Component.translatable(flag.translationKey()), x, y + 2, PipBoyTheme.text());
            y += ROW;
        }
        y = trustTop(area);
        screen.text(
            graphics,
            Component.translatable(
                "pipboy.atomicdawn.camp.trusted_header",
                access.get().trusted().size(),
                CampAccess.MAX_TRUSTED
            ),
            x,
            y,
            PipBoyTheme.text()
        );
        y += 12 + ROW + 4;
        if (access.get().trusted().isEmpty()) {
            var empty = Component.translatable("pipboy.atomicdawn.camp.trusted_empty");
            screen.text(graphics, empty, x, y + 2, PipBoyTheme.dim());
            return;
        }
        for (var trusted : access.get().trusted()) {
            if (y + BUTTON_H > area.bottom()) {
                break;
            }
            screen.text(graphics, Component.literal(trusted.name()), x, y + 2, PipBoyTheme.dim());
            y += ROW;
        }
    }
}

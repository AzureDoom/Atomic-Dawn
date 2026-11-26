package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.network.chat.Component;

import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;

abstract class EmptyStateTab implements PipBoyTab {

    private final String id;

    private final List<String> subtabs;

    EmptyStateTab(String id, List<String> subtabs) {
        this.id = id;
        this.subtabs = subtabs;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<String> subtabs() {
        return subtabs;
    }

    @Override
    public void init(PipBoyScreen screen, Rect area, String subtab) {}

    @Override
    public void render(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect area,
        String subtab,
        int mouseX,
        int mouseY
    ) {
        var message = Component.translatable("pipboy.atomicdawn." + id + "." + subtab + ".empty");
        var width = Math.min(area.width() - 20, 260);
        var x = area.x() + (area.width() - width) / 2;
        var y = area.y() + area.height() / 2 - 10;
        screen.wrapped(graphics, message, x, y, width, area.bottom(), PipBoyTheme.dim());
    }
}

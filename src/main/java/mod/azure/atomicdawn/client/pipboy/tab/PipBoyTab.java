package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.Rect;

public interface PipBoyTab {

    String id();

    List<String> subtabs();

    default Component label() {
        return Component.translatable("pipboy.atomicdawn.tab." + id());
    }

    default Component subtabLabel(String subtab) {
        return Component.translatable("pipboy.atomicdawn." + id() + "." + subtab.toLowerCase(Locale.ROOT));
    }

    void init(PipBoyScreen screen, Rect area, String subtab);

    void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect area, String subtab, int mouseX, int mouseY);

    default boolean scroll(PipBoyScreen screen, String subtab, int direction) {
        return false;
    }

    default void tick(PipBoyScreen screen, String subtab) {}

    default boolean keyPressed(PipBoyScreen screen, String subtab, int key) {
        return false;
    }

    default boolean back(PipBoyScreen screen, String subtab) {
        return false;
    }
}

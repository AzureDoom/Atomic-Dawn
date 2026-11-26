package mod.azure.atomicdawn.client.helper;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface ADHudLayer extends GuiLayer {

    @Override
    default void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        draw(ADGuiGraphics.of(graphics), deltaTracker);
    }

    void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker);
}

package mod.azure.atomicdawn.client.render.workbench;

import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;

public class TinkersBenchItemRenderer extends AzItemRenderer {

    public TinkersBenchItemRenderer() {
        super(
            AzItemRendererConfig.builder(TinkersBenchRenderer.MODEL, TinkersBenchRenderer.TEXTURE)
                .disableAnimationInAllContexts()
                .build()
        );
    }
}

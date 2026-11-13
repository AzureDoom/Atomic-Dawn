package mod.azure.atomicdawn.client.render.workbench;

import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;

public class ArmorBenchItemRenderer extends AzItemRenderer {

    public ArmorBenchItemRenderer() {
        super(
            AzItemRendererConfig.builder(ArmorBenchRenderer.MODEL, ArmorBenchRenderer.TEXTURE)
                .disableAnimationInAllContexts()
                .build()
        );
    }
}

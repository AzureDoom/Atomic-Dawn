package mod.azure.atomicdawn.client.render.workbench;

import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;

public class WorkbenchItemRenderer extends AzItemRenderer {

    public WorkbenchItemRenderer() {
        super(
            AzItemRendererConfig.builder(WorkbenchRenderer.MODEL, WorkbenchRenderer.TEXTURE)
                .disableAnimationInAllContexts()
                .build()
        );
    }
}

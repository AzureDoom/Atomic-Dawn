package mod.azure.atomicdawn.client.render.ammo;

import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;
import net.minecraft.resources.Identifier;

public class AmmoRenderer extends AzItemRenderer {

    public AmmoRenderer(Identifier itemId) {
        this(
            itemId.withPath(path -> "geo/item/" + path + ".geo.json"),
            itemId.withPath(path -> "textures/item/" + path + ".png")
        );
    }

    private AmmoRenderer(Identifier model, Identifier texture) {
        super(
            AzItemRendererConfig.builder(model, texture)
                .disableAnimationInAllContexts()
                .build()
        );
    }
}

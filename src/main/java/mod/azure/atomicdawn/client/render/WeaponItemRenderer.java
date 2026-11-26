package mod.azure.atomicdawn.client.render;

import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

import mod.azure.atomicdawn.client.animation.WeaponAnimator;

public class WeaponItemRenderer extends AzItemRenderer {

    public WeaponItemRenderer(Identifier itemId) {
        this(
            itemId.withPath(path -> "geo/item/" + path + ".geo.json"),
            itemId.withPath(path -> "textures/item/" + path + ".png"),
            itemId.withPath(path -> "animations/item/" + path + ".animation.json")
        );
    }

    private WeaponItemRenderer(Identifier model, Identifier texture, Identifier animations) {
        super(
            AzItemRendererConfig.builder(
                ignored -> model,
                stack -> WeaponModelParts.textureOverride(stack).orElse(texture)
            )
                .setAnimatorProvider(() -> new WeaponAnimator(animations))
                .useSeparateFirstPersonAnimator()
                .disableAnimationInContexts(ItemDisplayContext.GUI, ItemDisplayContext.FIXED, ItemDisplayContext.GROUND)
                .setBoneTextureOverrideProvider(PowerArmorArmBones::textureOverride)
                .setPrerenderEntry(context -> {
                    WeaponModelParts.applyVisibility(context.animatable(), context.bakedModel());
                    return context;
                })
                .build()
        );
    }
}

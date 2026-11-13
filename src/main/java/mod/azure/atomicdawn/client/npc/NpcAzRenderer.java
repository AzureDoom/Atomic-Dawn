package mod.azure.atomicdawn.client.npc;

import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.npc.NpcModelDefinition;
import mod.azure.atomicdawn.npc.NpcModels;

public class NpcAzRenderer extends AzEntityRenderer<NpcEntity> {

    private static final Identifier MISSING_GEO = AtomicDawn.id("geo/entity/npc/missing.geo.json");

    private static final Identifier MISSING_TEXTURE = AtomicDawn.id("textures/entity/npc/missing.png");

    public NpcAzRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(NpcAzRenderer::model, NpcAzRenderer::texture)
                .setAnimatorProvider(NpcAnimator::new)
                .build(),
            context
        );
    }

    private static Identifier model(NpcEntity npc) {
        return definition(npc) == null ? MISSING_GEO : definition(npc).geo();
    }

    private static Identifier texture(NpcEntity npc) {
        var definition = definition(npc);
        return definition == null ? MISSING_TEXTURE : definition.texture(NpcClient.appearance(npc).texture());
    }

    private static NpcModelDefinition definition(NpcEntity npc) {
        return NpcModels.get(npc.level().registryAccess(), NpcClient.appearance(npc).model()).orElse(null);
    }

    @Override
    protected boolean shouldShowName(@NotNull NpcEntity entity, double distanceToCameraSq) {
        return false;
    }
}

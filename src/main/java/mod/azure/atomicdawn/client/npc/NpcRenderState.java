package mod.azure.atomicdawn.client.npc;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jetbrains.annotations.Nullable;

public class NpcRenderState extends EntityRenderState {

    @Nullable
    public EntityRenderer<?, ?> delegate;

    @Nullable
    public EntityRenderState delegateState;

    public float modelScale = 1F;

    public boolean azure;

    public boolean sitting;
}

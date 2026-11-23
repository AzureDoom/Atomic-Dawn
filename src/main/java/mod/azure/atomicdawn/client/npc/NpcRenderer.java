package mod.azure.atomicdawn.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcAppearance;
import mod.azure.atomicdawn.npc.NpcBehavior;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.npc.NpcModels;

public class NpcRenderer extends EntityRenderer<NpcEntity, NpcRenderState> {

    private static final double SIT_OFFSET = -0.5;

    private final NpcAzRenderer azure;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.azure = new NpcAzRenderer(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public @NotNull NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(@NotNull NpcEntity npc, @NotNull NpcRenderState state, float partialTick) {
        super.extractRenderState(npc, state, partialTick);
        state.delegate = null;
        state.delegateState = null;
        state.azure = false;
        state.sitting = false;
        if (npc.isInvisible() || npc.isDowned()) {
            return;
        }
        var appearance = NpcClient.appearance(npc);
        state.modelScale = appearance.scale();
        try {
            if (
                appearance.kind() == NpcAppearance.Kind.AZURE
                    && NpcModels.get(npc.level().registryAccess(), appearance.model()).isPresent()
            ) {
                state.azure = true;
                state.delegate = azure;
                state.delegateState = capture(azure, npc, partialTick);
                return;
            }
            var puppet = NpcPuppets.get(npc, appearance);
            if (puppet == null) {
                return;
            }
            var renderer = entityRenderDispatcher.getRenderer(puppet);
            state.delegate = renderer;
            state.delegateState = capture(renderer, puppet, partialTick);
            applyPose(npc, state);
        } catch (RuntimeException exception) {
            NpcPuppets.markBroken(npc, appearance);
            state.delegate = null;
            state.delegateState = null;
            AtomicDawn.LOGGER.warn("Could not prepare NPC appearance {}", appearance.model(), exception);
        }
    }

    private static void applyPose(NpcEntity npc, NpcRenderState state) {
        var pose = NpcClient.pose(npc);
        var sitting = pose == NpcBehavior.NpcPose.SITTING;
        if (state.delegateState instanceof HumanoidRenderState humanoid) {
            humanoid.isCrouching = pose == NpcBehavior.NpcPose.CROUCHING;
            humanoid.isPassenger = sitting;
            state.sitting = sitting;
        } else if (state.delegateState instanceof IllagerRenderState illager) {
            illager.isRiding = sitting;
            state.sitting = sitting;
        }
    }

    private static <E extends Entity> EntityRenderState capture(
        EntityRenderer<? super E, ?> renderer,
        E entity,
        float partialTick
    ) {
        return renderer.createRenderState(entity, partialTick);
    }

    @Override
    public void submit(
        @NotNull NpcRenderState state,
        @NotNull PoseStack poseStack,
        @NotNull SubmitNodeCollector collector,
        @NotNull CameraRenderState camera
    ) {
        if (state.delegate != null && state.delegateState != null) {
            poseStack.pushPose();
            if (state.sitting) {
                poseStack.translate(0.0, SIT_OFFSET * state.modelScale, 0.0);
            }
            if (state.modelScale != 1F) {
                poseStack.scale(state.modelScale, state.modelScale, state.modelScale);
            }
            submitDelegate(state.delegate, state.delegateState, poseStack, collector, camera);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    @SuppressWarnings("unchecked")
    private static <S extends EntityRenderState> void submitDelegate(
        EntityRenderer<?, ?> renderer,
        EntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        ((EntityRenderer<?, S>) renderer).submit((S) state, poseStack, collector, camera);
    }
}

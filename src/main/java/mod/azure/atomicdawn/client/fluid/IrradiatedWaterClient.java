package mod.azure.atomicdawn.client.fluid;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.registry.ADFluids;

public final class IrradiatedWaterClient {

    private static final Identifier UNDERWATER = Identifier.withDefaultNamespace("textures/misc/underwater.png");

    public static final IClientFluidTypeExtensions EXTENSIONS = new IClientFluidTypeExtensions() {

        @Override
        public Identifier getRenderOverlayTexture(@NonNull Minecraft minecraft) {
            return UNDERWATER;
        }

        @Override
        public void modifyFogColor(
            @NonNull Camera camera,
            float partialTick,
            @NonNull ClientLevel level,
            int renderDistance,
            float darkenWorldAmount,
            Vector4f fluidFogColor
        ) {
            fluidFogColor.set(0.22F, 0.48F, 0.12F, fluidFogColor.w());
        }
    };

    private IrradiatedWaterClient() {}

    public static void registerModel(RegisterFluidModelsEvent event) {
        event.register(
            new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/water_still")),
                new Material(Identifier.withDefaultNamespace("block/water_flow")),
                new Material(Identifier.withDefaultNamespace("block/water_overlay")),
                FluidTintSources.constant(0xFF5BC236)
            ),
            ADFluids.IRRADIATED_WATER,
            ADFluids.FLOWING_IRRADIATED_WATER
        );
    }
}

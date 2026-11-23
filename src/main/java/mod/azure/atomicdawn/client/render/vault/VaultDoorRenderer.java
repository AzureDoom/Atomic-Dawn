package mod.azure.atomicdawn.client.render.vault;

import mod.azure.azurelib.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityRendererConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.vault.VaultDoorBlock;
import mod.azure.atomicdawn.vault.VaultDoorBlockEntity;

public class VaultDoorRenderer extends AzBlockEntityRenderer<VaultDoorBlockEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/block/vault_door.geo.json");

    public static final Identifier TEXTURE = AtomicDawn.id("textures/block/vault_door.png");

    public VaultDoorRenderer() {
        super(
            AzBlockEntityRendererConfig.<VaultDoorBlockEntity>builder(MODEL, TEXTURE)
                .setAnimatorProvider(VaultDoorAnimator::new)
                .build()
        );
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(VaultDoorBlockEntity door) {
        return VaultDoorBlock.renderBounds(door.getBlockPos());
    }
}

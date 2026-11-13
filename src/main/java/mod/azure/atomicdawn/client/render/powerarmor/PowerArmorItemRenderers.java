package mod.azure.atomicdawn.client.render.powerarmor;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorFirstPerson;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;

public final class PowerArmorItemRenderers {

    public static final Identifier FIST_MODEL = AtomicDawn.id("geo/item/power_armor_fist.geo.json");

    private PowerArmorItemRenderers() {}

    public static final class Station extends AzItemRenderer {

        public Station() {
            super(
                AzItemRendererConfig.builder(PowerArmorStationRenderer.MODEL, PowerArmorStationRenderer.TEXTURE)
                    .disableAnimationInAllContexts()
                    .build()
            );
        }
    }

    public static final class Fist extends AzItemRenderer {

        public Fist() {
            super(
                AzItemRendererConfig.builder(_ -> FIST_MODEL, _ -> currentTexture())
                    .disableAnimationInAllContexts()
                    .setPrerenderEntry(context -> {
                        applyVisibility(context.bakedModel());
                        return context;
                    })
                    .build()
            );
        }

        private static void applyVisibility(AzBakedModel model) {
            var player = Minecraft.getInstance().player;
            var armor = player != null && player.getVehicle() instanceof PowerArmorEntity entity ? entity : null;
            var mainArm = player == null ? HumanoidArm.RIGHT : player.getMainArm();
            for (var arm : HumanoidArm.values()) {
                var slot = PowerArmorFirstPerson.slot(arm);
                var frame = model.getBoneOrNull("fist_" + slot.getSerializedName());
                if (frame == null) {
                    continue;
                }
                frame.setHidden(arm != mainArm);
                var installed = armor != null && armor.getPiece(slot).getItem() instanceof PowerArmorPieceItem piece
                    ? piece.set()
                    : null;
                for (var set : PowerArmorModelParts.sets()) {
                    var bone = model.getBoneOrNull("fist_" + PowerArmorModelParts.bone(slot, set));
                    if (bone != null) {
                        bone.setHidden(!set.equals(installed));
                    }
                }
            }
        }

        private static Identifier currentTexture() {
            var player = Minecraft.getInstance().player;
            return PowerArmorTextures.texture(
                player != null && player.getVehicle() instanceof PowerArmorEntity armor ? armor : null
            );
        }
    }
}

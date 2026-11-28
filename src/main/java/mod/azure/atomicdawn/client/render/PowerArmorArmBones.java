package mod.azure.atomicdawn.client.render;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.client.powerarmor.PowerArmorFirstPerson;
import mod.azure.atomicdawn.client.render.powerarmor.PowerArmorModelParts;
import mod.azure.atomicdawn.client.render.powerarmor.PowerArmorTextures;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;

public final class PowerArmorArmBones {

    public static final String PREFIX = "pa_";

    private static final PowerArmorSlot[] ARM_SLOTS = { PowerArmorSlot.RIGHT_ARM, PowerArmorSlot.LEFT_ARM };

    private PowerArmorArmBones() {}

    public static boolean isArmorBone(AzBone bone) {
        return bone.getName().startsWith(PREFIX);
    }

    @Nullable
    public static Identifier textureOverride(AzBone bone) {
        return isArmorBone(bone) ? PowerArmorTextures.texture(PowerArmorFirstPerson.weaponArmor()) : null;
    }

    public static void apply(AzBakedModel model) {
        var armor = PowerArmorFirstPerson.consumeWeaponHand();
        for (var slot : ARM_SLOTS) {
            var frame = model.getBoneOrNull(PREFIX + slot.getSerializedName());
            if (frame == null) {
                continue;
            }
            frame.setHidden(armor == null);
            var installed = armor == null
                ? null
                : armor.getPiece(slot).getItem() instanceof PowerArmorPieceItem piece ? piece.set() : null;
            for (var set : PowerArmorModelParts.sets()) {
                var bone = model.getBoneOrNull(PREFIX + PowerArmorModelParts.bone(slot, set));
                if (bone != null) {
                    bone.setHidden(!set.equals(installed));
                }
            }
        }
    }
}

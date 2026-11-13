package mod.azure.atomicdawn.client.render.powerarmor;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;

public record PowerArmorRiderPose(
    Rotation torso,
    Rotation head,
    Rotation rightArm,
    Rotation leftArm,
    Rotation rightLeg,
    Rotation leftLeg
) {

    public record Rotation(
        float x,
        float y,
        float z
    ) {

        private static Rotation bone(AzBakedModel model, String name) {
            AzBone bone = model.getBoneOrNull(name);
            return bone == null
                ? new Rotation(0, 0, 0)
                : new Rotation(bone.getRotX(), bone.getRotY(), bone.getRotZ());
        }

        private void set(ModelPart part) {
            part.xRot = -x;
            part.yRot = y;
            part.zRot = z;
        }

        private Rotation plus(Rotation other) {
            return new Rotation(x + other.x, y + other.y, z + other.z);
        }
    }

    public static PowerArmorRiderPose capture(AzBakedModel model) {
        return new PowerArmorRiderPose(
            Rotation.bone(model, "torso"),
            Rotation.bone(model, "head"),
            Rotation.bone(model, "right_arm"),
            Rotation.bone(model, "left_arm"),
            Rotation.bone(model, "right_leg"),
            Rotation.bone(model, "left_leg")
        );
    }

    public void apply(PlayerModel model) {
        torso.set(model.body);
        torso.plus(head).set(model.head);
        torso.plus(rightArm).set(model.rightArm);
        torso.plus(leftArm).set(model.leftArm);
        rightLeg.set(model.rightLeg);
        leftLeg.set(model.leftLeg);
    }
}

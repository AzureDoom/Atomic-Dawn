package mod.azure.atomicdawn.client.render.powerarmor;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.render.AzRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;

public final class PowerArmorModelParts {

    public static final String CORE_BONE = "core";

    public static final String HATCH_BONE = "back_hatch";

    private static final List<String> FRAME_BONES = List.of(
        "root",
        "torso",
        "head",
        "right_arm",
        "left_arm",
        "right_leg",
        "left_leg"
    );

    private static Set<String> sets;

    private PowerArmorModelParts() {}

    public static String bone(PowerArmorSlot slot, String set) {
        return slot.getSerializedName() + "_" + set;
    }

    public static Set<String> sets() {
        if (sets == null) {
            var found = new TreeSet<String>();
            for (var item : BuiltInRegistries.ITEM) {
                if (item instanceof PowerArmorPieceItem piece) {
                    found.add(piece.set());
                }
            }
            sets = found;
        }
        return sets;
    }

    public static boolean isFirstPersonRider(PowerArmorEntity armor) {
        var minecraft = Minecraft.getInstance();
        return minecraft.player != null && armor.hasPassenger(minecraft.player)
            && minecraft.options.getCameraType().isFirstPerson();
    }

    public static void apply(PowerArmorEntity armor, AzBakedModel model) {
        var hideAll = !AzRenderContext.isGuiRender() && isFirstPersonRider(armor);
        for (var name : FRAME_BONES) {
            var bone = model.getBoneOrNull(name);
            if (bone != null) {
                bone.setHidden(hideAll);
            }
        }
        for (var slot : PowerArmorSlot.all()) {
            var installed = armor.getPiece(slot);
            var installedSet = installed.getItem() instanceof PowerArmorPieceItem piece ? piece.set() : null;
            for (var set : sets()) {
                var bone = model.getBoneOrNull(bone(slot, set));
                if (bone != null) {
                    bone.setHidden(hideAll || !set.equals(installedSet));
                }
            }
        }
        var hatch = model.getBoneOrNull(HATCH_BONE);
        if (hatch != null) {
            hatch.setHidden(hideAll || armor.getPiece(PowerArmorSlot.TORSO).isEmpty());
        }
        var core = model.getBoneOrNull(CORE_BONE);
        if (core != null) {
            core.setHidden(hideAll || armor.getCore().isEmpty());
        }
    }
}

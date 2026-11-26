package mod.azure.atomicdawn.armor;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import mod.azure.atomicdawn.helper.ADRegistryUtil;

@SuppressWarnings("unused")
public enum ArmorPiece implements StringRepresentable {

    HELMET(ArmorType.HELMET, EquipmentSlot.HEAD, "head_armor", 5, 0.25F),
    CHESTPLATE(ArmorType.CHESTPLATE, EquipmentSlot.CHEST, "chest_armor", 8, 0.35F),
    LEGGINGS(ArmorType.LEGGINGS, EquipmentSlot.LEGS, "leg_armor", 7, 0.25F),
    BOOTS(ArmorType.BOOTS, EquipmentSlot.FEET, "foot_armor", 4, 0.15F);

    public static final Codec<ArmorPiece> CODEC = StringRepresentable.fromEnum(ArmorPiece::values);

    public static final int COUNT = values().length;

    private static final ArmorPiece[] VALUES = values();

    private final ArmorType type;

    private final EquipmentSlot slot;

    private final TagKey<Item> tag;

    private final int units;

    private final float weightShare;

    ArmorPiece(ArmorType type, EquipmentSlot slot, String tag, int units, float weightShare) {
        this.type = type;
        this.slot = slot;
        this.tag = TagKey.create(Registries.ITEM, ADRegistryUtil.vanillaId(tag));
        this.units = units;
        this.weightShare = weightShare;
    }

    public static ArmorPiece[] all() {
        return VALUES;
    }

    public ArmorType type() {
        return type;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public TagKey<Item> tag() {
        return tag;
    }

    public int units() {
        return units;
    }

    public float weightShare() {
        return weightShare;
    }

    public String translationKey() {
        return "armor.atomicdawn.piece." + getSerializedName();
    }

    @Nullable
    public static ArmorPiece of(EquipmentSlot slot) {
        for (var piece : VALUES) {
            if (piece.slot == slot) {
                return piece;
            }
        }
        return null;
    }

    @Nullable
    public static ArmorPiece of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof ArmorPieceItem item) {
            return item.piece();
        }
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return null;
        }
        var piece = of(equippable.slot());
        return piece != null && stack.is(piece.tag) ? piece : null;
    }

    public static boolean isArmor(ItemStack stack) {
        return of(stack) != null;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

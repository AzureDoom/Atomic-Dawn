package mod.azure.atomicdawn.pipboy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ammunition.AmmoItem;
import mod.azure.atomicdawn.item.WeaponItem;

@SuppressWarnings("unused")
public enum PipBoyCategory {

    WEAPONS,
    APPAREL,
    AID,
    MISC,
    AMMO,
    JUNK,
    FAVORITES;

    private final TagKey<Item> tag = TagKey.create(
        Registries.ITEM,
        AtomicDawn.id("pipboy/" + name().toLowerCase(Locale.ROOT))
    );

    public TagKey<Item> tag() {
        return tag;
    }

    public String translationKey() {
        return "pipboy.atomicdawn.inv." + name().toLowerCase(Locale.ROOT);
    }

    public static PipBoyCategory classify(ItemStack stack) {
        for (var category : values()) {
            if (category != FAVORITES && stack.is(category.tag)) {
                return category;
            }
        }
        var item = stack.getItem();
        if (
            item instanceof WeaponItem || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT) || stack.is(Items.MACE)
        ) {
            return WEAPONS;
        }
        if (item instanceof AmmoItem || stack.is(ItemTags.ARROWS)) {
            return AMMO;
        }
        if (stack.has(DataComponents.EQUIPPABLE)) {
            return APPAREL;
        }
        if (
            stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS)
                || stack.has(DataComponents.CONSUMABLE)
        ) {
            return AID;
        }
        return MISC;
    }
}

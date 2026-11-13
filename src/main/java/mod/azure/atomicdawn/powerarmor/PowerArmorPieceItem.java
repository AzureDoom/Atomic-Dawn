package mod.azure.atomicdawn.powerarmor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import mod.azure.atomicdawn.registry.ADDataComponents;

public class PowerArmorPieceItem extends Item {

    public static final int NO_PAINT = -1;

    private final PowerArmorSlot slot;

    private final String set;

    public PowerArmorPieceItem(Properties properties, PowerArmorSlot slot, String set) {
        super(properties);
        this.slot = slot;
        this.set = set;
    }

    public PowerArmorSlot slot() {
        return slot;
    }

    public String set() {
        return set;
    }

    public static int paint(ItemStack stack) {
        var paint = stack.get(ADDataComponents.POWER_ARMOR_PAINT.get());
        return paint == null ? NO_PAINT : paint;
    }

    public static float condition(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 1F;
        }
        return 1F - stack.getDamageValue() / (float) stack.getMaxDamage();
    }

    public static boolean isBroken(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }
}

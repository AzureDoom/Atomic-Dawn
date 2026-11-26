package mod.azure.atomicdawn.powerarmor;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADDataComponents;

public class PowerCoreItem extends Item {

    public PowerCoreItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(1);
    }

    public static int capacity() {
        return AtomicDawn.CONFIG.common.equipment.powerArmor.fusionCores.FUSION_CORE_CAPACITY;
    }

    public static int charge(ItemStack stack) {
        if (!(stack.getItem() instanceof PowerCoreItem)) {
            return 0;
        }
        var charge = stack.get(ADDataComponents.CORE_CHARGE.get());
        return charge == null ? capacity() : Mth.clamp(charge, 0, capacity());
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.set(ADDataComponents.CORE_CHARGE.get(), Mth.clamp(charge, 0, capacity()));
    }

    public static float fraction(ItemStack stack) {
        return capacity() <= 0 ? 0F : charge(stack) / (float) capacity();
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return charge(stack) < capacity();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13F * fraction(stack));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(fraction(stack) / 3F, 1F, 1F);
    }
}

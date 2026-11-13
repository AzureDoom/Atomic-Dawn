package mod.azure.atomicdawn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.registry.ADMobEffects;

public class RadXItem extends Item {

    public RadXItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (!level.isClientSide()) {
            entity.addEffect(new MobEffectInstance(ADMobEffects.RAD_RESISTANCE, 20 * 180, 0));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}

package mod.azure.atomicdawn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleSupplier;

import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.thirst.ThirstService;

public class WaterItem extends Item {

    private final DoubleSupplier thirst;

    private final DoubleSupplier rads;

    public WaterItem(Properties properties, DoubleSupplier thirst, DoubleSupplier rads) {
        super(properties);
        this.thirst = thirst;
        this.rads = rads;
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16)
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
            .usingConvertsTo(Items.GLASS_BOTTLE);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (!level.isClientSide()) {
            var amount = (float) rads.getAsDouble();
            if (amount > 0F) {
                RadiationService.expose(entity, amount);
            }
            if (entity instanceof Player player) {
                ThirstService.drink(player, thirst.getAsDouble());
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}

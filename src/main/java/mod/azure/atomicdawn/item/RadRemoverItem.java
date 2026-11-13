package mod.azure.atomicdawn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleSupplier;

import mod.azure.atomicdawn.radiation.RadiationService;

public class RadRemoverItem extends Item {

    private final DoubleSupplier rads;

    public RadRemoverItem(Properties properties, DoubleSupplier rads) {
        super(properties);
        this.rads = rads;
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (!level.isClientSide() && entity instanceof Player player) {
            RadiationService.remove(player, (float) rads.getAsDouble());
        }
        return super.finishUsingItem(stack, level, entity);
    }
}

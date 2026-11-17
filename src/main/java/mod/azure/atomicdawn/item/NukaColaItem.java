package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class NukaColaItem extends Item {

    private final NukaCola variant;

    public NukaColaItem(Properties properties, NukaCola variant) {
        super(properties);
        this.variant = variant;
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK);
    }

    public NukaCola variant() {
        return variant;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            NukaColaEffects.apply(player, variant);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull TooltipDisplay display,
        @NotNull Consumer<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable(variant.descriptionKey()).withStyle(ChatFormatting.GRAY));
    }
}

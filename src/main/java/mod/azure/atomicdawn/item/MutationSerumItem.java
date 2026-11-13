package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import mod.azure.atomicdawn.mutation.Mutation;
import mod.azure.atomicdawn.mutation.MutationService;

public class MutationSerumItem extends Item {

    private final Mutation mutation;

    public MutationSerumItem(Properties properties, Mutation mutation) {
        super(properties);
        this.mutation = mutation;
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16)
            .rarity(Rarity.UNCOMMON)
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK);
    }

    public Mutation mutation() {
        return mutation;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (entity instanceof ServerPlayer player && !MutationService.applySerum(player, mutation)) {

            return stack;
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
        tooltip.accept(Component.translatable(mutation.translationKey() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}

package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;

public class CampPlanItem extends Item {

    public CampPlanItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16).rarity(Rarity.UNCOMMON);
    }

    public static ItemStack stack(Identifier plan) {
        var stack = new ItemStack(ADItems.CAMP_PLAN.get());
        stack.set(ADDataComponents.CAMP_PLAN.get(), plan);
        return stack;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var plan = stack.get(ADDataComponents.CAMP_PLAN.get());
        if (plan == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.atomicdawn.camp_plan.named", CampPlans.displayName(plan));
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull TooltipDisplay display,
        @NotNull Consumer<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.atomicdawn.camp_plan.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var plan = stack.get(ADDataComponents.CAMP_PLAN.get());
        if (plan == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (CampPlans.learn(serverPlayer, plan)) {
                stack.consume(1, player);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

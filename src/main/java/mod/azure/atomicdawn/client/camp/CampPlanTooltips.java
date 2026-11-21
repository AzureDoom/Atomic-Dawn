package mod.azure.atomicdawn.client.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class CampPlanTooltips {

    private CampPlanTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!stack.is(ADItems.CAMP_PLAN.get())) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var plan = stack.get(ADDataComponents.CAMP_PLAN.get());
        if (plan == null) {
            event.getToolTip()
                .add(Component.translatable("item.atomicdawn.camp_plan.random").withStyle(ChatFormatting.YELLOW));
            return;
        }
        var known = CampPlans.of(player).has(plan);
        event.getToolTip()
            .add(
                known
                    ? Component.translatable("item.atomicdawn.camp_plan.known").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("item.atomicdawn.camp_plan.unknown").withStyle(ChatFormatting.GOLD)
            );
    }
}

package mod.azure.atomicdawn.client.render.brahmin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADDataComponents;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class BrahminTooltips {

    private BrahminTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().has(ADDataComponents.IRRADIATED.get())) {
            event.getToolTip()
                .add(Component.translatable("tooltip.atomicdawn.irradiated").withStyle(ChatFormatting.GREEN));
        }
    }
}

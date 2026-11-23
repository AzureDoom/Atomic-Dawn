package mod.azure.atomicdawn.junk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ScrapboxItem extends BlockItem {

    public ScrapboxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull TooltipDisplay display,
        @NotNull Consumer<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.accept(
            Component.translatable("item.atomicdawn.scrapbox.joke")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
        );
    }
}

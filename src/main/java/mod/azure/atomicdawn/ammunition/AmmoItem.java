package mod.azure.atomicdawn.ammunition;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import mod.azure.atomicdawn.registry.ADDataComponents;

public class AmmoItem extends Item {

    private final int roundsPerItem;

    public AmmoItem(Properties properties) {
        this(properties, 1);
    }

    public AmmoItem(Properties properties, int roundsPerItem) {
        super(properties);
        this.roundsPerItem = Math.max(1, roundsPerItem);
    }

    public int roundsPerItem() {
        return roundsPerItem;
    }

    public int roundsInBox(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ADDataComponents.AMMO_REMAINING.get(), roundsPerItem), 1, roundsPerItem);
    }

    public boolean isPartial(ItemStack stack) {
        return roundsPerItem > 1 && roundsInBox(stack) < roundsPerItem;
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull TooltipDisplay display,
        @NotNull Consumer<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        if (roundsPerItem <= 1) {
            return;
        }
        var rounds = roundsInBox(stack) * stack.getCount();
        var capacity = roundsPerItem * stack.getCount();
        tooltip.accept(
            Component.translatable("item.atomicdawn.ammo.rounds", rounds, capacity)
                .withStyle(rounds < capacity ? ChatFormatting.YELLOW : ChatFormatting.GRAY)
        );
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return isPartial(stack);
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13F * roundsInBox(stack) / roundsPerItem);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(roundsInBox(stack) / (float) roundsPerItem / 3F, 1F, 1F);
    }
}

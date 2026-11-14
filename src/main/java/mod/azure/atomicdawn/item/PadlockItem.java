package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import mod.azure.atomicdawn.lock.LockLevel;

public class PadlockItem extends Item {

    private final LockLevel level;

    public PadlockItem(Properties properties, LockLevel level) {
        super(properties);
        this.level = level;
    }

    public static Properties properties(Properties properties, LockLevel level) {
        var rarity = switch (level) {
            case NOVICE, ADVANCED -> Rarity.COMMON;
            case EXPERT -> Rarity.UNCOMMON;
            case MASTER -> Rarity.RARE;
        };
        return properties.stacksTo(16).rarity(rarity);
    }

    public LockLevel level() {
        return level;
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
            Component.translatable("item.atomicdawn.padlock.level", level.component()).withStyle(ChatFormatting.GRAY)
        );
        if (level.requiredTier() > 0) {
            tooltip.accept(
                Component.translatable("item.atomicdawn.padlock.tier", level.requiredTier())
                    .withStyle(ChatFormatting.DARK_GRAY)
            );
        }
        tooltip.accept(Component.translatable("item.atomicdawn.padlock.desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.atomicdawn.padlock.remove").withStyle(ChatFormatting.DARK_GRAY));
    }
}

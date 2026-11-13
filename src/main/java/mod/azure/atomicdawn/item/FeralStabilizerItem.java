package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import mod.azure.atomicdawn.ghoul.GhoulService;

public class FeralStabilizerItem extends Item {

    public FeralStabilizerItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16)
            .rarity(Rarity.UNCOMMON)
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!GhoulService.isGhoul(player)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("ghoul.atomicdawn.stabilizer.not_ghoul").withStyle(ChatFormatting.RED),
                    true
                );
            }
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (entity instanceof ServerPlayer player) {
            if (!GhoulService.restoreStability(player, GhoulService.stabilizerAmount(player))) {
                return stack;
            }
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
        tooltip.accept(Component.translatable("item.atomicdawn.feral_stabilizer.desc").withStyle(ChatFormatting.GRAY));
    }
}

package mod.azure.atomicdawn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.mutation.MutationService;
import mod.azure.atomicdawn.quest.QuestActions;
import mod.azure.atomicdawn.radiation.RadiationService;

public class RadAwayItem extends Item {

    public RadAwayItem(Properties properties) {
        super(properties);
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
            RadiationService.remove(
                player,
                (float) AtomicDawn.CONFIG.common.survival.radiation.consumables.RADAWAY_RADS
            );
            if (player instanceof ServerPlayer serverPlayer) {
                MutationService.onRadAway(serverPlayer);
                QuestActions.trigger(serverPlayer, QuestActions.USE_RADAWAY);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}

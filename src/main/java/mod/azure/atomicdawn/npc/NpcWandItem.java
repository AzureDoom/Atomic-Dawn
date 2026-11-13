package mod.azure.atomicdawn.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class NpcWandItem extends Item {

    public NpcWandItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(1).rarity(Rarity.EPIC);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        if (
            !(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)
        ) {
            return InteractionResult.SUCCESS;
        }
        if (!NpcService.requireEditor(player)) {
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive()) {
            NpcService.openPicker(player);
            return InteractionResult.SUCCESS;
        }
        var spawn = context.getClickedPos().relative(context.getClickedFace());
        var yaw = player.getYRot() + 180F;
        var npc = NpcService.spawn(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, yaw, NpcConfig.DEFAULT);
        if (npc == null) {
            return InteractionResult.FAIL;
        }
        NpcService.openEditor(player, npc);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            NpcService.openPicker(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(
        @NotNull ItemStack stack,
        @NotNull Player player,
        @NotNull LivingEntity target,
        @NotNull InteractionHand hand
    ) {
        if (!(target instanceof NpcEntity npc)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NpcService.openEditor(serverPlayer, npc);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull TooltipDisplay display,
        @NotNull Consumer<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.atomicdawn.npc_wand.desc").withStyle(ChatFormatting.GRAY));
    }
}

package mod.azure.atomicdawn.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.holotape.HolotapeService;
import mod.azure.atomicdawn.registry.ADDataComponents;

public class HolotapeItem extends Item {

    public HolotapeItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(1).rarity(Rarity.UNCOMMON);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var id = stack.get(ADDataComponents.HOLOTAPE.get());
        if (id == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.atomicdawn.holotape.named", PipBoyNames.holotape(id));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var id = player.getItemInHand(hand).get(ADDataComponents.HOLOTAPE.get());
        if (id == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            HolotapeService.play(serverPlayer, id);
        }
        return InteractionResult.SUCCESS;
    }
}

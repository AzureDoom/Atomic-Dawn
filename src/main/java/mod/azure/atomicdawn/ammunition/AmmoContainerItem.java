package mod.azure.atomicdawn.ammunition;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.crafting.AmmoContainerMenu;
import mod.azure.atomicdawn.registry.ADItems;

public class AmmoContainerItem extends Item {

    public AmmoContainerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull InteractionResult use(Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 40;
            if (player.getInventory().getItem(slot).is(ADItems.AMMO_CONTAINER.get())) {
                serverPlayer.openMenu(
                    new SimpleMenuProvider(
                        (id, inventory, ignored) -> new AmmoContainerMenu(id, inventory, slot),
                        Component.translatable("container.atomicdawn.ammo_container")
                    ),
                    data -> data.writeVarInt(slot)
                );
            }
        }
        return InteractionResult.SUCCESS;
    }
}

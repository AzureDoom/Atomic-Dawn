package mod.azure.atomicdawn.ammunition;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADItems;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class AmmoContainerEvents {

    private static final int SWEEP_TICKS = 40;

    private static final Set<UUID> PENDING = new HashSet<>();

    private AmmoContainerEvents() {}

    public static void markDirty(ServerPlayer player) {
        PENDING.add(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            PENDING.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % SWEEP_TICKS == 0) {
            PENDING.add(player.getUUID());
        }
        if (!PENDING.contains(player.getUUID())) {
            return;
        }
        if (player.containerMenu instanceof mod.azure.atomicdawn.crafting.AmmoContainerMenu) {
            return;
        }
        PENDING.remove(player.getUUID());
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var box = inventory.getItem(slot);
            if (!box.is(ADItems.AMMO_CONTAINER.get())) {
                continue;
            }
            var contents = AmmoContainerStorage.read(box);
            boolean changed = false;
            for (int source = 0; source < inventory.getContainerSize(); source++) {
                var ammo = inventory.getItem(source);
                if (!(ammo.getItem() instanceof AmmoItem)) {
                    continue;
                }
                if (AmmoContainerStorage.insert(contents, ammo) > 0) {
                    changed = true;
                }
            }
            if (changed) {
                AmmoContainerStorage.write(box, contents);
                inventory.setChanged();
            }
        }
        AmmoInventory.consolidate(player);
    }
}

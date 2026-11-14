package mod.azure.atomicdawn.lock;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.command.LockCommands;
import mod.azure.atomicdawn.item.PadlockItem;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class LockEvents {

    private LockEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !LockService.enabled()) {
            return;
        }
        var level = player.level();
        var pos = event.getPos();
        var stack = player.getItemInHand(event.getHand());
        var located = LockService.find(level, pos);
        if (located.isEmpty()) {
            if (
                stack.getItem() instanceof PadlockItem padlock
                    && LockService.lockable(level, pos, level.getBlockState(pos))
            ) {
                LockService.install(player, pos, stack, padlock.level());
                cancel(event, InteractionResult.SUCCESS);
            }
            return;
        }
        var lock = located.get().lock();
        if (
            event.getHand() == InteractionHand.MAIN_HAND && player.isSecondaryUseActive()
                && player.getMainHandItem().isEmpty() && LockService.canRemove(player, lock)
        ) {
            LockService.removeByHand(player, located.get());
            cancel(event, InteractionResult.SUCCESS);
            return;
        }
        if (stack.getItem() instanceof PadlockItem) {
            LockService.notify(player, "lock.atomicdawn.already_locked", ChatFormatting.RED);
            cancel(event, InteractionResult.FAIL);
            return;
        }
        if (LockService.canOpen(player, lock)) {
            return;
        }
        cancel(event, InteractionResult.FAIL);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            LockpickService.begin(player, located.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!LockService.enabled() || !AtomicDawn.CONFIG.locks.LOCKS_PROTECT_BLOCKS) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var pos = event.getPosition();
        if (pos.isEmpty()) {
            return;
        }
        var located = LockService.find(player.level(), pos.get());
        if (located.isEmpty() || LockService.canRemove(player, located.get().lock())) {
            return;
        }
        event.setCanceled(true);
        if (player.tickCount % 20 == 0) {
            LockService.notify(player, "lock.atomicdawn.protected", ChatFormatting.RED);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots()) {
                LockService.put(level, snapshot.getPos(), null);
            }
            return;
        }
        LockService.put(level, event.getPos(), null);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LockpickService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LockpickService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LockCommands.register(event.getDispatcher());
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}

package mod.azure.atomicdawn.thirst;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADItems;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class ThirstEvents {

    public static final TagKey<Item> POTIONS = tag("thirst/potions");

    public static final TagKey<Item> MILK = tag("thirst/milk");

    public static final TagKey<Item> HONEY = tag("thirst/honey");

    public static final TagKey<Item> STEWS = tag("thirst/stews");

    public static final TagKey<Item> SOFT_DRINKS = tag("thirst/soft_drinks");

    private ThirstEvents() {}

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, AtomicDawn.id(name));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSprinting() && ThirstService.dehydrated(player)) {
            player.setSprinting(false);
        }
        if (player.tickCount % 20 == 0) {
            ThirstService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ThirstService.drink(player, restored(event.getItem()));
    }

    private static double restored(ItemStack stack) {
        var config = AtomicDawn.CONFIG.thirst;
        if (stack.is(Items.POTION)) {
            var contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                return config.WATER_BOTTLE_THIRST;
            }
        }
        if (stack.is(POTIONS)) {
            return config.POTION_THIRST;
        }
        if (stack.is(MILK)) {
            return config.MILK_THIRST;
        }
        if (stack.is(HONEY)) {
            return config.HONEY_THIRST;
        }
        if (stack.is(STEWS)) {
            return config.STEW_THIRST;
        }
        if (stack.is(SOFT_DRINKS)) {
            return config.SOFT_DRINK_THIRST;
        }
        return 0.0;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return;
        }
        var player = event.getEntity();
        var level = event.getLevel();
        var hit = player.pick(player.blockInteractionRange(), 1F, true);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return;
        }
        var pos = blockHit.getBlockPos();
        var fluid = level.getFluidState(pos);
        if (!fluid.isSource() || !DirtyWater.isDirty(level, pos, fluid)) {
            return;
        }
        if (!level.isClientSide()) {
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.BOTTLE_FILL,
                SoundSource.NEUTRAL,
                1F,
                1F
            );
            player.setItemInHand(
                event.getHand(),
                ItemUtils.createFilledResult(stack, player, new ItemStack(ADItems.DIRTY_WATER.get()))
            );
            RadiationService.expose(player, (float) AtomicDawn.CONFIG.radiation.DIRTY_WATER_COLLECT_RADS);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}

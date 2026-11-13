package mod.azure.atomicdawn.powerarmor;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import mod.azure.atomicdawn.registry.ADEntities;
import mod.azure.atomicdawn.registry.ADItems;

public class PowerArmorChassisItem extends Item {

    public PowerArmorChassisItem(Properties properties) {
        super(properties);
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(1);
    }

    public static ItemStack fromEntity(PowerArmorEntity entity) {
        var stack = new ItemStack(ADItems.POWER_ARMOR_CHASSIS.get());
        var items = new ArrayList<ItemStack>();
        for (var i = 0; i < PowerArmorEntity.SLOT_COUNT; i++) {
            items.add(entity.getSlotItem(i).copy());
        }
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        if (entity.hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, entity.getCustomName());
        }
        return stack;
    }

    public static void applyContents(PowerArmorEntity entity, ItemStack stack) {
        var contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            var items = NonNullList.withSize(PowerArmorEntity.SLOT_COUNT, ItemStack.EMPTY);
            contents.copyInto(items);
            for (var i = 0; i < PowerArmorEntity.SLOT_COUNT; i++) {
                if (PowerArmorEntity.accepts(i, items.get(i))) {
                    entity.setSlotItem(i, items.get(i));
                }
            }
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            entity.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
        }
    }

    public static PowerArmorEntity create(ServerLevel level, ItemStack stack) {
        var entity = ADEntities.POWER_ARMOR.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity != null) {
            applyContents(entity, stack);
        }
        return entity;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var level = context.getLevel();
        var clicked = context.getClickedPos();
        var stack = context.getItemInHand();
        var player = context.getPlayer();
        if (level.getBlockState(clicked).getBlock() instanceof PowerArmorStationBlock) {
            if (level instanceof ServerLevel serverLevel && player != null) {
                return PowerArmorStationBlock.dockItem(serverLevel, clicked, stack, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        var entity = create(serverLevel, stack);
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        var spawn = clicked.relative(context.getClickedFace());
        entity.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        var yaw = player == null ? 0F : player.getYRot();
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.yBodyRot = yaw;
        if (!serverLevel.noCollision(entity)) {
            return InteractionResult.FAIL;
        }
        serverLevel.addFreshEntity(entity);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}

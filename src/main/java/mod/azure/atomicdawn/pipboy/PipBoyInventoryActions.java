package mod.azure.atomicdawn.pipboy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.powerarmor.PowerArmorChassisItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;
import mod.azure.atomicdawn.util.PlayerItems;

public final class PipBoyInventoryActions {

    public static final int OFFHAND_SLOT = 40;

    private PipBoyInventoryActions() {}

    public static boolean isArmorSlot(int slot) {
        return slot >= Inventory.INVENTORY_SIZE && slot < OFFHAND_SLOT;
    }

    @Nullable
    public static EquipmentSlot armorSlot(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return null;
        }
        return equippable.slot();
    }

    public static boolean usable(ItemStack stack) {
        return stack.has(DataComponents.CONSUMABLE);
    }

    @Nullable
    private static ItemStack stackAt(ServerPlayer player, int slot, Identifier item) {
        var inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return null;
        }
        var stack = inventory.getItem(slot);
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item)) {
            return null;
        }
        return stack;
    }

    private static void fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }

    private static boolean bound(ServerPlayer player, ItemStack stack) {
        return !player.isCreative() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    public static void use(ServerPlayer player, int slot, Identifier item) {
        var stack = stackAt(player, slot, item);
        if (stack == null || !usable(stack) || isArmorSlot(slot)) {
            return;
        }
        var food = stack.get(DataComponents.FOOD);
        if (food != null && !player.canEat(food.canAlwaysEat())) {
            fail(player, "pipboy.atomicdawn.inv.not_hungry");
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        var original = stack.copy();
        var result = stack.finishUsingItem(player.level(), player);
        result = EventHooks.onItemUseFinish(player, original, 0, result);
        player.getInventory().setItem(slot, result);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static void equip(ServerPlayer player, int slot, Identifier item) {
        var stack = stackAt(player, slot, item);
        if (stack == null || isArmorSlot(slot)) {
            return;
        }
        if (player.getVehicle() instanceof PowerArmorEntity) {
            fail(player, "pipboy.atomicdawn.inv.in_power_armor");
            return;
        }
        var target = armorSlot(stack);
        if (target == null) {
            return;
        }
        var worn = player.getItemBySlot(target);
        if (!worn.isEmpty() && bound(player, worn)) {
            fail(player, "pipboy.atomicdawn.inv.bound");
            return;
        }
        var equipping = stack.split(1);
        player.setItemSlot(target, equipping);
        if (stack.isEmpty()) {
            player.getInventory().setItem(slot, worn);
        } else {
            PlayerItems.give(player, worn);
        }
        var equippable = equipping.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            player.level()
                .playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    equippable.equipSound(),
                    SoundSource.PLAYERS,
                    1F,
                    1F
                );
        }
        player.containerMenu.broadcastChanges();
    }

    public static void unequip(ServerPlayer player, int slot, Identifier item) {
        var stack = stackAt(player, slot, item);
        if (stack == null || !isArmorSlot(slot)) {
            return;
        }
        var target = armorSlot(stack);
        if (target == null || player.getItemBySlot(target) != stack) {
            return;
        }
        if (bound(player, stack)) {
            fail(player, "pipboy.atomicdawn.inv.bound");
            return;
        }
        player.setItemSlot(target, ItemStack.EMPTY);
        PlayerItems.give(player, stack);
        player.containerMenu.broadcastChanges();
    }

    public static void hold(ServerPlayer player, int slot, Identifier item) {
        var stack = stackAt(player, slot, item);
        var inventory = player.getInventory();
        var selected = inventory.getSelectedSlot();
        if (stack == null || isArmorSlot(slot) || slot == selected) {
            return;
        }
        var held = inventory.getItem(selected);
        inventory.setItem(selected, stack);
        inventory.setItem(slot, held);
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static void equipPowerArmorPiece(ServerPlayer player, int slot, Identifier item) {
        var stack = stackAt(player, slot, item);
        if (stack == null || !(player.getVehicle() instanceof PowerArmorEntity armor)) {
            return;
        }
        if (!(stack.getItem() instanceof PowerArmorPieceItem piece)) {
            return;
        }
        var index = piece.slot().ordinal();
        var current = armor.getSlotItem(index).copy();
        armor.setSlotItem(index, stack.split(1));
        if (stack.isEmpty()) {
            player.getInventory().setItem(slot, current);
        } else {
            PlayerItems.give(player, current);
        }
        player.containerMenu.broadcastChanges();
    }

    public static void unequipPowerArmorPiece(ServerPlayer player, int pieceIndex) {
        if (!(player.getVehicle() instanceof PowerArmorEntity armor)) {
            return;
        }
        if (pieceIndex < 0 || pieceIndex >= PowerArmorSlot.COUNT) {
            return;
        }
        var current = armor.getSlotItem(pieceIndex);
        if (current.isEmpty()) {
            return;
        }
        var removed = current.copy();
        armor.setSlotItem(pieceIndex, ItemStack.EMPTY);
        PlayerItems.give(player, removed);
        player.containerMenu.broadcastChanges();
    }

    public static void deployChassis(ServerPlayer player, int slot, Identifier item) {
        var stack = stackAt(player, slot, item);
        if (
            stack == null || !(stack.getItem() instanceof PowerArmorChassisItem) || player.isPassenger()
                || !(player.level() instanceof ServerLevel level)
        ) {
            return;
        }
        if (PowerArmorEntity.hasBoundArmor(player)) {
            fail(player, "power_armor.atomicdawn.bound_armor");
            return;
        }
        var entity = PowerArmorChassisItem.create(level, stack);
        if (entity == null) {
            return;
        }
        var yaw = player.getYRot();
        entity.setPos(player.getX(), player.getY(), player.getZ());
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.yBodyRot = yaw;
        if (!level.noCollision(entity)) {
            fail(player, "pipboy.atomicdawn.inv.no_room");
            return;
        }
        level.addFreshEntity(entity);
        stack.shrink(1);
        player.containerMenu.broadcastChanges();
        entity.enter(player);
    }
}

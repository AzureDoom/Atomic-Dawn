package mod.azure.atomicdawn.powerarmor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.crafting.WorkbenchService;
import mod.azure.atomicdawn.network.PowerArmorActionPayload;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorLegendary;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorMod;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgrades;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.util.RepairKits;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;

public final class PowerArmorService {

    public static final TagKey<Item> REPAIR_MATERIALS = TagKey.create(
        Registries.ITEM,
        AtomicDawn.id("power_armor_repair")
    );

    private PowerArmorService() {}

    public static void handle(ServerPlayer player, PowerArmorActionPayload payload) {
        if (payload.action() == PowerArmorActionPayload.Action.EXIT) {
            if (player.getVehicle() instanceof PowerArmorEntity armor && armor.getId() == payload.entityId()) {
                armor.exit(player);
            }
            return;
        }
        if (payload.action() == PowerArmorActionPayload.Action.SET_FLYING) {
            if (player.getVehicle() instanceof PowerArmorEntity armor && armor.getId() == payload.entityId()) {
                armor.setFlying(payload.slot() != 0);
            }
            return;
        }
        if (!(player.containerMenu instanceof PowerArmorStationMenu menu)) {
            return;
        }
        var armor = menu.armor();
        if (armor == null || armor.getId() != payload.entityId() || !menu.stillValid(player)) {
            return;
        }
        switch (payload.action()) {
            case REPAIR -> repair(player, armor, payload.slot());
            case REPAIR_KIT -> repairWithKit(player, armor, payload.slot());
            case PAINT -> paint(player, armor, menu, payload.slot());
            case CLEAR_PAINT -> clearPaint(armor, payload.slot());
            case FABRICATE -> fabricate(player, ResourceKey.create(ADRegistries.POWER_ARMOR_RECIPE, payload.recipe()));
            case SCRAP -> scrap(player, payload.slot());
            case INSTALL_MOD -> installMod(player, armor, payload.slot(), payload.recipe());
            case APPLY_LEGENDARY -> applyLegendary(player, armor, payload.slot(), payload.recipe());
            case REMOVE_MOD -> removeMod(player, armor, payload.slot(), payload.recipe());
        }
    }

    public static List<ItemStack> scrapYield(RegistryAccess access, ItemStack stack) {
        if (!(stack.getItem() instanceof PowerArmorPieceItem)) {
            return List.of();
        }
        var fraction = (float) AtomicDawn.CONFIG.powerArmor.POWER_ARMOR_SCRAP_RETURN
            * Math.max(0.2F, PowerArmorPieceItem.condition(stack));
        var result = new ArrayList<ItemStack>();
        for (var holder : PowerArmorRecipe.all(access)) {
            var recipe = holder.value();
            if (recipe.result() != stack.getItem()) {
                continue;
            }
            for (var ingredient : recipe.cost()) {
                var item = ingredient.ingredient().items().findFirst().orElse(null);
                var count = (int) Math.floor(ingredient.count() * fraction);
                if (item != null && count > 0) {
                    result.add(new ItemStack(item, count));
                }
            }
            break;
        }
        return result;
    }

    private static void scrap(ServerPlayer player, int inventorySlot) {
        var inventory = player.getInventory();
        if (inventorySlot < 0 || inventorySlot >= inventory.getContainerSize()) {
            return;
        }
        var stack = inventory.getItem(inventorySlot);
        if (!(stack.getItem() instanceof PowerArmorPieceItem)) {
            return;
        }
        var returns = scrapYield(player.level().registryAccess(), stack);
        inventory.removeItem(inventorySlot, 1);
        inventory.setChanged();
        returns.forEach(item -> PlayerItems.give(player, item));
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1F);
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < PowerArmorSlot.COUNT;
    }

    private static void repair(ServerPlayer player, PowerArmorEntity armor, int slot) {
        if (!validSlot(slot)) {
            return;
        }
        var piece = armor.getSlotItem(slot);
        if (piece.isEmpty() || !piece.isDamaged()) {
            return;
        }
        if (!player.hasInfiniteMaterials() && !consumeOne(player)) {
            player.sendSystemMessage(Component.translatable("power_armor.atomicdawn.repair_missing"), true);
            return;
        }
        var copy = piece.copy();
        var restore = Math.round(
            copy.getMaxDamage() * (float) AtomicDawn.CONFIG.powerArmor.POWER_ARMOR_REPAIR_FRACTION
        );
        copy.setDamageValue(Math.max(0, copy.getDamageValue() - restore));
        armor.setSlotItem(slot, copy);
        player.level().playSound(null, armor.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
    }

    private static void repairWithKit(ServerPlayer player, PowerArmorEntity armor, int slot) {
        if (!validSlot(slot)) {
            return;
        }
        var piece = armor.getSlotItem(slot);
        if (piece.isEmpty() || !piece.isDamaged()) {
            return;
        }
        if (!RepairKits.consume(player)) {
            player.sendSystemMessage(Component.translatable("message.atomicdawn.repair_kit_missing"), true);
            return;
        }
        var copy = piece.copy();
        copy.setDamageValue(0);
        armor.setSlotItem(slot, copy);
        player.level().playSound(null, armor.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.5F);
    }

    private static boolean consumeOne(ServerPlayer player) {
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(REPAIR_MATERIALS)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static void paint(ServerPlayer player, PowerArmorEntity armor, PowerArmorStationMenu menu, int slot) {
        if (!validSlot(slot)) {
            return;
        }

        var piece = armor.getSlotItem(slot);
        var dye = menu.dyeStack();

        if (piece.isEmpty() || !(dye.getItem() instanceof DyeItem)) {
            return;
        }

        DyeColor color = DyeColor.getColor(dye);
        if (color == null) {
            return;
        }

        var copy = piece.copy();
        copy.set(
            ADDataComponents.POWER_ARMOR_PAINT.get(),
            color.getTextureDiffuseColor() & 0xFFFFFF
        );

        armor.setSlotItem(slot, copy);
        menu.consumeDye(player);
        menu.broadcastChanges();

        player.level()
            .playSound(
                null,
                armor.blockPosition(),
                SoundEvents.DYE_USE,
                SoundSource.BLOCKS,
                1F,
                1F
            );
    }

    private static void clearPaint(PowerArmorEntity armor, int slot) {
        if (!validSlot(slot)) {
            return;
        }
        var piece = armor.getSlotItem(slot);
        if (piece.isEmpty() || !piece.has(ADDataComponents.POWER_ARMOR_PAINT.get())) {
            return;
        }
        var copy = piece.copy();
        copy.remove(ADDataComponents.POWER_ARMOR_PAINT.get());
        armor.setSlotItem(slot, copy);
    }

    private static void installMod(ServerPlayer player, PowerArmorEntity armor, int slot, Identifier id) {
        if (!validSlot(slot)) {
            return;
        }
        var piece = armor.getSlotItem(slot);
        var mod = PowerArmorMod.get(player.level().registryAccess(), id).orElse(null);
        if (mod == null || !(piece.getItem() instanceof PowerArmorPieceItem item) || !mod.value().fits(item)) {
            return;
        }
        var upgrades = PowerArmorUpgrades.of(piece);
        var group = mod.value().group();
        if (id.equals(upgrades.mods().get(group))) {
            return;
        }
        var limit = AtomicDawn.CONFIG.powerArmor.POWER_ARMOR_MODS_PER_PIECE;
        if (!upgrades.mods().containsKey(group) && upgrades.mods().size() >= limit) {
            if (limit > 1) {
                player.sendSystemMessage(Component.translatable("power_armor.atomicdawn.mod_limit", limit), true);
                return;
            }

            upgrades = upgrades.withoutMods();
        }
        var cost = mod.value().cost();
        if (!player.hasInfiniteMaterials()) {
            if (!WeaponModding.hasMaterials(player, cost)) {
                player.sendSystemMessage(Component.translatable("power_armor.atomicdawn.mod_missing"), true);
                return;
            }
            WeaponModding.consumeMaterials(player, cost);
        }
        var copy = piece.copy();
        copy.set(ADDataComponents.POWER_ARMOR_UPGRADES.get(), upgrades.withMod(group, id));
        armor.setSlotItem(slot, copy);
        player.level()
            .playSound(null, armor.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
    }

    private static void removeMod(ServerPlayer player, PowerArmorEntity armor, int slot, Identifier id) {
        if (!validSlot(slot)) {
            return;
        }
        var piece = armor.getSlotItem(slot);
        var upgrades = PowerArmorUpgrades.of(piece);
        if (!upgrades.mods().containsValue(id)) {
            return;
        }
        var copy = piece.copy();
        copy.set(ADDataComponents.POWER_ARMOR_UPGRADES.get(), upgrades.withoutMod(id));
        armor.setSlotItem(slot, copy);
        player.level()
            .playSound(null, armor.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    private static void applyLegendary(ServerPlayer player, PowerArmorEntity armor, int slot, Identifier id) {
        if (!validSlot(slot)) {
            return;
        }
        var access = player.level().registryAccess();
        var piece = armor.getSlotItem(slot);
        var legendary = PowerArmorLegendary.get(access, id).orElse(null);
        if (legendary == null || !(piece.getItem() instanceof PowerArmorPieceItem)) {
            return;
        }
        var upgrades = PowerArmorUpgrades.of(piece);
        var star = legendary.value().star();
        if (id.equals(upgrades.legendary(access, star))) {
            return;
        }
        var modules = WorkbenchService.legendaryCost(star);
        if (!player.hasInfiniteMaterials()) {
            if (countItem(player, ADItems.LEGENDARY_MODULE.get()) < modules) {
                player.sendSystemMessage(
                    Component.translatable("power_armor.atomicdawn.legendary_missing", modules),
                    true
                );
                return;
            }
            consumeItem(player, ADItems.LEGENDARY_MODULE.get(), modules);
        }
        var copy = piece.copy();
        copy.set(ADDataComponents.POWER_ARMOR_UPGRADES.get(), upgrades.withLegendary(access, star, id));
        armor.setSlotItem(slot, copy);
        player.level()
            .playSound(null, armor.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.9F, 1F);
    }

    public static int countItem(Player player, Item item) {
        var total = 0;
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeItem(ServerPlayer player, Item item, int count) {
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize() && count > 0; i++) {
            var stack = inventory.getItem(i);
            if (stack.is(item)) {
                var taken = Math.min(count, stack.getCount());
                stack.shrink(taken);
                count -= taken;
            }
        }
        inventory.setChanged();
    }

    private static void fabricate(ServerPlayer player, ResourceKey<PowerArmorRecipe> key) {
        var recipe = player.level().registryAccess().lookupOrThrow(ADRegistries.POWER_ARMOR_RECIPE).get(key);
        if (recipe.isEmpty()) {
            return;
        }
        var value = recipe.get().value();
        if (!player.hasInfiniteMaterials()) {
            if (!WeaponModding.hasMaterials(player, value.cost())) {
                player.sendSystemMessage(Component.translatable("power_armor.atomicdawn.fabricate_missing"), true);
                return;
            }
            WeaponModding.consumeMaterials(player, value.cost());
        }
        PlayerItems.give(player, value.resultStack());
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1F);
    }
}

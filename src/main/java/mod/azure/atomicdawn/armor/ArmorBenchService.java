package mod.azure.atomicdawn.armor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.upgrade.ArmorEvents;
import mod.azure.atomicdawn.armor.upgrade.ArmorLegendary;
import mod.azure.atomicdawn.armor.upgrade.ArmorMod;
import mod.azure.atomicdawn.armor.upgrade.ArmorUpgrades;
import mod.azure.atomicdawn.crafting.ArmorBenchMenu;
import mod.azure.atomicdawn.crafting.WorkbenchService;
import mod.azure.atomicdawn.network.ArmorBenchActionPayload;
import mod.azure.atomicdawn.powerarmor.PowerArmorService;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.util.RepairKits;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;

public final class ArmorBenchService {

    private ArmorBenchService() {}

    public static void handle(ServerPlayer player, ArmorBenchActionPayload payload) {
        if (payload.action() == ArmorBenchActionPayload.Action.JET_PACK) {
            ArmorEvents.jetPack(player);
            return;
        }
        if (!(player.containerMenu instanceof ArmorBenchMenu menu) || !menu.stillValid(player)) {
            return;
        }
        switch (payload.action()) {
            case FABRICATE -> fabricate(player, payload.id());
            case INSTALL_MOD -> installMod(player, menu, payload.id());
            case REMOVE_MOD -> removeMod(player, menu, payload.id());
            case APPLY_LEGENDARY -> applyLegendary(player, menu, payload.id());
            case REPAIR -> repair(player, menu);
            case REPAIR_KIT -> repairWithKit(player, menu);
            case SCRAP -> scrap(player, menu);
            default -> {}
        }
    }

    private static void fabricate(ServerPlayer player, Identifier id) {
        var recipe = player.level()
            .registryAccess()
            .lookupOrThrow(ADRegistries.ARMOR_RECIPE)
            .get(ResourceKey.create(ADRegistries.ARMOR_RECIPE, id));
        if (recipe.isEmpty()) {
            return;
        }
        var value = recipe.get().value();
        if (!player.hasInfiniteMaterials()) {
            if (!WeaponModding.hasMaterials(player, value.cost())) {
                player.sendSystemMessage(Component.translatable("armor.atomicdawn.bench.missing"), true);
                return;
            }
            WeaponModding.consumeMaterials(player, value.cost());
        }
        PlayerItems.give(player, value.resultStack());
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1F);
    }

    private static void installMod(ServerPlayer player, ArmorBenchMenu menu, Identifier id) {
        var stack = menu.work();
        var mod = ArmorMod.get(player.level().registryAccess(), id).orElse(null);
        if (mod == null || !(stack.getItem() instanceof ArmorPieceItem item) || !mod.value().fits(item)) {
            return;
        }
        var upgrades = ArmorUpgrades.of(stack);
        var kind = mod.value().kind();
        if (upgrades.mod(kind).filter(id::equals).isPresent()) {
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            if (!WeaponModding.hasMaterials(player, mod.value().cost())) {
                player.sendSystemMessage(Component.translatable("armor.atomicdawn.bench.missing"), true);
                return;
            }
            WeaponModding.consumeMaterials(player, mod.value().cost());
        }
        var copy = stack.copy();
        ArmorUpgrades.set(copy, upgrades.withMod(kind, id));
        menu.setWork(copy);
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
    }

    private static void removeMod(ServerPlayer player, ArmorBenchMenu menu, Identifier id) {
        var stack = menu.work();
        var upgrades = ArmorUpgrades.of(stack);
        if (stack.isEmpty() || !upgrades.hasMod(id)) {
            return;
        }
        var copy = stack.copy();
        ArmorUpgrades.set(copy, upgrades.withoutMod(id));
        menu.setWork(copy);
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    private static void applyLegendary(ServerPlayer player, ArmorBenchMenu menu, Identifier id) {
        var stack = menu.work();
        var access = player.level().registryAccess();
        var legendary = ArmorLegendary.get(access, id).orElse(null);
        if (legendary == null || !ArmorPiece.isArmor(stack)) {
            return;
        }
        var upgrades = ArmorUpgrades.of(stack);
        var star = legendary.value().star();
        if (id.equals(upgrades.legendary(access, star))) {
            return;
        }
        var modules = WorkbenchService.legendaryCost(star);
        if (!player.hasInfiniteMaterials() && modules > 0) {
            if (PowerArmorService.countItem(player, ADItems.LEGENDARY_MODULE.get()) < modules) {
                player.sendSystemMessage(
                    Component.translatable("armor.atomicdawn.bench.legendary_missing", modules),
                    true
                );
                return;
            }
            WeaponModding.consumeMaterials(
                player,
                List.of(SizedIngredient.of(ADItems.LEGENDARY_MODULE.get(), modules))
            );
        }
        var copy = stack.copy();
        ArmorUpgrades.set(copy, upgrades.withLegendary(access, star, id));
        menu.setWork(copy);
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.9F, 1F);
    }

    public static boolean isRepairMaterial(ItemStack armor, ItemStack candidate) {
        var repairable = armor.get(DataComponents.REPAIRABLE);
        return repairable != null && !candidate.isEmpty() && repairable.isValidRepairItem(candidate);
    }

    public static int repairMaterials(Player player, ItemStack armor) {
        var total = 0;
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (isRepairMaterial(armor, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void repair(ServerPlayer player, ArmorBenchMenu menu) {
        var stack = menu.work();
        if (!ArmorPiece.isArmor(stack) || !stack.isDamaged()) {
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            var inventory = player.getInventory();
            var consumed = false;
            for (var i = 0; i < inventory.getContainerSize() && !consumed; i++) {
                var candidate = inventory.getItem(i);
                if (isRepairMaterial(stack, candidate)) {
                    candidate.shrink(1);
                    consumed = true;
                }
            }
            if (!consumed) {
                player.sendSystemMessage(Component.translatable("armor.atomicdawn.bench.repair_missing"), true);
                return;
            }
            inventory.setChanged();
        }
        var copy = stack.copy();
        var amount = Math.max(1, (int) Math.ceil(copy.getMaxDamage() * AtomicDawn.CONFIG.armor.ARMOR_REPAIR_FRACTION));
        copy.setDamageValue(Math.max(0, copy.getDamageValue() - amount));
        menu.setWork(copy);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    private static void repairWithKit(ServerPlayer player, ArmorBenchMenu menu) {
        var stack = menu.work();
        if (!ArmorPiece.isArmor(stack) || !stack.isDamaged()) {
            return;
        }
        if (!RepairKits.consume(player)) {
            player.sendSystemMessage(Component.translatable("message.atomicdawn.repair_kit_missing"), true);
            return;
        }
        var copy = stack.copy();
        copy.setDamageValue(0);
        menu.setWork(copy);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.5F);
    }

    public static List<ItemStack> scrapYield(RegistryAccess access, ItemStack stack) {
        if (!ArmorPiece.isArmor(stack)) {
            return List.of();
        }
        var recipe = ArmorRecipe.forResult(access, stack.getItem()).orElse(null);
        if (recipe == null) {
            return List.of();
        }
        var fraction = (float) AtomicDawn.CONFIG.armor.ARMOR_SCRAP_RETURN * Math.max(
            0.2F,
            ArmorPieceItem.condition(stack)
        );
        var result = new ArrayList<ItemStack>();
        for (var ingredient : recipe.cost()) {
            var item = ingredient.ingredient().items().findFirst().orElse(null);
            var count = (int) Math.floor(ingredient.count() * fraction / recipe.count());
            if (item != null && count > 0) {
                result.add(new ItemStack(item, count));
            }
        }
        return result;
    }

    private static void scrap(ServerPlayer player, ArmorBenchMenu menu) {
        var stack = menu.work();
        if (!ArmorPiece.isArmor(stack)) {
            return;
        }
        var returns = scrapYield(player.level().registryAccess(), stack);
        menu.setWork(ItemStack.EMPTY);
        returns.forEach(item -> PlayerItems.give(player, item));
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1F);
    }
}

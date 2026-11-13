package mod.azure.atomicdawn.client.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.modification.ModNames;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class WeaponTooltips {

    private WeaponTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.is(ADItems.RADAWAY.get())) {
            event.getToolTip().add(Component.translatable("tooltip.atomicdawn.radaway").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (stack.is(ADItems.RAD_X.get())) {
            event.getToolTip().add(Component.translatable("tooltip.atomicdawn.rad_x").withStyle(ChatFormatting.GRAY));
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null || !(stack.getItem() instanceof WeaponItem)) {
            return;
        }
        var base = Weapons.baseDefinition(level, stack);
        var effective = Weapons.definition(level, stack);
        if (base == null || effective == null) {
            return;
        }
        var tooltip = event.getToolTip();

        for (var effect : ModificationResolver.legendary(level.registryAccess(), stack)) {
            tooltip.add(
                Component.literal("★".repeat(effect.value().star()) + " ")
                    .append(ModNames.legendary(effect.key()))
                    .withStyle(ChatFormatting.GOLD)
            );
            tooltip.add(
                Component.literal("  ")
                    .append(ModNames.legendaryDescription(effect.key()))
                    .withStyle(ChatFormatting.GRAY)
            );
        }

        if (effective.condition().enabled()) {
            var condition = Weapons.condition(stack, effective);
            tooltip.add(
                Component.translatable("tooltip.atomicdawn.condition", condition.current(), condition.max())
                    .withStyle(condition.isBroken() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY)
            );
        }

        tooltip.add(
            Component.translatable("tooltip.atomicdawn.vats_cost", Math.round(effective.vats().apCost()))
                .withStyle(ChatFormatting.DARK_GRAY)
        );

        var installed = ModificationResolver.installed(stack, base);
        var defaults = base.modifications().defaults();
        for (var slot : base.modifications().slots()) {
            installed.get(slot)
                .filter(key -> !key.equals(defaults.get(slot)))
                .ifPresent(
                    key -> tooltip.add(
                        Component.translatable(slot.translationKey())
                            .append(": ")
                            .append(ModNames.mod(key))
                            .withStyle(ChatFormatting.DARK_GREEN)
                    )
                );
        }
    }
}

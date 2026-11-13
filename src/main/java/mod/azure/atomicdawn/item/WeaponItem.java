package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;

public class WeaponItem extends Item {

    private static final int AIM_USE_DURATION = 72000;

    private final ResourceKey<WeaponDefinition> definitionKey;

    public WeaponItem(Properties properties, ResourceKey<WeaponDefinition> definitionKey) {
        super(properties);
        this.definitionKey = definitionKey;
    }

    public ResourceKey<WeaponDefinition> definitionKey() {
        return definitionKey;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        var definition = Weapons.definition(level, player.getItemInHand(hand));
        if (definition != null && definition.isMelee()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            WeaponActions.setAiming(serverPlayer, player.getItemInHand(hand), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onStopUsing(@NotNull ItemStack stack, @NotNull LivingEntity entity, int count) {
        if (entity instanceof ServerPlayer serverPlayer) {
            WeaponActions.setAiming(serverPlayer, stack, false);
        }
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return AIM_USE_DURATION;
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity entity) {
        return true;
    }

    @Override
    public boolean onEntitySwing(
        @NotNull ItemStack stack,
        @NotNull LivingEntity entity,
        @NotNull InteractionHand hand
    ) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(
        @NotNull ItemStack oldStack,
        @NotNull ItemStack newStack,
        boolean slotChanged
    ) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        var condition = stack.get(ADDataComponents.WEAPON_CONDITION.get());
        return condition != null && condition.max() > 0 && condition.current() < condition.max();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var condition = stack.get(ADDataComponents.WEAPON_CONDITION.get());
        return condition == null ? 13 : Math.round(13F * condition.fraction());
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var condition = stack.get(ADDataComponents.WEAPON_CONDITION.get());
        var fraction = condition == null ? 1F : condition.fraction();
        return Mth.hsvToRgb(fraction / 3F, 1F, 1F);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var base = super.getName(stack);
        var legendary = stack.get(ADDataComponents.LEGENDARY.get());
        if (legendary == null || legendary.isEmpty()) {
            return base;
        }
        var prefix = Component.translatable(LegendaryEffect.translationKey(legendary.effects().getFirst()));
        var stars = "\u2605".repeat(legendary.effects().size());
        return Component.translatable("item.atomicdawn.legendary_name", prefix, base, stars)
            .withStyle(ChatFormatting.GOLD);
    }
}

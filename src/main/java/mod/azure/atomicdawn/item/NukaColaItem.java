package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Consumer;

import mod.azure.atomicdawn.AtomicDawn;

public class NukaColaItem extends Item {

    private final NukaCola variant;

    public NukaColaItem(Properties properties, NukaCola variant) {
        super(properties);
        this.variant = variant;
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(16).component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK);
    }

    public NukaCola variant() {
        return variant;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            NukaColaEffects.apply(player, variant);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull TooltipDisplay display,
        @NotNull Consumer<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable(variant.descriptionKey()).withStyle(ChatFormatting.GRAY));
        var config = AtomicDawn.CONFIG.common.survival.nukaCola;
        switch (variant) {
            case CLASSIC, ORANGE, WILD, VACCINATED -> restore(
                tooltip,
                config.NUKA_COLA_HEALTH,
                config.NUKA_COLA_AP,
                config.NUKA_COLA_RADS
            );
            case CRANBERRY -> {
                restore(tooltip, config.NUKA_CRANBERRY_HEALTH, 0.0, 0.0);
                if (config.NUKA_CRANBERRY_MINUTES > 0 && config.NUKA_CRANBERRY_XP_BONUS > 0.0) {
                    good(
                        tooltip,
                        "item.atomicdawn.nuka.xp",
                        amount(config.NUKA_CRANBERRY_XP_BONUS * 100.0),
                        config.NUKA_CRANBERRY_MINUTES
                    );
                }
            }
            case CHERRY -> restore(tooltip, config.NUKA_CHERRY_HEALTH, config.NUKA_CHERRY_AP, config.NUKA_CHERRY_RADS);
            case QUANTUM -> restore(
                tooltip,
                config.NUKA_QUANTUM_HEALTH,
                config.NUKA_QUANTUM_AP,
                config.NUKA_QUANTUM_RADS
            );
            case DARK -> {
                if (config.NUKA_DARK_SECONDS > 0) {
                    if (config.NUKA_DARK_BONUS > 0) {
                        good(
                            tooltip,
                            "item.atomicdawn.nuka.dark_bonus",
                            config.NUKA_DARK_BONUS,
                            config.NUKA_DARK_SECONDS
                        );
                    }
                    if (config.NUKA_DARK_PENALTY > 0) {
                        bad(
                            tooltip,
                            "item.atomicdawn.nuka.dark_penalty",
                            config.NUKA_DARK_PENALTY,
                            config.NUKA_DARK_SECONDS
                        );
                    }
                }
            }
            case TWIST -> {
                if (config.NUKA_TWIST_MINUTES > 0 && config.NUKA_TWIST_BONUS > 0) {
                    good(tooltip, "item.atomicdawn.nuka.twist", config.NUKA_TWIST_BONUS, config.NUKA_TWIST_MINUTES);
                }
            }
            case GRAPE -> {
                overTime(
                    tooltip,
                    "item.atomicdawn.nuka.health_over",
                    config.NUKA_GRAPE_HEALTH,
                    config.NUKA_GRAPE_HEALTH_SECONDS,
                    false
                );
                overTime(
                    tooltip,
                    "item.atomicdawn.nuka.ap_over",
                    config.NUKA_GRAPE_AP,
                    config.NUKA_GRAPE_AP_SECONDS,
                    false
                );
                overTime(
                    tooltip,
                    "item.atomicdawn.nuka.rads_removed_over",
                    config.NUKA_GRAPE_RADS,
                    config.NUKA_GRAPE_RAD_SECONDS,
                    false
                );
            }
        }
    }

    private static void overTime(
        Consumer<Component> tooltip,
        String key,
        double amount,
        int seconds,
        boolean rads
    ) {
        if (amount <= 0.0) {
            return;
        }
        if (rads) {
            bad(tooltip, key, amount(amount), seconds);
        } else {
            good(tooltip, key, amount(amount), seconds);
        }
    }

    private static void restore(Consumer<Component> tooltip, double health, double ap, double rads) {
        if (health > 0.0) {
            good(tooltip, "item.atomicdawn.nuka.health", amount(health));
        }
        if (ap > 0.0) {
            good(tooltip, "item.atomicdawn.nuka.ap", amount(ap));
        }
        if (rads > 0.0) {
            bad(tooltip, "item.atomicdawn.nuka.rads", amount(rads));
        }
    }

    private static void good(Consumer<Component> tooltip, String key, Object... args) {
        tooltip.accept(Component.translatable(key, args).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void bad(Consumer<Component> tooltip, String key, Object... args) {
        tooltip.accept(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }

    private static String amount(double value) {
        return value == Math.rint(value)
            ? Long.toString(Math.round(value))
            : String.format(Locale.ROOT, "%.1f", value);
    }
}

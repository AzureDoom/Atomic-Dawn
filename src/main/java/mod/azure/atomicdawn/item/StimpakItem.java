package mod.azure.atomicdawn.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.limb.LimbService;

public class StimpakItem extends Item {

    private final boolean fullLimbRestore;

    public StimpakItem(Properties properties, boolean fullLimbRestore) {
        super(properties);
        this.fullLimbRestore = fullLimbRestore;
    }

    public static Properties properties(Properties properties, float seconds) {
        return properties.stacksTo(16)
            .component(
                DataComponents.CONSUMABLE,
                Consumable.builder()
                    .consumeSeconds(seconds)
                    .animation(ItemUseAnimation.NONE)
                    .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BOTTLE_EMPTY))
                    .hasConsumeParticles(false)
                    .build()
            );
    }

    private void treat(LivingEntity target) {
        if (fullLimbRestore) {
            LimbService.healAll(target);
            return;
        }
        target.heal((float) AtomicDawn.CONFIG.limbs.STIMPAK_HEALTH);
        LimbService.restore(target, (float) AtomicDawn.CONFIG.limbs.STIMPAK_LIMB_RESTORE, true);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entity
    ) {
        if (!level.isClientSide()) {
            treat(entity);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(
        @NotNull ItemStack stack,
        @NotNull Player player,
        @NotNull LivingEntity target,
        @NotNull InteractionHand hand
    ) {
        if (!target.isAlive()) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            treat(target);
            player.level()
                .playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    SoundEvents.BOTTLE_EMPTY,
                    SoundSource.PLAYERS,
                    1F,
                    1.2F
                );
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}

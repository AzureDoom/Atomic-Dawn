package mod.azure.atomicdawn.progression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADItems;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class ProgressionEvents {

    private ProgressionEvents() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var victim = event.getEntity();
        if (
            !(victim instanceof Enemy)
                || !(event.getSource().getEntity() instanceof ServerPlayer)
                || !(victim.level() instanceof ServerLevel level)
        ) {
            return;
        }
        var random = victim.getRandom();
        if (random.nextDouble() < AtomicDawn.CONFIG.common.equipment.workbenches.LEGENDARY_MODULE_DROP_CHANCE) {
            drop(event, level, new ItemStack(ADItems.LEGENDARY_MODULE.get()));
        }
        if (random.nextDouble() < AtomicDawn.CONFIG.common.equipment.workbenches.LEGENDARY_WEAPON_DROP_CHANCE) {
            drop(event, level, LegendaryRolls.randomLegendaryWeapon(level, random));
        }
        if (random.nextDouble() < AtomicDawn.CONFIG.common.equipment.workbenches.LEGENDARY_ARMOR_DROP_CHANCE) {
            drop(event, level, LegendaryRolls.randomLegendaryArmor(level, random));
        }
    }

    private static void drop(LivingDropsEvent event, ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        var victim = event.getEntity();
        event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack));
    }
}

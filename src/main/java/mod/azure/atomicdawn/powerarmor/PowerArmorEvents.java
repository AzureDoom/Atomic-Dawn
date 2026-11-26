package mod.azure.atomicdawn.powerarmor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADEntities;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class PowerArmorEvents {

    private PowerArmorEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ADEntities.POWER_ARMOR.get(), PowerArmorEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onDismount(EntityMountEvent event) {
        if (
            event.isDismounting() && event.getEntityBeingMounted() instanceof PowerArmorEntity armor
                && !armor.isExitRequested() && event.getEntityMounting() instanceof Player player && player.isAlive()
                && player.isShiftKeyDown()
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (
            event.getEntity() instanceof ServerPlayer player && player.getVehicle() instanceof PowerArmorEntity
                && !event.getTo().isEmpty() && isPilotArmorSlot(event.getSlot())
        ) {
            PowerArmorEntity.unequipArmor(player);
        }
    }

    private static boolean isPilotArmorSlot(EquipmentSlot slot) {
        for (var armorSlot : PowerArmorEntity.PILOT_ARMOR_SLOTS) {
            if (armorSlot == slot) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (
            source.getEntity() instanceof ServerPlayer attacker && attacker
                .getVehicle() instanceof PowerArmorEntity armor
        ) {
            if (event.getEntity() != armor) {
                armor.drain((float) AtomicDawn.CONFIG.common.equipment.powerArmor.fusionCores.FUSION_CORE_ATTACK_COST);
            }
        }
        if (
            !(event.getEntity() instanceof ServerPlayer player) || !(player
                .getVehicle() instanceof PowerArmorEntity armor)
        ) {
            return;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            return;
        }
        if (PowerArmorEntity.isForwarding()) {
            return;
        }
        event.setAmount(armor.absorb(source, event.getAmount()));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity().getVehicle() instanceof PowerArmorEntity) {
            event.setCanceled(true);
        }
    }
}

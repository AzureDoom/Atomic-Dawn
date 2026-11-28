package mod.azure.atomicdawn.powerarmor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADEntities;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class PowerArmorEvents {

    private static final double FOLLOW_DISTANCE_SQR = 9.0;

    private static final long FOLLOW_WINDOW_TICKS = 5L;

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private record Pending(
        UUID armor,
        ResourceKey<Level> dimension,
        long time
    ) {}

    private PowerArmorEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ADEntities.POWER_ARMOR.get(), PowerArmorEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onDismount(EntityMountEvent event) {
        if (
            !event.isDismounting() || !(event.getEntityBeingMounted() instanceof PowerArmorEntity armor)
                || armor.isExitRequested() || !(event.getEntityMounting() instanceof Player player) || !player.isAlive()
        ) {
            return;
        }
        if (player.isShiftKeyDown()) {
            event.setCanceled(true);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && !armor.isRemoved()) {
            armor.prepareQuietDismount();
            PENDING.put(
                serverPlayer.getUUID(),
                new Pending(armor.getUUID(), armor.level().dimension(), serverPlayer.level().getGameTime())
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || PENDING.isEmpty()) {
            return;
        }
        var pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }
        var armor = pendingArmor(player, pending);
        if (
            armor == null || armor.isRemoved() || !player.isAlive() || player.isPassenger() || !armor.getPassengers()
                .isEmpty()
        ) {
            PENDING.remove(player.getUUID());
            return;
        }
        var moved = armor.level() != player.level() || armor.distanceToSqr(player) > FOLLOW_DISTANCE_SQR;
        if (!moved) {
            if (player.level().getGameTime() - pending.time() > FOLLOW_WINDOW_TICKS) {
                PENDING.remove(player.getUUID());
                armor.playExitEffects();
            }
            return;
        }
        PENDING.remove(player.getUUID());
        var target = armor.teleport(
            new TeleportTransition(
                player.level(),
                player.position(),
                Vec3.ZERO,
                player.getYRot(),
                0F,
                TeleportTransition.DO_NOTHING
            )
        );
        if (target instanceof PowerArmorEntity relocated) {
            player.startRiding(relocated);
        }
    }

    @Nullable
    private static PowerArmorEntity pendingArmor(ServerPlayer player, Pending pending) {
        var server = player.level().getServer();
        if (server == null) {
            return null;
        }
        var origin = server.getLevel(pending.dimension());
        if (origin == null) {
            return null;
        }
        return origin.getEntity(pending.armor()) instanceof PowerArmorEntity armor ? armor : null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (
            event.getEntity() instanceof Player player && player.getVehicle() instanceof PowerArmorEntity
                && player.isInvisible() && !player.hasEffect(MobEffects.INVISIBILITY)
        ) {
            var cover = Math.max(0.1F, player.getArmorCoverPercentage());
            event.modifyVisibility(1.0 / (0.7 * cover));
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

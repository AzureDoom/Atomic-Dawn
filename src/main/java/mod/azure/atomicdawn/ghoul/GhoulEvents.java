package mod.azure.atomicdawn.ghoul;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class GhoulEvents {

    private GhoulEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GhoulService.suppressExhaustion(player);
        if ((player.tickCount + player.getId()) % 20 == 0) {
            GhoulService.tickSecond(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GhoulService.reconcile(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GhoulService.reconcile(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GhoulService.reconcile(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GhoulService.forget(event.getEntity().getUUID());
        GhoulTreatmentService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!GhoulService.enabled()) {
            return;
        }
        var source = event.getSource();
        var victim = event.getEntity();
        var amount = event.getAmount();
        if (
            source.getEntity() instanceof ServerPlayer attacker && attacker != victim && GhoulService.isGhoul(attacker)
        ) {
            amount *= GhoulService.outgoingDamageMultiplier(attacker);
            if (isMelee(source)) {
                amount *= GhoulService.meleeMultiplier(attacker);
            }
        }
        if (
            victim instanceof ServerPlayer player && GhoulService.isGhoul(player)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
        ) {
            amount *= GhoulService.incomingDamageMultiplier(player);
        }
        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }

    private static boolean isMelee(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypes.THORNS);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        GhoulCommands.register(event.getDispatcher());
    }
}

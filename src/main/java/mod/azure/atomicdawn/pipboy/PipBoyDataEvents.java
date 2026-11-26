package mod.azure.atomicdawn.pipboy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.command.QuestCommands;
import mod.azure.atomicdawn.pipboy.bestiary.BestiaryService;
import mod.azure.atomicdawn.pipboy.location.LocationService;
import mod.azure.atomicdawn.quest.QuestService;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class PipBoyDataEvents {

    private PipBoyDataEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestService.autoStart(player);
            QuestService.evaluate(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var age = player.tickCount;
        if (age % 20 == 0) {
            QuestService.evaluate(player);
        }
        if (age % AtomicDawn.CONFIG.common.pipboy.locations.PIPBOY_LOCATION_SCAN_TICKS == 7) {
            LocationService.tick(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity()) {
            BestiaryService.encounter(killer, event.getEntity());
            BestiaryService.recordKill(killer, event.getEntity());
            QuestService.onKill(killer, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        var source = event.getSource().getEntity();
        if (source instanceof ServerPlayer attacker && victim != attacker) {
            BestiaryService.encounter(attacker, victim);
        } else if (
            victim instanceof ServerPlayer player && source instanceof LivingEntity attacker
                && attacker instanceof Enemy
        ) {
            BestiaryService.encounter(player, attacker);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        QuestCommands.register(event.getDispatcher());
    }
}

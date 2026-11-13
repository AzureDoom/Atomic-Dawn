package mod.azure.atomicdawn.special;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.command.AtomicDawnCommands;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;
import mod.azure.atomicdawn.weapon.instance.Weapons;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class SpecialEvents {

    private SpecialEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PipBoyService.ensureDevice(player);
            SpecialService.ensureInitialized(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpecialService.forget(player);
            PipBoyService.forget(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PipBoyService.ensureDevice(player);
            SpecialService.ensureInitialized(player);
            if (!event.isEndConquered()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpecialService.refresh(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        var victim = event.getEntity();
        if (
            victim instanceof Enemy && event.getSource().getEntity() instanceof ServerPlayer killer && killer != victim
        ) {
            SpecialService.awardKillXp(killer, victim);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (event.getEntity() instanceof ServerPlayer victim && source.getEntity() != null) {
            SpecialService.markCombat(victim);
        }
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        SpecialService.markCombat(attacker);
        if (!source.is(DamageTypes.PLAYER_ATTACK) || source.getDirectEntity() != attacker) {
            return;
        }
        var held = attacker.getMainHandItem();
        if (Weapons.definition(attacker.level(), held) != null) {
            return;
        }
        var category = held.isEmpty() ? WeaponCategory.UNARMED : WeaponCategory.MELEE;
        var bonus = SpecialStatResolver.resolve(attacker).damageBonus(category, false, true);
        if (bonus > 0F) {
            event.setAmount(event.getAmount() * (1F + bonus));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AtomicDawnCommands.register(event.getDispatcher());
    }
}

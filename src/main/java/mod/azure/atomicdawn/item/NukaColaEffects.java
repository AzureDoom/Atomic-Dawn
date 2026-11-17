package mod.azure.atomicdawn.item;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADMobEffects;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialService;
import mod.azure.atomicdawn.weapon.action.VatsActions;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class NukaColaEffects {

    private static final Set<UUID> PENDING_REFRESH = ConcurrentHashMap.newKeySet();

    private NukaColaEffects() {}

    public static void apply(ServerPlayer player, NukaCola variant) {
        var config = AtomicDawn.CONFIG.nukaCola;
        switch (variant) {
            case CLASSIC, ORANGE, WILD, VACCINATED -> restore(
                player,
                config.NUKA_COLA_HEALTH,
                config.NUKA_COLA_AP,
                config.NUKA_COLA_RADS
            );
            case CRANBERRY -> {
                restore(player, config.NUKA_CRANBERRY_HEALTH, 0.0, 0.0);
                timed(player, ADMobEffects.NUKA_CRANBERRY, config.NUKA_CRANBERRY_MINUTES * 60);
            }
            case CHERRY -> restore(player, config.NUKA_CHERRY_HEALTH, config.NUKA_CHERRY_AP, config.NUKA_CHERRY_RADS);
            case QUANTUM -> restore(
                player,
                config.NUKA_QUANTUM_HEALTH,
                config.NUKA_QUANTUM_AP,
                config.NUKA_QUANTUM_RADS
            );
            case DARK -> timed(player, ADMobEffects.NUKA_DARK, config.NUKA_DARK_SECONDS);
            case TWIST -> {
                var attribute = SpecialAttribute.byIndex(player.getRandom().nextInt(SpecialAttribute.COUNT));
                timed(player, ADMobEffects.NUKA_TWIST.get(attribute), config.NUKA_TWIST_MINUTES * 60);
            }
        }
    }

    public static int specialBonus(Player player, SpecialAttribute attribute) {
        var config = AtomicDawn.CONFIG.nukaCola;
        var bonus = 0;
        if (player.hasEffect(ADMobEffects.NUKA_DARK)) {
            bonus += switch (attribute) {
                case STRENGTH, ENDURANCE -> config.NUKA_DARK_BONUS;
                case PERCEPTION -> -config.NUKA_DARK_PENALTY;
                default -> 0;
            };
        }
        if (player.hasEffect(ADMobEffects.NUKA_TWIST.get(attribute))) {
            bonus += config.NUKA_TWIST_BONUS;
        }
        return bonus;
    }

    public static float xpBonus(Player player) {
        return player.hasEffect(ADMobEffects.NUKA_CRANBERRY)
            ? (float) AtomicDawn.CONFIG.nukaCola.NUKA_CRANBERRY_XP_BONUS
            : 0F;
    }

    private static void restore(ServerPlayer player, double health, double ap, double rads) {
        if (health > 0.0) {
            player.heal((float) health);
        }
        if (ap > 0.0) {
            var current = VatsActions.actionPoints(player);
            player.setData(
                ADAttachments.ACTION_POINTS.get(),
                current.withPoints(current.points() + (float) ap, VatsActions.maxActionPoints(player))
            );
        }
        if (rads > 0.0) {
            RadiationService.expose(player, (float) rads);
        }
    }

    private static void timed(ServerPlayer player, Holder<MobEffect> effect, int seconds) {
        if (seconds <= 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, seconds * 20, 0));
        SpecialService.refresh(player);
    }

    private static boolean affectsSpecial(@Nullable Holder<MobEffect> effect) {
        if (effect == null) {
            return false;
        }
        var value = effect.value();
        if (value == ADMobEffects.NUKA_DARK.get()) {
            return true;
        }
        for (var twist : ADMobEffects.NUKA_TWIST.values()) {
            if (value == twist.get()) {
                return true;
            }
        }
        return false;
    }

    private static void queue(LivingEntity entity, @Nullable Holder<MobEffect> effect) {
        if (entity instanceof ServerPlayer player && affectsSpecial(effect)) {
            PENDING_REFRESH.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        var instance = event.getEffectInstance();
        queue(event.getEntity(), instance == null ? null : instance.getEffect());
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        queue(event.getEntity(), event.getEffect());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (
            !PENDING_REFRESH.isEmpty() && event.getEntity() instanceof ServerPlayer player && PENDING_REFRESH.remove(
                player.getUUID()
            )
        ) {
            SpecialService.refresh(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_REFRESH.remove(event.getEntity().getUUID());
    }
}

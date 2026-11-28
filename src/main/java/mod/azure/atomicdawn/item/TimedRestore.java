package mod.azure.atomicdawn.item;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.weapon.action.VatsActions;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class TimedRestore {

    private static final Map<UUID, List<Active>> ACTIVE = new ConcurrentHashMap<>();

    private TimedRestore() {}

    public record Dose(
        float health,
        int healthTicks,
        float ap,
        int apTicks,
        float rads,
        int radTicks
    ) {}

    private static final class Active {

        private final Dose dose;

        private int age;

        private Active(Dose dose) {
            this.dose = dose;
        }

        private boolean done() {
            return age >= Math.max(dose.healthTicks(), Math.max(dose.apTicks(), dose.radTicks()));
        }
    }

    public static void add(ServerPlayer player, Dose dose) {
        apply(player, dose.health(), dose.healthTicks(), 0, true, Kind.HEALTH);
        apply(player, dose.ap(), dose.apTicks(), 0, true, Kind.AP);
        apply(player, dose.rads(), dose.radTicks(), 0, true, Kind.RADS);
        if (dose.healthTicks() > 0 || dose.apTicks() > 0 || dose.radTicks() > 0) {
            ACTIVE.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(new Active(dose));
        }
    }

    private enum Kind {
        HEALTH,
        AP,
        RADS
    }

    private static void apply(ServerPlayer player, float total, int ticks, int age, boolean instant, Kind kind) {
        if (total <= 0F) {
            return;
        }
        float amount;
        if (ticks <= 0) {
            if (!instant) {
                return;
            }
            amount = total;
        } else {
            if (instant || age >= ticks) {
                return;
            }
            amount = total / ticks;
        }
        switch (kind) {
            case HEALTH -> player.heal(amount);
            case AP -> {
                var current = VatsActions.actionPoints(player);
                player.setData(
                    ADAttachments.ACTION_POINTS.get(),
                    current.withPoints(current.points() + amount, VatsActions.maxActionPoints(player))
                );
            }
            case RADS -> RadiationService.remove(player, amount);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || ACTIVE.isEmpty()) {
            return;
        }
        var doses = ACTIVE.get(player.getUUID());
        if (doses == null) {
            return;
        }
        for (var active : doses) {
            var dose = active.dose;
            apply(player, dose.health(), dose.healthTicks(), active.age, false, Kind.HEALTH);
            apply(player, dose.ap(), dose.apTicks(), active.age, false, Kind.AP);
            apply(player, dose.rads(), dose.radTicks(), active.age, false, Kind.RADS);
            active.age++;
        }
        doses.removeIf(Active::done);
        if (doses.isEmpty()) {
            ACTIVE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }
}

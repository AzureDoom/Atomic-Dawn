package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CortexRuntime;
import com.azure.azurecortex.sensing.TargetSensor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.npc.NpcEntity;

public final class FeralGhoulBrain {

    public static final String CLAW_COOLDOWN = "atomicdawn:feral_ghoul/claw";

    public static final String LEAP_COOLDOWN = "atomicdawn:feral_ghoul/leap";

    public static final String WANDER_COOLDOWN = "atomicdawn:feral_ghoul/wander";

    public static final String SCREAM_COOLDOWN = "atomicdawn:feral_ghoul/scream";

    public static final int SCREAM_INTERVAL = 600;

    public static final int LEAP_INTERVAL = 80;

    private static final int SENSOR_INTERVAL = 10;

    private final CortexRuntime<FeralGhoulEntity, Void> runtime;

    public FeralGhoulBrain(FeralGhoulEntity ghoul) {
        var sensor = new TargetSensor<FeralGhoulEntity>(
            TargetSensor.<FeralGhoulEntity>nearestMatching(
                AtomicDawn.CONFIG.feralGhouls.DETECTION_RANGE,
                FeralGhoulBrain::validTarget
            ),
            SENSOR_INTERVAL,
            TargetSensor.<FeralGhoulEntity>lineOfSight()
        );
        this.runtime = new CortexRuntime<>(ghoul, sensor, FeralGhoulTree.create());
    }

    public void tick() {
        runtime.tick();
    }

    @Nullable
    public LivingEntity target() {
        LivingEntity target = runtime.getBlackboard().get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    public void setTarget(LivingEntity target) {
        runtime.getBlackboard().set(CommonBlackboardKeys.TARGET, target);
    }

    public void suppressScream(int ticks) {
        runtime.getCooldowns().set(SCREAM_COOLDOWN, ticks);
    }

    public static boolean validTarget(LivingEntity candidate) {
        if (!candidate.isAlive() || candidate instanceof FeralGhoulEntity) {
            return false;
        }
        if (candidate instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            return !(AtomicDawn.CONFIG.feralGhouls.IGNORE_GHOUL_PLAYERS && GhoulService.isGhoul(player));
        }
        if (candidate instanceof NpcEntity npc) {
            return !npc.isHostile() && npc.canFight();
        }
        return candidate instanceof AbstractVillager && AtomicDawn.CONFIG.feralGhouls.ATTACK_VILLAGERS;
    }
}

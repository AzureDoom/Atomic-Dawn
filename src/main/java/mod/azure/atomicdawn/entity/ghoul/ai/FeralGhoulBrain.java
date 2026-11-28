package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CortexRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;
import mod.azure.atomicdawn.helper.ADEntityUtil;
import mod.azure.atomicdawn.npc.NpcEntity;

public final class FeralGhoulBrain {

    public static final TagKey<EntityType<?>> TARGETS = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("feral_ghoul_targets")
    );

    public static final String CLAW_COOLDOWN = "atomicdawn:feral_ghoul/claw";

    public static final String LEAP_COOLDOWN = "atomicdawn:feral_ghoul/leap";

    public static final String WANDER_COOLDOWN = "atomicdawn:feral_ghoul/wander";

    public static final String SCREAM_COOLDOWN = "atomicdawn:feral_ghoul/scream";

    public static final int SCREAM_INTERVAL = 600;

    public static final int LEAP_INTERVAL = 80;

    private static final int SENSOR_INTERVAL = 10;

    private static final int LOSE_SIGHT_TICKS = 100;

    private final FeralGhoulEntity ghoul;

    private final CortexRuntime<FeralGhoulEntity, Void> runtime;

    private long lastSeenTick;

    public FeralGhoulBrain(FeralGhoulEntity ghoul) {
        this.ghoul = ghoul;
        this.runtime = new CortexRuntime<>(ghoul, null, FeralGhoulTree.create());
    }

    public void tick() {
        sense(ghoul.level().getGameTime());
        runtime.tick();
    }

    private void sense(long now) {
        var blackboard = runtime.getBlackboard();
        var target = target();
        if (target == null && blackboard.has(CommonBlackboardKeys.TARGET)) {
            blackboard.remove(CommonBlackboardKeys.TARGET);
        } else if (target != null && !validTarget(target)) {
            blackboard.remove(CommonBlackboardKeys.TARGET);
            target = null;
        }
        var visible = false;
        if (target != null) {
            var range = AtomicDawn.CONFIG.server.entities.mutants.feralGhouls.behavior.DETECTION_RANGE * 1.5;
            visible = ghoul.distanceToSqr(target) <= range * range && ghoul.getSensing().hasLineOfSight(target);
            if (visible) {
                lastSeenTick = now;
            } else if (now - lastSeenTick > LOSE_SIGHT_TICKS) {
                blackboard.remove(CommonBlackboardKeys.TARGET);
                target = null;
            }
        }
        if ((now + ghoul.getId()) % SENSOR_INTERVAL == 0 && (target == null || !visible)) {
            var found = scan();
            if (found != null && found != target) {
                setTarget(found);
            }
        }
    }

    @Nullable
    private LivingEntity scan() {
        var range = AtomicDawn.CONFIG.server.entities.mutants.feralGhouls.behavior.DETECTION_RANGE;
        return ghoul.level()
            .getEntitiesOfClass(
                LivingEntity.class,
                ghoul.getBoundingBox().inflate(range),
                candidate -> candidate != ghoul && validTarget(candidate) && ghoul.distanceToSqr(candidate) <= range
                    * range && ghoul.getSensing().hasLineOfSight(candidate)
            )
            .stream()
            .min(Comparator.comparingDouble(ghoul::distanceToSqr))
            .orElse(null);
    }

    @Nullable
    public LivingEntity target() {
        LivingEntity target = runtime.getBlackboard().get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    public void setTarget(LivingEntity target) {
        runtime.getBlackboard().set(CommonBlackboardKeys.TARGET, target);
        lastSeenTick = ghoul.level().getGameTime();
    }

    public void suppressScream(int ticks) {
        runtime.getCooldowns().set(SCREAM_COOLDOWN, ticks);
    }

    public static boolean validTarget(LivingEntity candidate) {
        if (!candidate.isAlive() || candidate instanceof FeralGhoulEntity) {
            return false;
        }
        if (candidate instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        if (candidate instanceof NpcEntity npc) {
            return !npc.isHostile() && npc.canFight();
        }
        if (candidate instanceof AbstractVillager) {
            return AtomicDawn.CONFIG.server.entities.mutants.feralGhouls.behavior.ATTACK_VILLAGERS;
        }
        return ADEntityUtil.isIn(candidate, TARGETS);
    }
}

package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastCloudEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.registry.ADSounds;

final class ScorchbeastCropDustTask extends ScorchbeastTask {

    private static final double SETUP_DISTANCE = 34.0;

    private static final double SETUP_SPEED = 0.9;

    private static final double CLIMB_SPEED = 1.1;

    private static final double DIVE_AGILITY = 0.35;

    private static final double OVERSHOOT = 26.0;

    private static final double LEAD = 8.0;

    private static final double RELEASE_RANGE = 14.0;

    private static final int RELEASE_INTERVAL = 3;

    private static final int MAX_TICKS = 260;

    private enum Phase {
        SETUP,
        DIVE,
        CLIMB
    }

    private Phase phase = Phase.SETUP;

    @Nullable
    private Vec3 heading;

    @Nullable
    private Vec3 exit;

    private int ticks;

    private boolean screeched;

    ScorchbeastCropDustTask() {
        super(55, false);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !agent.flying() || agent.wingCrippled()) {
            return false;
        }
        return attackReady(cooldowns, ScorchbeastBrain.DUST_COOLDOWN) && horizontalDistance(
            agent,
            target
        ) <= AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.cropDusting.DUST_TRIGGER_RANGE;
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        screeched = false;
        exit = null;
        var target = target(blackboard);
        if (target == null) {
            heading = null;
            return;
        }
        var flat = new Vec3(target.getX() - agent.getX(), 0, target.getZ() - agent.getZ());
        heading = flat.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0F, agent.getYRot()) : flat.normalize();
        phase = flat.length() >= SETUP_DISTANCE * 0.8 ? Phase.DIVE : Phase.SETUP;
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (heading == null || ++ticks > MAX_TICKS) {
            return ActionOutcome.success();
        }
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast;
        switch (phase) {
            case SETUP -> {
                if (target == null) {
                    return ActionOutcome.success();
                }
                var start = target.position().subtract(heading.scale(SETUP_DISTANCE));
                var altitude = Math.max(target.getY(), agent.groundY(start.x, start.z)) + config.flight.CRUISE_ALTITUDE;
                agent.flyTo(new Vec3(start.x, altitude, start.z), SETUP_SPEED, 0.2);
                if (horizontalDistance(agent, target) >= SETUP_DISTANCE * 0.8) {
                    phase = Phase.DIVE;
                }
            }
            case DIVE -> {
                if (!screeched) {
                    screeched = true;
                    agent.playSound(ADSounds.SCORCHBEAST_SCREECH.get(), 4F, agent.getVoicePitch());
                }
                agent.allowLowFlight(config.cropDusting.DUST_ALTITUDE * 0.6);
                Vec3 aim;
                if (target != null) {
                    var flat = new Vec3(target.getX() - agent.getX(), 0, target.getZ() - agent.getZ());
                    if (flat.dot(heading) <= 0) {
                        startClimb(agent);
                        return ActionOutcome.running();
                    }
                    if (flat.length() > 4.0) {
                        heading = heading.scale(0.85).add(flat.normalize().scale(0.15)).normalize();
                    }
                    aim = target.position().add(heading.scale(LEAD));
                } else {
                    aim = agent.position().add(heading.scale(LEAD));
                }
                var floor = Math.max(aim.y, agent.groundY(aim.x, aim.z));
                agent.flyTo(
                    new Vec3(aim.x, floor + config.cropDusting.DUST_ALTITUDE, aim.z),
                    config.cropDusting.DUST_DIVE_SPEED,
                    DIVE_AGILITY
                );
                if (target == null) {
                    startClimb(agent);
                }
            }
            case CLIMB -> {
                if (exit == null) {
                    return ActionOutcome.success();
                }
                agent.allowLowFlight(config.cropDusting.DUST_ALTITUDE * 0.6);
                agent.flyTo(exit, CLIMB_SPEED, 0.25);
                if (agent.position().distanceTo(exit) < 6.0) {
                    return ActionOutcome.success();
                }
            }
        }
        if (
            target != null && phase != Phase.SETUP && ticks % RELEASE_INTERVAL == 0 && near(agent, target)
                && agent.level() instanceof ServerLevel level
        ) {
            release(level, agent);
        }
        return ActionOutcome.running();
    }

    private void startClimb(ScorchbeastEntity agent) {
        phase = Phase.CLIMB;
        var out = agent.position().add(heading.scale(OVERSHOOT));
        exit = new Vec3(
            out.x,
            agent.groundY(out.x, out.z) + AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.flight.CRUISE_ALTITUDE,
            out.z
        );
    }

    private static boolean near(ScorchbeastEntity agent, LivingEntity target) {
        return horizontalDistance(agent, target) <= RELEASE_RANGE;
    }

    private static void release(ServerLevel level, ScorchbeastEntity agent) {
        var side = Vec3.directionFromRotation(0F, agent.getYRot() + 90F);
        var span = agent.getBbWidth() * 1.6;
        for (var sign = -1; sign <= 1; sign += 2) {
            var tip = agent.position().add(side.scale(span * sign)).add(0, agent.getBbHeight() * 0.3, 0);
            level.addFreshEntity(ScorchbeastCloudEntity.create(level, tip.x, tip.y, tip.z));
        }
        agent.playSound(ADSounds.SCORCHBEAST_DUST.get(), 2.5F, 0.8F + agent.getRandom().nextFloat() * 0.2F);
    }

    @Override
    void end(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.cropDusting;
        cooldowns.set(ScorchbeastBrain.DUST_COOLDOWN, config.DUST_COOLDOWN_TICKS + agent.getRandom().nextInt(120));
        finishAttack(agent, cooldowns);
    }
}

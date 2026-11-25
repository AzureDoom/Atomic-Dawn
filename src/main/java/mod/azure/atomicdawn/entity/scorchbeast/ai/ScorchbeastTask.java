package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

abstract class ScorchbeastTask implements Action<ScorchbeastEntity, Void> {

    private final int priority;

    private final boolean interruptible;

    private boolean running;

    ScorchbeastTask(int priority, boolean interruptible) {
        this.priority = priority;
        this.interruptible = interruptible;
    }

    @Nullable
    static LivingEntity target(Blackboard blackboard) {
        LivingEntity target = blackboard.get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(ScorchbeastBlackboardKeys.TARGET_VISIBLE));
    }

    static boolean hunting(Blackboard blackboard) {
        return target(blackboard) != null;
    }

    static boolean attackReady(CooldownTracker cooldowns, String key) {
        return cooldowns.ready(key) && cooldowns.ready(ScorchbeastBrain.GLOBAL_COOLDOWN);
    }

    static double horizontalDistance(ScorchbeastEntity agent, LivingEntity target) {
        var dx = target.getX() - agent.getX();
        var dz = target.getZ() - agent.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    static void finishAttack(ScorchbeastEntity agent, CooldownTracker cooldowns) {
        cooldowns.set(ScorchbeastBrain.GLOBAL_COOLDOWN, 15 + agent.getRandom().nextInt(15));
    }

    boolean running() {
        return running;
    }

    @Override
    public final void start(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        running = true;
        begin(agent, blackboard, cooldowns);
    }

    @Override
    public final void stop(
        ScorchbeastEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns,
        ActionStatus reason
    ) {
        running = false;
        end(agent, blackboard, cooldowns, reason);
    }

    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {}

    void end(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {}

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public int priority() {
        return priority;
    }
}

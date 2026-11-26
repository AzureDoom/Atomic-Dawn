package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

abstract class MisterHandyTask implements Action<MisterHandyEntity, Void> {

    private final int priority;

    private final boolean interruptible;

    private boolean running;

    MisterHandyTask(int priority, boolean interruptible) {
        this.priority = priority;
        this.interruptible = interruptible;
    }

    @Nullable
    static LivingEntity target(Blackboard blackboard) {
        LivingEntity target = blackboard.get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(MisterHandyBlackboardKeys.TARGET_VISIBLE));
    }

    static boolean hunting(Blackboard blackboard) {
        return target(blackboard) != null;
    }

    static boolean attackReady(CooldownTracker cooldowns, String key) {
        return cooldowns.ready(key) && cooldowns.ready(MisterHandyBrain.GLOBAL_COOLDOWN);
    }

    static void finishAttack(CooldownTracker cooldowns) {
        cooldowns.set(MisterHandyBrain.GLOBAL_COOLDOWN, 8);
    }

    boolean running() {
        return running;
    }

    @Override
    public final void start(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        running = true;
        begin(agent, blackboard, cooldowns);
    }

    @Override
    public final void stop(
        MisterHandyEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns,
        ActionStatus reason
    ) {
        running = false;
        end(agent, blackboard, cooldowns, reason);
    }

    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {}

    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {}

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public int priority() {
        return priority;
    }
}

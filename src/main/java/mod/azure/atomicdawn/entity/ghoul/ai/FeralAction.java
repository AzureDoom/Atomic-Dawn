package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

abstract class FeralAction implements Action<FeralGhoulEntity, Void> {

    private final int priority;

    private final boolean interruptible;

    FeralAction(int priority, boolean interruptible) {
        this.priority = priority;
        this.interruptible = interruptible;
    }

    @Nullable
    static LivingEntity target(Blackboard blackboard) {
        LivingEntity target = blackboard.get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    @Override
    public void stop(
        FeralGhoulEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns,
        ActionStatus reason
    ) {}

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public int priority() {
        return priority;
    }
}

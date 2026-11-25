package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

final class ScorchbeastTree {

    private ScorchbeastTree() {}

    private static ActionNode<ScorchbeastEntity, Void> node(ScorchbeastTask task) {
        return new ActionNode<ScorchbeastEntity, Void>(task, task.priority());
    }

    private static BehaviorNode<ScorchbeastEntity, Void> gate(ScorchbeastTask task, Gate gate) {
        return new Condition<ScorchbeastEntity, Void>(
            (agent, blackboard, cooldowns) -> (!task.isInterruptible() && task.running()) || gate.test(
                agent,
                blackboard,
                cooldowns
            ),
            node(task)
        );
    }

    @FunctionalInterface
    private interface Gate {

        boolean test(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns);
    }

    static BehaviorNode<ScorchbeastEntity, Void> create() {
        return PrioritySelector.<ScorchbeastEntity, Void>of(
            node(new ScorchbeastIdleTask()),
            gate(
                new ScorchbeastPatrolTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastPatrolTask.ready(agent, blackboard)
            ),
            gate(
                new ScorchbeastCircleTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastCircleTask.ready(agent, blackboard)
            ),
            gate(
                new ScorchbeastGroundChaseTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastGroundChaseTask.ready(agent, blackboard)
            ),
            gate(
                new ScorchbeastLungeTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastLungeTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new ScorchbeastCropDustTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastCropDustTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new ScorchbeastSonicTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastSonicTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new ScorchbeastWingSweepTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastWingSweepTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new ScorchbeastShockwaveTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastShockwaveTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new ScorchbeastTakeoffTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastTakeoffTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new ScorchbeastLandTask(),
                (agent, blackboard, cooldowns) -> ScorchbeastLandTask.ready(agent, blackboard, cooldowns)
            )
        );
    }
}

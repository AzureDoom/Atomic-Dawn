package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

final class MisterHandyTree {

    private MisterHandyTree() {}

    private static ActionNode<MisterHandyEntity, Void> node(MisterHandyTask task) {
        return new ActionNode<MisterHandyEntity, Void>(task, task.priority());
    }

    private static BehaviorNode<MisterHandyEntity, Void> gate(MisterHandyTask task, Gate gate) {
        return new Condition<MisterHandyEntity, Void>(
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

        boolean test(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns);
    }

    static BehaviorNode<MisterHandyEntity, Void> create() {
        return PrioritySelector.<MisterHandyEntity, Void>of(
            node(new MisterHandyIdleTask()),
            gate(
                new MisterHandyPatrolTask(),
                (agent, blackboard, cooldowns) -> MisterHandyPatrolTask.ready(blackboard, cooldowns)
            ),
            gate(
                new MisterHandyInvestigateTask(),
                (agent, blackboard, cooldowns) -> MisterHandyInvestigateTask.ready(blackboard)
            ),
            gate(new MisterHandyChaseTask(), (agent, blackboard, cooldowns) -> MisterHandyChaseTask.ready(blackboard)),
            gate(
                new MisterHandyFlamerTask(),
                (agent, blackboard, cooldowns) -> MisterHandyFlamerTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new MisterHandyClawTask(),
                (agent, blackboard, cooldowns) -> MisterHandyClawTask.ready(agent, blackboard, cooldowns)
            ),
            gate(
                new MisterHandySawTask(),
                (agent, blackboard, cooldowns) -> MisterHandySawTask.ready(agent, blackboard, cooldowns)
            )
        );
    }
}

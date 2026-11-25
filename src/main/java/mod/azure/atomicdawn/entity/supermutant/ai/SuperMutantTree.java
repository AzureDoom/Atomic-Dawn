package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;

final class SuperMutantTree {

    private SuperMutantTree() {}

    private static ActionNode<SuperMutantEntity, Void> node(SuperMutantTask task) {
        return new ActionNode<SuperMutantEntity, Void>(task, task.priority());
    }

    static BehaviorNode<SuperMutantEntity, Void> create() {
        var idle = new SuperMutantIdleTask();
        var patrol = new SuperMutantPatrolTask();
        var home = new SuperMutantReturnHomeTask();
        var investigate = new SuperMutantInvestigateTask();
        var chase = new SuperMutantChaseTask();
        var ranged = new SuperMutantRangedTask();
        var assist = new SuperMutantAssistTask();
        var taunt = new SuperMutantRoarTask(false);
        var reload = new SuperMutantReloadTask();
        var melee = new SuperMutantMeleeAttackTask();
        var alert = new SuperMutantRoarTask(true);
        return PrioritySelector.<SuperMutantEntity, Void>of(
            node(idle),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantPatrolTask.ready(blackboard, cooldowns),
                node(patrol)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantReturnHomeTask.ready(agent, blackboard),
                node(home)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantInvestigateTask.ready(blackboard),
                node(investigate)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantChaseTask.ready(agent, blackboard),
                node(chase)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantRangedTask.ready(agent, blackboard),
                node(ranged)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantAssistTask.ready(blackboard),
                node(assist)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantRoarTask.tauntReady(agent, blackboard, cooldowns),
                node(taunt)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantReloadTask.ready(agent),
                node(reload)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantMeleeAttackTask.ready(agent, blackboard, cooldowns),
                node(melee)
            ),
            new Condition<SuperMutantEntity, Void>(
                (agent, blackboard, cooldowns) -> SuperMutantRoarTask.alertReady(agent, blackboard, cooldowns),
                node(alert)
            )
        );
    }
}

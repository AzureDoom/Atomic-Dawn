package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;

import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;

final class ScorchedTree {

    private ScorchedTree() {}

    private static ActionNode<ScorchedEntity, Void> node(ScorchedTask task) {
        return new ActionNode<ScorchedEntity, Void>(task, task.priority());
    }

    static BehaviorNode<ScorchedEntity, Void> create() {
        var idle = new ScorchedIdleTask();
        var patrol = new ScorchedPatrolTask();
        var sleep = new ScorchedEnterDormancyTask();
        var home = new ScorchedReturnHomeTask();
        var investigate = new ScorchedInvestigateTask();
        var chase = new ScorchedChaseTask();
        var ranged = new ScorchedRangedTask();
        var assist = new ScorchedAssistTask();
        var reload = new ScorchedReloadTask();
        var melee = new ScorchedMeleeAttackTask();
        var alert = new ScorchedAlertTask();
        var dormant = new ScorchedDormantTask();
        return PrioritySelector.<ScorchedEntity, Void>of(
            node(idle),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedPatrolTask.ready(agent, blackboard, cooldowns),
                node(patrol)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedEnterDormancyTask.ready(agent, blackboard),
                node(sleep)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedReturnHomeTask.ready(agent, blackboard),
                node(home)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedInvestigateTask.ready(agent, blackboard),
                node(investigate)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedChaseTask.ready(agent, blackboard),
                node(chase)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedRangedTask.ready(agent, blackboard),
                node(ranged)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedAssistTask.ready(agent, blackboard),
                node(assist)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedReloadTask.ready(agent),
                node(reload)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedMeleeAttackTask.ready(agent, blackboard, cooldowns),
                node(melee)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> ScorchedAlertTask.ready(agent, blackboard, cooldowns),
                node(alert)
            ),
            new Condition<ScorchedEntity, Void>(
                (agent, blackboard, cooldowns) -> agent.isDormant(),
                node(dormant)
            )
        );
    }
}

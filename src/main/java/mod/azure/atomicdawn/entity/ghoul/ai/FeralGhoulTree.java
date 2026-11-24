package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;

import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralGhoulTree {

    private FeralGhoulTree() {}

    private static boolean hunting(FeralGhoulEntity agent, Blackboard blackboard) {
        return !agent.isDormant() && FeralAction.target(blackboard) != null;
    }

    static BehaviorNode<FeralGhoulEntity, Void> create() {
        var idle = new FeralIdleAction();
        var wander = new FeralWanderAction();
        var dormant = new FeralDormantAction();
        var scream = new FeralScreamAction();
        var chase = new FeralChaseAction();
        var claw = new FeralClawAction();
        var leap = new FeralLeapAction();
        return PrioritySelector.<FeralGhoulEntity, Void>of(
            new ActionNode<FeralGhoulEntity, Void>(idle, idle.priority()),
            new Condition<FeralGhoulEntity, Void>(
                (agent, blackboard, cooldowns) -> !agent.isDormant() && FeralAction.target(blackboard) == null
                    && cooldowns.ready(FeralGhoulBrain.WANDER_COOLDOWN),
                new ActionNode<FeralGhoulEntity, Void>(wander, wander.priority())
            ),
            new Condition<FeralGhoulEntity, Void>(
                (agent, blackboard, cooldowns) -> hunting(agent, blackboard),
                new ActionNode<FeralGhoulEntity, Void>(chase, chase.priority())
            ),
            new Condition<FeralGhoulEntity, Void>(
                (agent, blackboard, cooldowns) -> hunting(agent, blackboard)
                    && cooldowns.ready(FeralGhoulBrain.LEAP_COOLDOWN)
                    && FeralLeapAction.canLeap(agent, FeralAction.target(blackboard)),
                new ActionNode<FeralGhoulEntity, Void>(leap, leap.priority())
            ),
            new Condition<FeralGhoulEntity, Void>(
                (agent, blackboard, cooldowns) -> hunting(agent, blackboard)
                    && cooldowns.ready(FeralGhoulBrain.CLAW_COOLDOWN)
                    && agent.inClawRange(FeralAction.target(blackboard)),
                new ActionNode<FeralGhoulEntity, Void>(claw, claw.priority())
            ),
            new Condition<FeralGhoulEntity, Void>(
                (agent, blackboard, cooldowns) -> hunting(agent, blackboard)
                    && cooldowns.ready(FeralGhoulBrain.SCREAM_COOLDOWN),
                new ActionNode<FeralGhoulEntity, Void>(scream, scream.priority())
            ),
            new Condition<FeralGhoulEntity, Void>(
                (agent, blackboard, cooldowns) -> agent.isDormant(),
                new ActionNode<FeralGhoulEntity, Void>(dormant, dormant.priority())
            )
        );
    }
}

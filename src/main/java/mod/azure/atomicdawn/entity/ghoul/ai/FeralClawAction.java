package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

import mod.azure.atomicdawn.entity.ghoul.FeralGhoulAction;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralClawAction extends FeralAction {

    private static final int WINDUP = 5;

    private static final int DURATION = 10;

    private static final int COOLDOWN = 12;

    private int ticks;

    private boolean left;

    FeralClawAction() {
        super(60, false);
    }

    @Override
    public void start(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        left = !left;
        agent.getNavigation().stop();
        agent.playAction(left ? FeralGhoulAction.CLAW_LEFT : FeralGhoulAction.CLAW_RIGHT);
        agent.attackCry();
    }

    @Override
    public ActionOutcome<Void> tick(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null) {
            cooldowns.set(FeralGhoulBrain.CLAW_COOLDOWN, COOLDOWN);
            return ActionOutcome.failed();
        }
        agent.getLookControl().setLookAt(target, 30F, 30F);
        ticks++;
        if (ticks == WINDUP) {
            cooldowns.set(FeralGhoulBrain.CLAW_COOLDOWN, COOLDOWN);
            if (
                agent.level() instanceof ServerLevel level && agent.inClawRange(target) && agent.hasLineOfSight(
                    target
                )
            ) {
                agent.swing(left ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
                agent.doHurtTarget(level, target);
            }
        }
        return ticks >= DURATION ? ActionOutcome.success() : ActionOutcome.running();
    }
}

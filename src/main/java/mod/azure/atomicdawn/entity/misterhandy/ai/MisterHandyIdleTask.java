package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;
import mod.azure.atomicdawn.registry.ADSounds;

final class MisterHandyIdleTask extends MisterHandyTask {

    private int scanTicks;

    MisterHandyIdleTask() {
        super(0, true);
    }

    @Override
    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        scanTicks = 0;
    }

    @Override
    public ActionOutcome<Void> tick(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var random = agent.getRandom();
        if (scanTicks > 0) {
            if (--scanTicks == 0) {
                agent.setScanning(false);
            }
        } else if (random.nextInt(300) == 0) {
            scanTicks = 80;
            agent.setScanning(true);
            agent.playSound(ADSounds.MISTER_HANDY_SCAN.get(), 0.8F, 1.0F);
        } else if (random.nextInt(80) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (random.nextFloat() - 0.5F) * 90F);
        }
        return ActionOutcome.running();
    }

    @Override
    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.setScanning(false);
    }
}

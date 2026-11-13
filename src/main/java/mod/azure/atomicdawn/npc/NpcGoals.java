package mod.azure.atomicdawn.npc;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class NpcGoals {

    private static final double WALK_SPEED = 0.6;

    private NpcGoals() {}

    public static final class TalkGoal extends Goal {

        private final NpcEntity npc;

        public TalkGoal(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return npc.talkingPlayer() != null;
        }

        @Override
        public void start() {
            npc.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            var player = npc.talkingPlayer();
            if (player != null) {
                npc.getLookControl().setLookAt(player, 30F, 30F);
            }
        }
    }

    public static final class ReturnHomeGoal extends Goal {

        private final NpcEntity npc;

        public ReturnHomeGoal(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private double limit() {
            var behavior = npc.config().behavior();
            if (behavior.movement() == NpcBehavior.Movement.STATIONARY) {
                return 1.5;
            }
            return behavior.wanderRadius() + 2.0;
        }

        @Override
        public boolean canUse() {
            var home = npc.home();
            if (home == null || npc.hasOwner() || npc.talkingPlayer() != null) {
                return false;
            }
            var limit = limit();
            return home.distToCenterSqr(npc.position()) > limit * limit;
        }

        @Override
        public void start() {
            var home = npc.home();
            if (home != null) {
                npc.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, WALK_SPEED);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.hasOwner() && !npc.getNavigation().isDone() && npc.talkingPlayer() == null;
        }
    }

    public static final class WanderGoal extends Goal {

        private final NpcEntity npc;

        public WanderGoal(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return npc.home() != null && !npc.hasOwner() && npc.talkingPlayer() == null && npc.getRandom()
                .nextInt(120) == 0;
        }

        @Override
        public void start() {
            var home = npc.home();
            if (home == null) {
                return;
            }
            var radius = npc.config().behavior().wanderRadius();
            var random = npc.getRandom();
            var x = home.getX() + random.nextInt(radius * 2 + 1) - radius + 0.5;
            var z = home.getZ() + random.nextInt(radius * 2 + 1) - radius + 0.5;
            npc.getNavigation().moveTo(x, home.getY(), z, WALK_SPEED);
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.hasOwner() && !npc.getNavigation().isDone() && npc.talkingPlayer() == null;
        }
    }
}

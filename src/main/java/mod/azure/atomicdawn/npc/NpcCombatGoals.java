package mod.azure.atomicdawn.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;

public final class NpcCombatGoals {

    private NpcCombatGoals() {}

    public static final class MeleeGoal extends MeleeAttackGoal {

        private final NpcEntity npc;

        public MeleeGoal(NpcEntity npc) {
            super(npc, 1.1, true);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return npc.canFight() && NpcWeapons.kind(npc) == NpcWeapons.Kind.NONE && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return npc.canFight() && NpcWeapons.kind(npc) == NpcWeapons.Kind.NONE && super.canContinueToUse();
        }
    }

    public static final class RangedGoal extends Goal {

        private static final int BURST_PAUSE = 18;

        private final NpcEntity npc;

        private int cooldown;

        private int burstShots;

        private int magazine;

        private int reloadTicks;

        private int seeTime;

        public RangedGoal(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean valid() {
            var target = npc.getTarget();
            return npc.canFight() && target != null && target.isAlive() && NpcWeapons.kind(npc) != NpcWeapons.Kind.NONE;
        }

        @Override
        public boolean canUse() {
            return valid();
        }

        @Override
        public boolean canContinueToUse() {
            return valid();
        }

        @Override
        public void start() {
            npc.setAggressive(true);
            magazine = NpcWeapons.magazine(npc);
            cooldown = 10;
            burstShots = 0;
        }

        @Override
        public void stop() {
            npc.setAggressive(false);
            seeTime = 0;
            npc.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            var target = npc.getTarget();
            if (target == null) {
                return;
            }
            var distance = npc.distanceToSqr(target);
            var range = npc.config().combat().rangedRange();
            var canSee = npc.getSensing().hasLineOfSight(target);
            seeTime = canSee ? seeTime + 1 : 0;
            if (!canSee || distance > range * range * 0.75) {
                npc.getNavigation().moveTo(target, 1.0);
            } else {
                npc.getNavigation().stop();
            }
            npc.getLookControl().setLookAt(target, 30F, 30F);
            if (reloadTicks > 0) {
                if (--reloadTicks == 0) {
                    magazine = NpcWeapons.magazine(npc);
                }
                return;
            }
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (!canSee || seeTime < 5 || distance > range * range) {
                return;
            }
            NpcWeapons.fire(npc, target);
            npc.animate("shoot");
            magazine--;
            burstShots++;
            cooldown = NpcWeapons.interval(npc);
            if (burstShots >= NpcWeapons.burst(npc)) {
                burstShots = 0;
                cooldown += NpcWeapons.burst(npc) > 1 ? BURST_PAUSE : 0;
            }
            if (magazine <= 0) {
                reloadTicks = NpcWeapons.reloadTicks(npc);
                npc.animate("reload");
            }
        }
    }

    public static final class FollowOwnerGoal extends Goal {

        private static final double START_DISTANCE = 6.0;

        private static final double STOP_DISTANCE = 3.0;

        private static final double TELEPORT_DISTANCE = 24.0;

        private final NpcEntity npc;

        private int recalculate;

        public FollowOwnerGoal(NpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            var owner = npc.ownerPlayer();
            return owner != null && npc.companionMode() == NpcEntity.CompanionMode.FOLLOW && !owner.isSpectator()
                && npc.distanceToSqr(owner) > START_DISTANCE * START_DISTANCE
                && (npc.getTarget() == null || npc.distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE);
        }

        @Override
        public boolean canContinueToUse() {
            var owner = npc.ownerPlayer();
            return owner != null && npc.companionMode() == NpcEntity.CompanionMode.FOLLOW
                && npc.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE && !npc.getNavigation().isDone();
        }

        @Override
        public void start() {
            recalculate = 0;
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            var owner = npc.ownerPlayer();
            if (owner == null) {
                return;
            }
            npc.getLookControl().setLookAt(owner, 10F, npc.getMaxHeadXRot());
            if (--recalculate > 0) {
                return;
            }
            recalculate = 10;
            if (npc.distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                teleportNear(npc, owner);
            } else {
                npc.getNavigation().moveTo(owner, 1.15);
            }
        }

        private static void teleportNear(NpcEntity npc, LivingEntity owner) {
            var origin = owner.blockPosition();
            var random = npc.getRandom();
            for (var attempt = 0; attempt < 12; attempt++) {
                var pos = origin.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
                if (safe(npc, pos)) {
                    npc.getNavigation().stop();
                    npc.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    return;
                }
            }
        }

        private static boolean safe(NpcEntity npc, BlockPos pos) {
            var level = npc.level();
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return false;
            }
            var box = npc.getBoundingBox()
                .move(pos.getX() + 0.5 - npc.getX(), pos.getY() - npc.getY(), pos.getZ() + 0.5 - npc.getZ());
            return level.noCollision(npc, box);
        }
    }

    public static final class DefendOwnerGoal extends TargetGoal {

        private final NpcEntity npc;

        private int timestamp;

        private LivingEntity attacker;

        public DefendOwnerGoal(NpcEntity npc) {
            super(npc, false);
            this.npc = npc;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            var owner = npc.ownerPlayer();
            if (owner == null || !npc.canFight()) {
                return false;
            }
            attacker = owner.getLastHurtByMob();
            return attacker != null && owner.getLastHurtByMobTimestamp() != timestamp && !npc.isAlly(attacker)
                && canAttack(attacker, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            npc.setTarget(attacker);
            var owner = npc.ownerPlayer();
            if (owner != null) {
                timestamp = owner.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    public static final class AssistOwnerGoal extends TargetGoal {

        private final NpcEntity npc;

        private int timestamp;

        private LivingEntity victim;

        public AssistOwnerGoal(NpcEntity npc) {
            super(npc, false);
            this.npc = npc;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            var owner = npc.ownerPlayer();
            if (owner == null || !npc.canFight()) {
                return false;
            }
            victim = owner.getLastHurtMob();
            return victim != null && owner.getLastHurtMobTimestamp() != timestamp && !npc.isAlly(victim)
                && canAttack(victim, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            npc.setTarget(victim);
            var owner = npc.ownerPlayer();
            if (owner != null) {
                timestamp = owner.getLastHurtMobTimestamp();
            }
            super.start();
        }
    }
}

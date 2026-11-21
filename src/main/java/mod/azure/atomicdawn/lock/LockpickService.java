package mod.azure.atomicdawn.lock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.LockpickOpenPayload;
import mod.azure.atomicdawn.network.LockpickStatePayload;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.quest.QuestActions;
import mod.azure.atomicdawn.special.SpecialService;

public final class LockpickService {

    private static final long SESSION_TICKS = 6000L;

    private static final long PROBE_COOLDOWN = 2L;

    private static final long STRAIN_COOLDOWN = 3L;

    private static final long MIN_TURN_TICKS = 4L;

    private static final double REACH_SQR = 8.0 * 8.0;

    private static final float SWEET_SPOT_RANGE = 80F;

    private static final float MIN_TURN = 0.05F;

    private static final class Session {

        final ResourceKey<Level> dimension;

        final BlockPos pos;

        final LockLevel level;

        final float sweetSpot;

        final float fullWidth;

        final float partialWidth;

        final float strainDamage;

        final long expiresAt;

        float pinHealth = 1F;

        float lastMax;

        long lastProbe = -1L;

        long lastStrain = -1L;

        Session(
            ResourceKey<Level> dimension,
            BlockPos pos,
            LockLevel level,
            float sweetSpot,
            float fullWidth,
            float partialWidth,
            float strainDamage,
            long expiresAt
        ) {
            this.dimension = dimension;
            this.pos = pos;
            this.level = level;
            this.sweetSpot = sweetSpot;
            this.fullWidth = fullWidth;
            this.partialWidth = partialWidth;
            this.strainDamage = strainDamage;
            this.expiresAt = expiresAt;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private LockpickService() {}

    public static int tier(ServerPlayer player) {
        return PerkHooks.lockpickTier(player);
    }

    public static void begin(ServerPlayer player, LockService.Located located) {
        var lock = located.lock();
        var config = AtomicDawn.CONFIG.locks;
        if (lock.owner().isPresent() && !config.LOCKS_ALLOW_PICKING_PLAYER_LOCKS) {
            LockService.notify(player, "lock.atomicdawn.locked_owned", ChatFormatting.RED);
            denied(player, located.pos());
            return;
        }
        var required = lock.level().requiredTier();
        var extra = 0;
        if (config.LOCKPICK_REQUIRE_PERKS) {
            var tier = tier(player);
            if (tier < required) {
                LockService.notify(
                    player,
                    "lock.atomicdawn.requires_perk",
                    ChatFormatting.RED,
                    lock.level().component(),
                    required
                );
                denied(player, located.pos());
                return;
            }
            extra = Math.min(2, tier - required);
        }
        var pins = BobbyPins.count(player);
        if (pins == 0) {
            LockService.notify(player, "lock.atomicdawn.no_pins", ChatFormatting.RED, lock.level().component());
            denied(player, located.pos());
            return;
        }
        var level = player.level();
        var random = player.getRandom();
        var scale = (float) config.LOCKPICK_SWEET_SPOT_SCALE * (1F + 0.25F * extra);
        var full = Math.min(60F, lock.level().fullWidth() * scale);
        var partial = Math.clamp(lock.level().partialWidth() * scale, full + 4F, 120F);
        var strain = lock.level().strainDamage() / (float) Math.max(0.05, config.LOCKPICK_PIN_DURABILITY_SCALE);
        var session = new Session(
            level.dimension(),
            located.pos().immutable(),
            lock.level(),
            (random.nextFloat() * 2F - 1F) * SWEET_SPOT_RANGE,
            full,
            partial,
            strain,
            level.getGameTime() + SESSION_TICKS
        );
        SESSIONS.put(player.getUUID(), session);
        ADNetwork.sendTo(player, new LockpickOpenPayload(session.pos, lock.level().ordinal(), pins));
    }

    public static void probe(ServerPlayer player, float angle) {
        var session = valid(player);
        if (session == null) {
            return;
        }
        var now = player.level().getGameTime();
        if (!Float.isFinite(angle) || (session.lastProbe >= 0L && now - session.lastProbe < PROBE_COOLDOWN)) {
            reply(player, session, LockpickStatePayload.Status.REJECTED, 0F);
            return;
        }
        var distance = Math.abs(Math.clamp(angle, -90F, 90F) - session.sweetSpot);
        float max;
        if (distance <= session.fullWidth) {
            max = 1F;
        } else if (distance >= session.partialWidth) {
            max = MIN_TURN;
        } else {
            var t = (distance - session.fullWidth) / (session.partialWidth - session.fullWidth);
            max = Math.clamp(1F - t, MIN_TURN, 0.95F);
        }
        session.lastMax = max;
        session.lastProbe = now;
        reply(player, session, LockpickStatePayload.Status.PROBE, max);
    }

    public static void strain(ServerPlayer player) {
        var session = valid(player);
        if (session == null || session.lastProbe < 0L || session.lastMax >= 1F) {
            return;
        }
        var level = player.level();
        var now = level.getGameTime();
        if (session.lastStrain >= 0L && now - session.lastStrain < STRAIN_COOLDOWN) {
            return;
        }
        session.lastStrain = now;
        session.pinHealth -= session.strainDamage;
        if (session.pinHealth > 0F) {
            reply(player, session, LockpickStatePayload.Status.STRAIN, session.lastMax);
            return;
        }
        BobbyPins.consume(player);
        level.playSound(null, session.pos, SoundEvents.TRIPWIRE_DETACH, SoundSource.PLAYERS, 0.8F, 1.6F);
        session.pinHealth = 1F;
        session.lastMax = 0F;
        session.lastProbe = -1L;
        session.lastStrain = -1L;
        if (BobbyPins.count(player) == 0) {
            SESSIONS.remove(player.getUUID());
            LockService.notify(player, "lock.atomicdawn.out_of_pins", ChatFormatting.RED);
            reply(player, session, LockpickStatePayload.Status.FAILED, 0F);
            return;
        }
        reply(player, session, LockpickStatePayload.Status.PIN_BROKEN, 0F);
    }

    public static void open(ServerPlayer player) {
        var session = valid(player);
        if (session == null) {
            return;
        }
        var level = player.level();
        var now = level.getGameTime();
        if (session.lastProbe < 0L || session.lastMax < 1F || now - session.lastProbe < MIN_TURN_TICKS) {
            reply(player, session, LockpickStatePayload.Status.REJECTED, 0F);
            return;
        }
        SESSIONS.remove(player.getUUID());
        LockService.grantAccess(level, session.pos, player.getUUID());
        var xp = (long) AtomicDawn.CONFIG.locks.LOCKPICK_XP * (session.level.ordinal() + 1L);
        if (xp > 0L) {
            SpecialService.awardXp(player, xp);
        }
        level.playSound(null, session.pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.3F);
        LockService.notify(player, "lock.atomicdawn.picked", ChatFormatting.GREEN, session.level.component());
        reply(player, session, LockpickStatePayload.Status.SUCCESS, 1F);
        QuestActions.trigger(player, QuestActions.PICK_LOCK);
    }

    public static void cancel(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static void forget(UUID id) {
        SESSIONS.remove(id);
    }

    private static @Nullable Session valid(ServerPlayer player) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null) {
            ADNetwork.sendTo(player, new LockpickStatePayload(LockpickStatePayload.Status.FAILED, 0F, 0, 0F));
            return null;
        }
        var level = player.level();
        var stillValid = level.dimension().equals(session.dimension) && level.getGameTime() <= session.expiresAt
            && player.isAlive() && player.distanceToSqr(Vec3.atCenterOf(session.pos)) <= REACH_SQR
            && BobbyPins.available(player);
        if (stillValid) {
            var located = LockService.find(level, session.pos);
            stillValid = located.isPresent() && located.get().pos().equals(session.pos)
                && located.get().lock().level() == session.level;
        }
        if (!stillValid) {
            SESSIONS.remove(player.getUUID());
            reply(player, session, LockpickStatePayload.Status.FAILED, 0F);
            return null;
        }
        return session;
    }

    private static void denied(ServerPlayer player, BlockPos pos) {
        player.level().playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8F, 1F);
    }

    private static void reply(ServerPlayer player, Session session, LockpickStatePayload.Status status, float max) {
        ADNetwork.sendTo(
            player,
            new LockpickStatePayload(status, max, BobbyPins.count(player), Math.max(0F, session.pinHealth))
        );
    }
}

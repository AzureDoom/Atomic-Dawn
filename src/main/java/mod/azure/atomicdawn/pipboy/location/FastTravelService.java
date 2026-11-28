package mod.azure.atomicdawn.pipboy.location;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.pipboy.PipBoyNames;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class FastTravelService {

    public static final PermissionNode<Boolean> PERMISSION = new PermissionNode<>(
        AtomicDawn.MOD_ID,
        "fast_travel",
        PermissionTypes.BOOLEAN,
        (player, uuid, context) -> player != null && defaultAllowed(player)
    );

    private static final int SEARCH_RADIUS = 8;

    private static final int SEARCH_HEIGHT = 32;

    private static final Map<UUID, Long> LAST_TRAVEL = new ConcurrentHashMap<>();

    private FastTravelService() {}

    @SubscribeEvent
    public static void onPermissionNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(PERMISSION);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_TRAVEL.remove(event.getEntity().getUUID());
    }

    public static boolean enabled() {
        return AtomicDawn.CONFIG.common.pipboy.fastTravel.FAST_TRAVEL_ENABLED;
    }

    private static boolean defaultAllowed(ServerPlayer player) {
        return !AtomicDawn.CONFIG.common.pipboy.fastTravel.FAST_TRAVEL_OP_ONLY || Commands.hasPermission(
            Commands.LEVEL_GAMEMASTERS
        ).test(player.createCommandSourceStack());
    }

    public static boolean allowed(ServerPlayer player) {
        return enabled() && PermissionAPI.getPermission(player, PERMISSION);
    }

    public static void travel(ServerPlayer player, DiscoveredLocation requested) {
        var config = AtomicDawn.CONFIG.common.pipboy.fastTravel;
        if (!enabled()) {
            fail(player, "pipboy.atomicdawn.fast_travel.disabled");
            return;
        }
        if (!PermissionAPI.getPermission(player, PERMISSION)) {
            fail(player, "pipboy.atomicdawn.fast_travel.no_permission");
            return;
        }
        if (player.isSpectator() || !player.isAlive() || player.isSleeping()) {
            return;
        }
        var location = discovered(player, requested);
        if (location == null) {
            fail(player, "pipboy.atomicdawn.fast_travel.unknown");
            return;
        }
        if (location.dimension() != player.level().dimension() && !config.FAST_TRAVEL_CROSS_DIMENSION) {
            fail(player, "pipboy.atomicdawn.fast_travel.dimension");
            return;
        }
        var now = player.level().getGameTime();
        var last = LAST_TRAVEL.get(player.getUUID());
        var cooldown = config.FAST_TRAVEL_COOLDOWN_SECONDS * 20L;
        if (last != null && now >= last && now - last < cooldown && !player.isCreative()) {
            fail(player, "pipboy.atomicdawn.fast_travel.cooldown", (cooldown - (now - last) + 19) / 20);
            return;
        }
        var combat = config.FAST_TRAVEL_COMBAT_SECONDS * 20;
        if (
            combat > 0 && !player.isCreative() && player.getLastHurtByMob() != null
                && player.tickCount - player.getLastHurtByMobTimestamp() < combat
        ) {
            fail(player, "pipboy.atomicdawn.fast_travel.combat");
            return;
        }
        var server = player.level().getServer();
        var level = server == null ? null : server.getLevel(location.dimension());
        if (level == null) {
            fail(player, "pipboy.atomicdawn.fast_travel.dimension");
            return;
        }
        var name = PipBoyNames.location(location.structure());
        var landing = landing(level, location.pos());
        if (landing == null) {
            fail(player, "pipboy.atomicdawn.fast_travel.no_landing", name);
            return;
        }
        if (
            player.teleportTo(
                level,
                landing.x,
                landing.y,
                landing.z,
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            )
        ) {
            LAST_TRAVEL.put(player.getUUID(), level.getGameTime());
            player.sendSystemMessage(Component.translatable("pipboy.atomicdawn.fast_travel.arrived", name), true);
        }
    }

    @Nullable
    private static DiscoveredLocation discovered(ServerPlayer player, DiscoveredLocation requested) {
        for (var location : LocationService.log(player).discovered()) {
            if (
                location.structure().equals(requested.structure()) && location.dimension() == requested.dimension()
                    && location.pos().equals(requested.pos())
            ) {
                return location;
            }
        }
        return null;
    }

    @Nullable
    private static Vec3 landing(ServerLevel level, BlockPos center) {
        if (!level.getWorldBorder().isWithinBounds(center)) {
            return null;
        }
        level.getChunk(center.getX() >> 4, center.getZ() >> 4);
        var ceiling = level.dimensionType().hasCeiling();
        for (var radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (var dx = -radius; dx <= radius; dx++) {
                for (var dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    var column = center.offset(dx, 0, dz);
                    var spot = ceiling ? scan(level, column) : surface(level, column);
                    if (spot != null) {
                        return Vec3.atBottomCenterOf(spot);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos surface(ServerLevel level, BlockPos column) {
        var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        var pos = new BlockPos(column.getX(), y, column.getZ());
        return standable(level, pos) ? pos : null;
    }

    @Nullable
    private static BlockPos scan(ServerLevel level, BlockPos column) {
        for (var dy = 0; dy <= SEARCH_HEIGHT; dy++) {
            var up = column.above(dy);
            if (standable(level, up)) {
                return up;
            }
            var down = column.below(dy);
            if (dy > 0 && standable(level, down)) {
                return down;
            }
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY() || pos.getY() + 1 >= level.getMaxY()) {
            return false;
        }
        var below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        return clear(level, pos) && clear(level, pos.above());
    }

    private static boolean clear(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private static void fail(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
    }
}

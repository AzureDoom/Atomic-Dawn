package mod.azure.atomicdawn.pipboy.light;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgradeEvents;
import mod.azure.atomicdawn.registry.ADBlocks;

public final class PipBoyLightService {

    private record Lit(
        ResourceKey<Level> dimension,
        @Nullable BlockPos pos
    ) {}

    private static final Map<UUID, Lit> LIT = new HashMap<>();

    private PipBoyLightService() {}

    public static boolean isOn(ServerPlayer player) {
        return LIT.containsKey(player.getUUID());
    }

    public static void toggle(ServerPlayer player) {
        if (isOn(player)) {
            turnOff(player);
        } else if (allowed(player)) {
            LIT.put(player.getUUID(), new Lit(player.level().dimension(), null));
            feedback(player, true);
            tick(player);
        }
    }

    public static void turnOff(ServerPlayer player) {
        var lit = LIT.remove(player.getUUID());
        if (lit == null) {
            return;
        }
        if (lit.pos() != null) {
            player.level();
            var level = player.level().getServer().getLevel(lit.dimension());
            if (level != null) {
                remove(level, lit.pos());
            }
        }
        feedback(player, false);
    }

    public static void drop(ServerPlayer player) {
        var lit = LIT.remove(player.getUUID());
        if (lit != null && lit.pos() != null) {
            player.level();
            var level = player.level().getServer().getLevel(lit.dimension());
            if (level != null) {
                remove(level, lit.pos());
            }
        }
    }

    private static boolean allowed(ServerPlayer player) {
        if (player.isSpectator()) {
            return false;
        }
        return PowerArmorUpgradeEvents.headlamp(player) > 0
            || (AtomicDawn.CONFIG.pipboy.PIPBOY_ENABLED && AtomicDawn.CONFIG.pipboy.PIPBOY_LIGHT_ENABLED);
    }

    private static int level(ServerPlayer player) {
        var headlamp = PowerArmorUpgradeEvents.headlamp(player);
        return Math.clamp(headlamp > 0 ? headlamp : AtomicDawn.CONFIG.pipboy.PIPBOY_LIGHT_LEVEL, 1, 15);
    }

    public static boolean claims(ServerLevel level, BlockPos pos) {
        for (var lit : LIT.values()) {
            if (pos.equals(lit.pos()) && lit.dimension().equals(level.dimension())) {
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerPlayer player) {
        var lit = LIT.get(player.getUUID());
        if (lit == null) {
            return;
        }
        if (!allowed(player) || !player.isAlive()) {
            turnOff(player);
            return;
        }
        var level = player.level();
        var target = target(level, player);
        if (lit.dimension().equals(level.dimension()) && target != null && target.equals(lit.pos())) {
            return;
        }
        LIT.put(player.getUUID(), new Lit(level.dimension(), target));
        if (lit.pos() != null) {
            player.level();
            var old = player.level().getServer().getLevel(lit.dimension());
            if (old != null) {
                remove(old, lit.pos());
            }
        }
        if (target != null) {
            var current = level.getBlockState(target);
            var waterlogged = current.is(ADBlocks.PIPBOY_LIGHT.get())
                ? current.getValue(PipBoyLightBlock.WATERLOGGED)
                : current.getFluidState().is(Fluids.WATER);
            var state = ADBlocks.PIPBOY_LIGHT.get()
                .defaultBlockState()
                .setValue(PipBoyLightBlock.LEVEL, level(player))
                .setValue(PipBoyLightBlock.WATERLOGGED, waterlogged);
            if (!current.equals(state)) {
                level.setBlock(target, state, Block.UPDATE_ALL);
            }
        }
    }

    @Nullable
    private static BlockPos target(ServerLevel level, ServerPlayer player) {
        var eye = BlockPos.containing(player.getEyePosition());
        for (var pos : new BlockPos[] { eye, player.blockPosition(), eye.above() }) {
            if (level.isInWorldBounds(pos) && usable(level.getBlockState(pos))) {
                return pos;
            }
        }
        return null;
    }

    private static boolean usable(BlockState state) {
        return state.isAir() || state.is(ADBlocks.PIPBOY_LIGHT.get())
            || (state.is(Blocks.WATER) && state.getFluidState().isSource());
    }

    private static void remove(ServerLevel level, BlockPos pos) {
        if (claims(level, pos)) {
            return;
        }
        var state = level.getBlockState(pos);
        if (state.is(ADBlocks.PIPBOY_LIGHT.get())) {
            level.setBlock(pos, PipBoyLightBlock.emptied(state), Block.UPDATE_ALL);
        }
    }

    private static void feedback(ServerPlayer player, boolean on) {
        player.level()
            .playSound(
                null,
                player.blockPosition(),
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.PLAYERS,
                0.4F,
                on ? 1.4F : 1.1F
            );
        player.sendSystemMessage(
            Component.translatable(on ? "pipboy.atomicdawn.light.on" : "pipboy.atomicdawn.light.off"),
            true
        );
    }
}

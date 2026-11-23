package mod.azure.atomicdawn.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import mod.azure.atomicdawn.registry.ADBlockEntities;

@SuppressWarnings("unused")
public class VaultDoorBlockEntity extends BlockEntity {

    public static final int EVENT_ANIMATE = 1;

    @Nullable
    private Boolean shownOpen;

    private long lastToggle = Long.MIN_VALUE;

    private int animationTick = -1;

    private boolean opening;

    private boolean swept;

    private final List<BlockPos> barriers = new ArrayList<>();

    public VaultDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ADBlockEntities.VAULT_DOOR.get(), pos, state);
    }

    public boolean tryToggle(long gameTime) {
        if (animationTick >= 0) {
            return false;
        }
        if (lastToggle != Long.MIN_VALUE && gameTime - lastToggle < VaultDoorBlock.ANIMATION_TICKS) {
            return false;
        }
        lastToggle = gameTime;
        return true;
    }

    public void start(boolean opening) {
        this.opening = opening;
        this.animationTick = 0;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != EVENT_ANIMATE) {
            return super.triggerEvent(id, param);
        }
        if (level != null && level.isClientSide()) {
            if (param == 1) {
                VaultDoorAnimations.open(this);
            } else {
                VaultDoorAnimations.close(this);
            }
            shownOpen = param == 1;
        }
        return true;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, VaultDoorBlockEntity door) {
        if (door.shownOpen == null) {
            boolean open = state.getValue(VaultDoorBlock.OPEN);
            VaultDoorAnimations.snap(door, open);
            door.shownOpen = open;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VaultDoorBlockEntity door) {
        var facing = state.getValue(VaultDoorBlock.FACING);
        if (!door.swept) {
            door.swept = true;
            sweep(level, pos, facing);
            if (state.getValue(VaultDoorBlock.OPEN)) {
                door.syncBarriers(level, VaultDoorBlock.doorCells(pos, facing, 1, VaultDoorBlock.SIZE));
            }
        }
        if (door.animationTick < 0) {
            return;
        }
        var tick = door.animationTick++;
        var inward = VaultDoorBlock.inward(door.opening, tick);
        var lateral = VaultDoorBlock.lateral(door.opening, tick);
        var passable = inward > 0;
        VaultDoorBlock.setOpen(level, pos, facing, passable);
        door.syncBarriers(level, passable ? VaultDoorBlock.doorCells(pos, facing, inward, lateral) : List.of());
        if (tick >= VaultDoorBlock.ANIMATION_TICKS) {
            door.animationTick = -1;
        }
    }

    private void syncBarriers(Level level, List<BlockPos> desired) {
        var wanted = new HashSet<>(desired);
        barriers.removeIf(pos -> {
            if (wanted.contains(pos)) {
                return false;
            }
            if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return true;
        });
        for (var pos : desired) {
            if (barriers.contains(pos) || !level.getBlockState(pos).isAir()) {
                continue;
            }
            if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
                continue;
            }
            level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_CLIENTS);
            barriers.add(pos);
        }
    }

    public void clearBarriers(Level level) {
        syncBarriers(level, List.of());
    }

    public static void sweep(Level level, BlockPos master, Direction facing) {
        for (var pos : VaultDoorBlock.sweptCells(master, facing)) {
            if (level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}

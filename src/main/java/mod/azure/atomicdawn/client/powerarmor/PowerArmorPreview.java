package mod.azure.atomicdawn.client.powerarmor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.helper.ADEntityUtil;
import mod.azure.atomicdawn.powerarmor.PowerArmorChassisItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlock;
import mod.azure.atomicdawn.registry.ADEntities;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class PowerArmorPreview {

    private static final int GHOST_ID = Integer.MIN_VALUE + 7331;

    @Nullable
    private static PowerArmorEntity ghost;

    private static ItemStack contents = ItemStack.EMPTY;

    private PowerArmorPreview() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (ghost != null && ghost.level() != level) {
            ghost = null;
            contents = ItemStack.EMPTY;
        }
        if (level == null || player == null) {
            return;
        }
        var stack = chassis(player);
        var hit = stack.isEmpty() || player.isSpectator() || player.getVehicle() instanceof PowerArmorEntity
            ? null
            : target(minecraft, level);
        if (hit == null) {
            remove(level);
            return;
        }
        var preview = ghost(level);
        if (preview == null) {
            return;
        }
        if (!ItemStack.matches(stack, contents)) {
            for (var i = 0; i < PowerArmorEntity.SLOT_COUNT; i++) {
                preview.setSlotItem(i, ItemStack.EMPTY);
            }
            PowerArmorChassisItem.applyContents(preview, stack);
            preview.setCustomNameVisible(false);
            contents = stack.copy();
        }
        var spawn = hit.getBlockPos().relative(hit.getDirection());
        var x = spawn.getX() + 0.5;
        var y = (double) spawn.getY();
        var z = spawn.getZ() + 0.5;
        var yaw = player.getYRot();
        preview.setPos(x, y, z);
        preview.xo = x;
        preview.yo = y;
        preview.zo = z;
        preview.xOld = x;
        preview.yOld = y;
        preview.zOld = z;
        preview.setDeltaMovement(0, 0, 0);
        preview.setYRot(yaw);
        preview.yRotO = yaw;
        preview.setXRot(0F);
        preview.xRotO = 0F;
        preview.yBodyRot = yaw;
        preview.yBodyRotO = yaw;
        preview.yHeadRot = yaw;
        preview.yHeadRotO = yaw;
        preview.setInvisible(!level.noCollision(preview));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ghost = null;
        contents = ItemStack.EMPTY;
    }

    private static ItemStack chassis(LocalPlayer player) {
        var main = player.getMainHandItem();
        if (main.getItem() instanceof PowerArmorChassisItem) {
            return main;
        }
        var off = player.getOffhandItem();
        return off.getItem() instanceof PowerArmorChassisItem ? off : ItemStack.EMPTY;
    }

    @Nullable
    private static BlockHitResult target(Minecraft minecraft, ClientLevel level) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        if (level.getBlockState(hit.getBlockPos()).getBlock() instanceof PowerArmorStationBlock) {
            return null;
        }
        return hit;
    }

    @Nullable
    private static PowerArmorEntity ghost(ClientLevel level) {
        if (ghost != null) {
            return ghost;
        }
        var created = ADEntityUtil.create(ADEntities.POWER_ARMOR.get(), level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (created == null) {
            return null;
        }
        created.setId(GHOST_ID);
        created.setPreview(true);
        created.setNoGravity(true);
        created.noPhysics = true;
        created.setSilent(true);
        created.setGlowingTag(true);
        level.addEntity(created);
        ghost = created;
        contents = ItemStack.EMPTY;
        return created;
    }

    private static void remove(ClientLevel level) {
        if (ghost == null) {
            return;
        }
        level.removeEntity(GHOST_ID, Entity.RemovalReason.DISCARDED);
        ghost = null;
        contents = ItemStack.EMPTY;
    }
}

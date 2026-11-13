package mod.azure.atomicdawn.client.camera;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.vats.VatsClient;
import mod.azure.atomicdawn.config.ADConfig;
import mod.azure.atomicdawn.entity.ExplosiveProjectileEntity;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class KillCam {

    @Nullable
    private static ExplosiveProjectileEntity following;

    @Nullable
    private static CameraType previousCameraType;

    private static int holdTicks;

    private KillCam() {}

    public static boolean isActive() {
        return following != null;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof ExplosiveProjectileEntity projectile)) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || projectile.getOwner() != player || following != null) {
            return;
        }
        var mode = AtomicDawn.CONFIG.killCam.KILL_CAM;
        var wanted = mode == ADConfig.KillCam.KillCamMode.ALWAYS
            || mode == ADConfig.KillCam.KillCamMode.VATS && VatsClient.isActive();
        if (!wanted) {
            return;
        }
        following = projectile;
        holdTicks = AtomicDawn.CONFIG.killCam.KILL_CAM_HOLD_TICKS;
        previousCameraType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        minecraft.setCameraEntity(projectile);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (following == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || !player.isAlive() || minecraft.gui.screen() != null) {
            stop();
            return;
        }
        if (minecraft.getCameraEntity() != following) {
            following = null;
            restoreCameraType();
            return;
        }
        if (following.isRemoved() && --holdTicks <= 0) {
            stop();
        }
    }

    @SubscribeEvent
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (following != null) {
            event.setDistance((float) AtomicDawn.CONFIG.killCam.KILL_CAM_DISTANCE);
        }
    }

    public static void stop() {
        var minecraft = Minecraft.getInstance();
        if (following != null && minecraft.getCameraEntity() == following && minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }
        following = null;
        restoreCameraType();
    }

    private static void restoreCameraType() {
        if (previousCameraType != null) {
            Minecraft.getInstance().options.setCameraType(previousCameraType);
            previousCameraType = null;
        }
    }
}

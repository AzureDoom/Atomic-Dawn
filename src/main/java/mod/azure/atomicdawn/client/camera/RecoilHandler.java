package mod.azure.atomicdawn.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class RecoilHandler {

    private static float pendingPitch;

    private static float pendingYaw;

    private static long lastFrameNanos = -1;

    private RecoilHandler() {}

    public static void kick(WeaponDefinition.Recoil recoil, boolean aiming, RandomSource random) {
        var multiplier = aiming ? recoil.adsMultiplier() : 1F;
        var variance = 0.85F + random.nextFloat() * 0.3F;
        pendingPitch += recoil.vertical() * multiplier * variance;
        pendingYaw += recoil.horizontal() * multiplier * (random.nextFloat() * 2F - 1F);
    }

    public static void clear() {
        pendingPitch = 0F;
        pendingYaw = 0F;
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        var now = System.nanoTime();
        var seconds = lastFrameNanos < 0 ? 0.0 : Math.min(0.1, (now - lastFrameNanos) / 1.0E9);
        lastFrameNanos = now;

        if (pendingPitch == 0F && pendingYaw == 0F) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            clear();
            return;
        }

        var fraction = (float) (1.0 - Math.exp(-30D * seconds));
        var pitchStep = pendingPitch * fraction;
        var yawStep = pendingYaw * fraction;
        pendingPitch -= pitchStep;
        pendingYaw -= yawStep;
        if (Math.abs(pendingPitch) < 0.001F && Math.abs(pendingYaw) < 0.001F) {
            clear();
        }

        player.turn(yawStep / 0.15D, -pitchStep / 0.15D);
    }
}

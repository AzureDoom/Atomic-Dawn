package mod.azure.atomicdawn.client.radiation;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.config.ADColors;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.worldgen.ADBiomes;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class RadStormVisuals {

    private static final DustParticleOptions FALLOUT = new DustParticleOptions(0x8BE04A, 1.1F);

    private static final DustParticleOptions FALLOUT_BRIGHT = new DustParticleOptions(0xC4FF6A, 0.8F);

    private static final int DEFAULT_TINT = 0x504CC22A;

    private static float tint;

    private static float previousTint;

    private RadStormVisuals() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        previousTint = tint;
        if (level == null || player == null) {
            tint = 0F;
            previousTint = 0F;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        var active = player.getData(ADAttachments.RAD_STORM.get()) && level.getBiome(player.blockPosition())
            .is(ADBiomes.WASTELAND);
        var exposed = active && level.canSeeSky(BlockPos.containing(player.getEyePosition()));
        var target = exposed ? 1F : active ? 0.35F : 0F;
        tint += Math.clamp(target - tint, -0.02F, 0.02F);
        if (!active || !AtomicDawn.CONFIG.clientRadStorm.RAD_STORM_PARTICLES) {
            return;
        }
        var random = level.getRandom();
        var cursor = new BlockPos.MutableBlockPos();
        for (var i = 0; i < 14; i++) {
            var x = player.getX() + (random.nextDouble() - 0.5) * 32.0;
            var z = player.getZ() + (random.nextDouble() - 0.5) * 32.0;
            var y = player.getY() + 4.0 + random.nextDouble() * 12.0;
            cursor.set(x, y, z);
            if (!level.canSeeSky(cursor)) {
                continue;
            }
            level.addParticle(random.nextInt(5) == 0 ? FALLOUT_BRIGHT : FALLOUT, x, y, z, 0, -0.35, 0);
        }
    }

    public static final class Overlay implements GuiLayer {

        @Override
        public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
            if (!AtomicDawn.CONFIG.clientRadStorm.RAD_STORM_OVERLAY) {
                return;
            }
            var partial = deltaTracker.getGameTimeDeltaPartialTick(false);
            var current = previousTint + (tint - previousTint) * partial;
            if (current <= 0.01F) {
                return;
            }
            var color = ADColors.argb(AtomicDawn.CONFIG.clientRadStorm.RAD_STORM_OVERLAY_COLOR, DEFAULT_TINT);
            var alpha = Math.round(Math.clamp(current, 0F, 1F) * ((color >>> 24) & 0xFF));
            if (alpha <= 0) {
                return;
            }
            var window = Minecraft.getInstance().getWindow();
            graphics.fill(
                0,
                0,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                (alpha << 24) | (color & 0xFFFFFF)
            );
        }
    }
}

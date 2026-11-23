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

    private static final int DEFAULT_TINT = 0x384CC22A;

    private static final int VIGNETTE_BANDS = 12;

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
        var target = exposed ? 1F : active ? 0.2F : 0F;
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
            var strength = Math.clamp(current, 0F, 1F) * (float) Math.clamp(
                AtomicDawn.CONFIG.clientRadStorm.RAD_STORM_OVERLAY_INTENSITY,
                0.0,
                1.0
            );
            var peak = strength * ((color >>> 24) & 0xFF);
            if (peak < 1F) {
                return;
            }
            var rgb = color & 0xFFFFFF;
            var window = Minecraft.getInstance().getWindow();
            var width = window.getGuiScaledWidth();
            var height = window.getGuiScaledHeight();
            if (!AtomicDawn.CONFIG.clientRadStorm.RAD_STORM_OVERLAY_VIGNETTE) {
                graphics.fill(0, 0, width, height, (Math.round(peak) << 24) | rgb);
                return;
            }
            var base = Math.round(peak * 0.2F);
            if (base > 0) {
                graphics.fill(0, 0, width, height, (base << 24) | rgb);
            }
            var depth = Math.max(VIGNETTE_BANDS, Math.round(Math.min(width, height) * 0.28F));
            var band = Math.max(1, depth / VIGNETTE_BANDS);
            for (var i = 0; i < VIGNETTE_BANDS; i++) {
                var falloff = 1F - (float) i / VIGNETTE_BANDS;
                var alpha = Math.round(peak * falloff * falloff);
                if (alpha <= 0) {
                    continue;
                }
                var argb = (alpha << 24) | rgb;
                var outer = i * band;
                var inner = outer + band;
                if (inner * 2 >= width || inner * 2 >= height) {
                    break;
                }
                graphics.fill(outer, outer, width - outer, inner, argb);
                graphics.fill(outer, height - inner, width - outer, height - outer, argb);
                graphics.fill(outer, inner, inner, height - inner, argb);
                graphics.fill(width - inner, inner, width - outer, height - inner, argb);
            }
        }
    }
}

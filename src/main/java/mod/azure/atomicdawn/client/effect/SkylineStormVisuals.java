package mod.azure.atomicdawn.client.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.worldgen.ADBiomes;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class SkylineStormVisuals {

    private static final double TAU = Math.PI * 2.0;

    private static final DustParticleOptions INNER_GLOW = new DustParticleOptions(0xFF494A, 3.3F);

    private static final DustParticleOptions LIGHTNING = new DustParticleOptions(0xFF5B64, 1.8F);

    private static final DustParticleOptions LIGHTNING_CORE = new DustParticleOptions(0xFFD0BA, 1.2F);

    private static int ticks;

    private static int stormStrength;

    private static int nextArcTick;

    private SkylineStormVisuals() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            stormStrength = 0;
            nextArcTick = 0;
            return;
        }

        if (!AtomicDawn.CONFIG.skylineVisuals.SKYLINE_STORM_VISUALS) {
            stormStrength = 0;
            return;
        }

        boolean inValley = level.getBiome(minecraft.player.blockPosition()).is(ADBiomes.SKYLINE_VALLEY);
        stormStrength = Math.clamp(stormStrength + (inValley ? 1 : -2), 0, 40);
        if (stormStrength == 0)
            return;

        ticks++;
        RandomSource random = level.getRandom();
        Vec3 center = minecraft.player.position();
        int detail = AtomicDawn.CONFIG.skylineVisuals.SKYLINE_STORM_DETAIL;

        if (ticks % 2 == 0) {
            for (int i = 0; i < detail + 1; i++) {
                double angle = ticks * 0.028 + random.nextDouble() * TAU;
                double radius = 5.0 + random.nextDouble() * 10.0;
                level.addParticle(
                    INNER_GLOW,
                    center.x + Math.cos(angle) * radius,
                    center.y + 27 + random.nextDouble() * 9,
                    center.z + Math.sin(angle) * radius,
                    -Math.sin(angle) * 0.08,
                    -0.016,
                    Math.cos(angle) * 0.08
                );
            }
        }

        if (inValley && stormStrength >= 30 && ticks >= nextArcTick) {
            nextArcTick = ticks + 28 + random.nextInt(53);
            double angle = random.nextDouble() * TAU;
            double radius = 18.0 + random.nextDouble() * 25.0;
            Vec3 from = center.add(Math.cos(angle) * radius, 44 + random.nextDouble() * 9, Math.sin(angle) * radius);
            Vec3 to = center.add(
                Math.cos(angle + 0.35) * (radius * 0.6),
                20 + random.nextDouble() * 9,
                Math.sin(angle + 0.35) * (radius * 0.6)
            );
            drawFork(level, random, from, to, detail);
        }
    }

    private static void drawFork(ClientLevel level, RandomSource random, Vec3 start, Vec3 end, int detail) {
        int segments = 14 + detail * 8;
        Vec3 previous = start;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double jitter = Math.sin(t * Math.PI) * 2.6;
            Vec3 point = start.lerp(end, t)
                .add(
                    (random.nextDouble() - 0.5) * jitter,
                    0,
                    (random.nextDouble() - 0.5) * jitter
                );
            drawSegment(level, previous, point, 2 + detail);
            if (i % 3 == 0)
                level.addParticle(LIGHTNING_CORE, point.x, point.y, point.z, 0, 0, 0);
            if (i == segments / 3 || i == (segments * 2) / 3) {
                Vec3 forkEnd = point.add(
                    (random.nextDouble() - 0.5) * 12.0,
                    -5.0 - random.nextDouble() * 6.0,
                    (random.nextDouble() - 0.5) * 12.0
                );
                drawSegment(level, point, forkEnd, 8 + detail * 3);
            }
            previous = point;
        }
    }

    private static void drawSegment(ClientLevel level, Vec3 start, Vec3 end, int samples) {
        for (int j = 0; j <= samples; j++) {
            double t = (double) j / samples;
            Vec3 point = start.lerp(end, t);
            level.addParticle(SkylineStormVisuals.LIGHTNING, point.x, point.y, point.z, 0, 0, 0);
        }
    }
}

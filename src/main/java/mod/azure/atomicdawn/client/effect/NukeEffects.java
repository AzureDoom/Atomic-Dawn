package mod.azure.atomicdawn.client.effect;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.network.NukeExplosionPayload;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class NukeEffects {

    private static final DustParticleOptions FIRE_DUST = new DustParticleOptions(0xFF8A2A, 4F);

    private static final DustParticleOptions EMBER_DUST = new DustParticleOptions(0xFFD15A, 3F);

    private static final DustParticleOptions SOOT_DUST = new DustParticleOptions(0x4A403A, 4F);

    private static final List<Blast> ACTIVE = new ArrayList<>();

    private static float flash;

    private static float shake;

    private NukeEffects() {}

    private static final class Blast {

        final Vec3 center;

        final float radius;

        int age;

        Blast(Vec3 center, float radius) {
            this.center = center;
            this.radius = radius;
        }
    }

    public static void onDetonation(NukeExplosionPayload payload) {
        ACTIVE.add(new Blast(payload.position(), Math.max(2F, payload.radius())));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            if (level == null) {
                ACTIVE.clear();
                flash = 0F;
                shake = 0F;
            }
            return;
        }
        flash = 0F;
        shake = 0F;
        var iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            var blast = iterator.next();
            var distance = player.position().distanceTo(blast.center);
            if (blast.age < 40F) {
                var strength = (float) Mth.clamp(1.0 - distance / 384.0D, 0.0, 1.0);
                flash = Math.max(flash, strength * (1F - blast.age / (float) 40));
            }
            if (blast.age < 60F) {
                var strength = (float) Mth.clamp(1.0 - distance / 192.0D, 0.0, 1.0);
                shake = Math.max(shake, strength * (1F - blast.age / 60F));
            }
            spawn(level, blast);
            if (++blast.age >= 200) {
                iterator.remove();
            }
        }
    }

    private static void spawn(Level level, Blast blast) {
        var random = level.getRandom();
        var t = blast.age;
        var r = blast.radius;
        var c = blast.center;
        var stemHeight = r * 3.5;
        var top = stemHeight * Math.min(1.0, t / 60.0);

        if (t < 8) {
            var size = r * 0.6 * (t + 1) / 8.0;
            for (var i = 0; i < 40; i++) {
                var p = c.add(sphere(random, size));
                add(level, i % 4 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, p, Vec3.ZERO);
                if (i % 3 == 0) {
                    add(level, FIRE_DUST, p, Vec3.ZERO);
                }
            }
            if (t % 2 == 0) {
                add(level, ParticleTypes.EXPLOSION_EMITTER, c.add(sphere(random, r * 0.3)), Vec3.ZERO);
            }
        }

        if (t < 14) {
            var ring = r * 2.8 * (t + 1) / 14.0;
            for (var i = 0; i < 64; i++) {
                var angle = random.nextDouble() * Math.PI * 2;
                var dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                add(
                    level,
                    i % 2 == 0 ? ParticleTypes.CLOUD : ParticleTypes.POOF,
                    c.add(dir.scale(ring)).add(0, 0.3, 0),
                    dir.scale(0.4)
                );
            }
        }

        if (t < 140) {
            for (var i = 0; i < 8; i++) {
                var height = random.nextDouble() * top;
                var spread = r * (0.18 + 0.12 * (1.0 - height / stemHeight));
                var p = c.add(disc(random, spread)).add(0, height, 0);
                add(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, p, new Vec3(0, 0.05, 0));
                if (t < 70 && i % 2 == 0) {
                    add(level, i % 4 == 0 ? FIRE_DUST : EMBER_DUST, p, Vec3.ZERO);
                }
            }
        }

        if (t > 10 && t < 170) {
            var growth = Math.min(1.0, (t - 10) / 70.0);
            var major = r * 1.5 * growth;
            var minor = r * 0.7 * growth + 1.0;
            for (var i = 0; i < 14; i++) {
                var u = random.nextDouble() * Math.PI * 2;
                var v = random.nextDouble() * Math.PI * 2;
                var ring = major + minor * Math.cos(v);
                var p = c.add(ring * Math.cos(u), top + minor * Math.sin(v) * 0.8, ring * Math.sin(u));
                var out = new Vec3(Math.cos(u), 0.2, Math.sin(u)).scale(0.04);
                ParticleOptions type = i % 3 == 0 ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.LARGE_SMOKE;
                add(level, type, p, out);
                if (t < 90) {
                    add(level, i % 2 == 0 ? FIRE_DUST : EMBER_DUST, p, Vec3.ZERO);
                } else if (i % 4 == 0) {
                    add(level, SOOT_DUST, p, Vec3.ZERO);
                }
            }

            for (var i = 0; i < 4; i++) {
                var p = c.add(disc(random, major * 0.8)).add(0, top + minor * 0.9, 0);
                add(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p, new Vec3(0, 0.02, 0));
            }
        }
    }

    private static void add(Level level, ParticleOptions particle, Vec3 pos, Vec3 velocity) {
        level.addAlwaysVisibleParticle(particle, true, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
    }

    private static Vec3 sphere(RandomSource random, double radius) {
        Vec3 v;
        do {
            v = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
        } while (v.lengthSqr() > 1);
        return v.scale(radius);
    }

    private static Vec3 disc(RandomSource random, double radius) {
        var angle = random.nextDouble() * Math.PI * 2;
        var distance = Math.sqrt(random.nextDouble()) * radius;
        return new Vec3(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shake <= 0F) {
            return;
        }
        var random = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getRandom();
        if (random == null) {
            return;
        }
        var amplitude = shake * 4F;
        event.setPitch(event.getPitch() + (random.nextFloat() - 0.5F) * amplitude);
        event.setYaw(event.getYaw() + (random.nextFloat() - 0.5F) * amplitude);
        event.setRoll(event.getRoll() + (random.nextFloat() - 0.5F) * amplitude * 0.5F);
    }

    public static final class FlashLayer implements ADHudLayer {

        @Override
        public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
            if (flash <= 0.01F) {
                return;
            }
            var window = Minecraft.getInstance().getWindow();
            var alpha = Math.round(Mth.clamp(flash, 0F, 1F) * 255F);
            graphics.fill(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight(), (alpha << 24) | 0xFFF6E0);
        }
    }
}

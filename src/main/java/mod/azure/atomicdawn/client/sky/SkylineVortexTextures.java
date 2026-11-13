package mod.azure.atomicdawn.client.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import mod.azure.atomicdawn.AtomicDawn;

public final class SkylineVortexTextures {

    public static final Identifier VORTEX = AtomicDawn.id("dynamic/skyline_vortex");

    public static final Identifier WISPS = AtomicDawn.id("dynamic/skyline_wisps");

    public static final Identifier HAZE = AtomicDawn.id("dynamic/skyline_haze");

    private static final int VORTEX_SIZE = 768;

    private static final int WISP_SIZE = 512;

    private static final int HAZE_HEIGHT = 64;

    private static final float[][] VORTEX_GRADIENT = {
        { 0.00F, 12, 3, 6 },
        { 0.06F, 50, 8, 16 },
        { 0.15F, 140, 26, 40 },
        { 0.30F, 214, 62, 72 },
        { 0.48F, 236, 104, 100 },
        { 0.66F, 246, 150, 132 },
        { 0.84F, 232, 176, 182 },
        { 1.00F, 210, 172, 206 } };

    private static final double[] SHADOW = { 70, 12, 24 };

    private static final double[] HIGHLIGHT = { 255, 214, 200 };

    @Nullable
    private static CompletableFuture<int[][]> pending;

    private static boolean ready;

    private SkylineVortexTextures() {}

    public static boolean ready() {
        if (ready) {
            return true;
        }
        if (pending == null) {
            pending = CompletableFuture.supplyAsync(
                () -> new int[][] { vortex(), wisps(), haze() }
            );
            return false;
        }
        if (!pending.isDone()) {
            return false;
        }
        try {
            var pixels = pending.join();
            upload(VORTEX, VORTEX_SIZE, VORTEX_SIZE, pixels[0]);
            upload(WISPS, WISP_SIZE, WISP_SIZE, pixels[1]);
            upload(HAZE, 4, HAZE_HEIGHT, pixels[2]);
            ready = true;
        } catch (RuntimeException exception) {
            AtomicDawn.LOGGER.error("Failed to build Skyline Valley sky textures", exception);
            pending = null;
        }
        return ready;
    }

    public static void release() {
        if (!ready) {
            pending = null;
            return;
        }
        var manager = Minecraft.getInstance().getTextureManager();
        manager.release(VORTEX);
        manager.release(WISPS);
        manager.release(HAZE);
        ready = false;
        pending = null;
    }

    private static void upload(Identifier id, int width, int height, int[] argb) {
        var texture = new DynamicTexture(() -> "Atomic Dawn " + id.getPath(), width, height, true);
        var image = texture.getPixels();
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                image.setPixel(x, y, argb[y * width + x]);
            }
        }
        texture.upload();
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    private static int[] vortex() {
        var detail = new Noise(1337L);
        var warp = new Noise(1337L + 7L);
        var out = new int[SkylineVortexTextures.VORTEX_SIZE * SkylineVortexTextures.VORTEX_SIZE];
        for (var py = 0; py < SkylineVortexTextures.VORTEX_SIZE; py++) {
            for (var px = 0; px < SkylineVortexTextures.VORTEX_SIZE; px++) {
                var x = (px + 0.5) / SkylineVortexTextures.VORTEX_SIZE * 2.0 - 1.0;
                var y = (py + 0.5) / SkylineVortexTextures.VORTEX_SIZE * 2.0 - 1.0;
                var r = Math.sqrt(x * x + y * y);
                if (r > 1.0) {
                    out[py * SkylineVortexTextures.VORTEX_SIZE + px] = 0;
                    continue;
                }
                var twist = 3.0 * Math.pow(1.0 - r, 2.2) + 0.55 * Math.log(r + 0.03);
                var cos = Math.cos(twist);
                var sin = Math.sin(twist);
                var sx = x * cos - y * sin;
                var sy = x * sin + y * cos;
                var density = density(detail, warp, sx, sy);
                var offset = density(detail, warp, sx + 0.012, sy + 0.012);
                var shade = clamp(0.75 + (density - offset) * 5.0, 0.5, 1.25);

                var base = gradient(r);
                var depth = 0.17 + 0.83 * smooth(0.02, 0.58, r);
                var rings = 0.88 + 0.12 * Math.sin(r * 46.0 + detail.fbm(x * 4.0, y * 4.0, 3) * 6.0)
                    * (1.0 - smooth(0.15, 0.6, r));
                var light = clamp((0.35 + 0.85 * density) * shade * depth * rings, 0.0, 1.6);
                var low = Math.min(1.0, light);
                var high = Math.clamp(light - 1.0, 0.0, 0.6) / 0.6 * 0.7;
                var red = mix(SHADOW[0], base[0], low);
                var green = mix(SHADOW[1], base[1], low);
                var blue = mix(SHADOW[2], base[2], low);
                red = mix(red, HIGHLIGHT[0], high);
                green = mix(green, HIGHLIGHT[1], high);
                blue = mix(blue, HIGHLIGHT[2], high);

                var alpha = (0.96 + 0.04 * density) * (1.0 - smooth(0.88, 1.0, r));
                alpha = Math.max(alpha, 1.0 - smooth(0.00, 0.36, r));
                var eye = 1.0 - 0.80 * (1.0 - smooth(0.04, 0.25, r));
                red *= eye;
                green *= eye;
                blue *= eye;
                out[py * SkylineVortexTextures.VORTEX_SIZE + px] = argb(alpha, red, green, blue);
            }
        }
        return out;
    }

    private static double density(Noise detail, Noise warp, double x, double y) {
        var wx = warp.fbm(x * 2.0, y * 2.0, 4);
        var wy = warp.fbm(x * 2.0 + 5.2, y * 2.0 + 1.3, 4);
        var qx = x * 4.2 + (wx - 0.5) * 1.1;
        var qy = y * 4.2 + (wy - 0.5) * 1.1;
        var radial = 1.0 - clamp01(Math.sqrt(x * x + y * y));
        var n = detail.fbm(qx * (1.0 + 1.2 * radial), qy, 6);
        var billow = 1.0 - Math.abs(2.0 * n - 1.0);
        var mixed = n * 0.6 + billow * 0.4;
        var low = warp.fbm(x * 1.3 + 3.1, y * 1.3 - 2.2, 3);
        return clamp01(0.28 + 0.45 * smooth(0.42, 0.78, mixed) + 0.3 * smooth(0.35, 0.7, low));
    }

    private static int[] wisps() {
        var noise = new Noise(4242L);
        var out = new int[SkylineVortexTextures.WISP_SIZE * SkylineVortexTextures.WISP_SIZE];
        for (var py = 0; py < SkylineVortexTextures.WISP_SIZE; py++) {
            for (var px = 0; px < SkylineVortexTextures.WISP_SIZE; px++) {
                var x = (px + 0.5) / SkylineVortexTextures.WISP_SIZE * 2.0 - 1.0;
                var y = (py + 0.5) / SkylineVortexTextures.WISP_SIZE * 2.0 - 1.0;
                var r = Math.sqrt(x * x + y * y);
                if (r > 1.0) {
                    out[py * SkylineVortexTextures.WISP_SIZE + px] = 0;
                    continue;
                }
                var twist = 7.5 * Math.pow(1.0 - r, 2.0);
                var cos = Math.cos(twist);
                var sin = Math.sin(twist);
                var sx = x * cos - y * sin;
                var sy = x * sin + y * cos;
                var density = noise.fbm(sx * 5.5, sy * 2.2, 5);
                density = clamp01((density - 0.42) * 2.4);
                var alpha = density * density * 0.63 * smooth(0.12, 0.35, r) * (1.0 - smooth(0.78, 0.99, r));
                var warm = smooth(0.2, 0.9, r);
                out[py * SkylineVortexTextures.WISP_SIZE + px] = argb(
                    alpha,
                    mix(250, 236, warm),
                    mix(150, 196, warm),
                    mix(150, 196, warm)
                );
            }
        }
        return out;
    }

    private static int[] haze() {
        var out = new int[4 * SkylineVortexTextures.HAZE_HEIGHT];
        for (var y = 0; y < SkylineVortexTextures.HAZE_HEIGHT; y++) {
            var t = y / (double) (SkylineVortexTextures.HAZE_HEIGHT - 1);
            var alpha = Math.pow(1.0 - t, 1.8);
            for (var x = 0; x < 4; x++) {
                out[y * 4 + x] = argb(alpha, 255, 255, 255);
            }
        }
        return out;
    }

    private static double[] gradient(double t) {
        for (var i = 1; i < SkylineVortexTextures.VORTEX_GRADIENT.length; i++) {
            if (t <= SkylineVortexTextures.VORTEX_GRADIENT[i][0]) {
                var a = SkylineVortexTextures.VORTEX_GRADIENT[i - 1];
                var b = SkylineVortexTextures.VORTEX_GRADIENT[i];
                var f = (t - a[0]) / Math.max(1e-6, b[0] - a[0]);
                return new double[] { mix(a[1], b[1], f), mix(a[2], b[2], f), mix(a[3], b[3], f) };
            }
        }
        var last = SkylineVortexTextures.VORTEX_GRADIENT[SkylineVortexTextures.VORTEX_GRADIENT.length - 1];
        return new double[] { last[1], last[2], last[3] };
    }

    private static int argb(double alpha, double red, double green, double blue) {
        return channel(alpha * 255.0) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(double value) {
        return Math.clamp(Math.round(value), 0, 255);
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double value) {
        return Math.clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double smooth(double edge0, double edge1, double value) {
        var t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    private static final class Noise {

        private final int[] permutation = new int[512];

        Noise(long seed) {
            var random = new Random(seed);
            var base = new int[256];
            for (var i = 0; i < 256; i++) {
                base[i] = i;
            }
            for (var i = 255; i > 0; i--) {
                var j = random.nextInt(i + 1);
                var tmp = base[i];
                base[i] = base[j];
                base[j] = tmp;
            }
            for (var i = 0; i < 512; i++) {
                permutation[i] = base[i & 255];
            }
        }

        private double hash(int x, int y) {
            return permutation[permutation[x & 255] + (y & 255)] / 255.0;
        }

        double value(double x, double y) {
            var x0 = (int) Math.floor(x);
            var y0 = (int) Math.floor(y);
            var fx = x - x0;
            var fy = y - y0;
            var u = fx * fx * (3.0 - 2.0 * fx);
            var v = fy * fy * (3.0 - 2.0 * fy);
            var a = hash(x0, y0);
            var b = hash(x0 + 1, y0);
            var c = hash(x0, y0 + 1);
            var d = hash(x0 + 1, y0 + 1);
            return mix(mix(a, b, u), mix(c, d, u), v);
        }

        double fbm(double x, double y, int octaves) {
            var sum = 0.0;
            var amplitude = 0.5;
            var total = 0.0;
            for (var i = 0; i < octaves; i++) {
                sum += value(x, y) * amplitude;
                total += amplitude;
                var nx = x * 1.97 + y * 0.31;
                y = y * 1.97 - x * 0.31;
                x = nx;
                amplitude *= 0.5;
            }
            return sum / total;
        }
    }
}

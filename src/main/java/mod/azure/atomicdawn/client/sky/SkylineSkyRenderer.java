package mod.azure.atomicdawn.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.worldgen.ADBiomes;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class SkylineSkyRenderer {

    private static final float FADE_PER_TICK = 1F / 60F;

    private static final int RINGS = 32;

    private static final int SEGMENTS = 96;

    private static final int HAZE_SEGMENTS = 96;

    private static float strength;

    private static float previousStrength;

    private SkylineSkyRenderer() {}

    private static RenderType type(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        previousStrength = strength;
        var target = 0F;
        if (minecraft.level != null && AtomicDawn.CONFIG.client.effects.skyline.SKYLINE_SKY) {
            var pos = minecraft.gameRenderer.mainCamera().blockPosition();
            if (minecraft.level.getBiome(pos).is(ADBiomes.SKYLINE_VALLEY)) {
                target = 1F;
            }
        }
        strength = strength < target
            ? Math.min(target, strength + FADE_PER_TICK)
            : Math.max(target, strength - FADE_PER_TICK);
        if (strength > 0F) {
            SkylineVortexTextures.ready();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        strength = 0F;
        previousStrength = 0F;
        SkylineVortexTextures.release();
    }

    @SubscribeEvent
    public static void onSubmitSky(SubmitCustomGeometryEvent event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return;
        }

        var partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var fade = Mth.lerp(partialTick, previousStrength, strength);
        if (fade <= 0.001F || !SkylineVortexTextures.ready()) {
            return;
        }

        var radius = 72F;
        var height = 48F;
        var time = (level.getGameTime() + partialTick)
            * AtomicDawn.CONFIG.client.effects.skyline.SKYLINE_SKY_SPIN;

        var flash = 1F + 0.45F * level.getThunderLevel(partialTick);

        var pose = event.getPoseStack();
        pose.pushPose();

        var collector = event.getSubmitNodeCollector();
        collector.submitCustomGeometry(
            pose,
            type(SkylineVortexTextures.HAZE),
            (transform, consumer) -> drawHaze(
                consumer,
                transform,
                radius * 0.96F,
                -height * 0.7F,
                height * 1.15F,
                fade * 0.85F
            )
        );

        collector.submitCustomGeometry(
            pose,
            type(SkylineVortexTextures.VORTEX),
            (transform, consumer) -> drawDisk(
                consumer,
                transform,
                height,
                radius,
                (float) (time * 0.0007F),
                fade,
                flash
            )
        );

        collector.submitCustomGeometry(
            pose,
            type(SkylineVortexTextures.WISPS),
            (transform, consumer) -> drawDisk(
                consumer,
                transform,
                height * 0.94F,
                radius * 0.96F,
                (float) (-time * 0.0013F),
                fade * 0.67F,
                flash
            )
        );

        collector.submitCustomGeometry(
            pose,
            type(SkylineVortexTextures.WISPS),
            (transform, consumer) -> drawDisk(
                consumer,
                transform,
                height * 0.985F,
                radius * 0.985F,
                (float) (time * 0.0017F + 1.9F),
                fade * 0.35F,
                flash * 0.85F
            )
        );

        var lightningTime = level.getGameTime() + partialTick;
        var phase = lightningTime % 109F;
        if (phase < 8F) {
            var strikeAlpha = fade * (phase < 2F ? 1F : phase < 4F ? 0.12F : (8F - phase) / 4F);
            collector.submitCustomGeometry(
                pose,
                type(SkylineVortexTextures.HAZE),
                (transform, consumer) -> drawLightning(consumer, transform, lightningTime, strikeAlpha)
            );
        }

        pose.popPose();
    }

    private static void drawDisk(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float height,
        float radius,
        float rotation,
        float alpha,
        float brightness
    ) {
        var shade = Math.min(255, Math.round(255 * brightness));
        var a = Math.round(255 * Mth.clamp(alpha, 0F, 1F));
        for (var ring = 0; ring < RINGS; ring++) {
            var r0 = ring / (float) RINGS;
            var r1 = (ring + 1) / (float) RINGS;
            for (var segment = 0; segment < SEGMENTS; segment++) {
                var a0 = segment / (float) SEGMENTS * Mth.TWO_PI;
                var a1 = (segment + 1) / (float) SEGMENTS * Mth.TWO_PI;
                diskVertex(consumer, pose, height, radius, r0, a0, rotation, shade, a);
                diskVertex(consumer, pose, height, radius, r1, a0, rotation, shade, a);
                diskVertex(consumer, pose, height, radius, r1, a1, rotation, shade, a);
                diskVertex(consumer, pose, height, radius, r0, a1, rotation, shade, a);
            }
        }
    }

    private static void diskVertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float height,
        float radius,
        float r,
        float angle,
        float rotation,
        int shade,
        int alpha
    ) {
        var x = Mth.cos(angle) * r * radius;
        var z = Mth.sin(angle) * r * radius;
        var u = 0.5F + 0.5F * r * Mth.cos(angle + rotation);
        var v = 0.5F + 0.5F * r * Mth.sin(angle + rotation);
        var dip = r * r * height * 0.35F;
        vertex(consumer, pose, x, height - dip, z, u, v, shade, shade, shade, alpha);
    }

    private static void drawHaze(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float radius,
        float bottom,
        float top,
        float alpha
    ) {
        var a = Math.round(255 * Mth.clamp(alpha, 0F, 1F));
        for (var segment = 0; segment < HAZE_SEGMENTS; segment++) {
            var a0 = segment / (float) HAZE_SEGMENTS * Mth.TWO_PI;
            var a1 = (segment + 1) / (float) HAZE_SEGMENTS * Mth.TWO_PI;
            var x0 = Mth.cos(a0) * radius;
            var z0 = Mth.sin(a0) * radius;
            var x1 = Mth.cos(a1) * radius;
            var z1 = Mth.sin(a1) * radius;
            vertex(consumer, pose, x0, bottom, z0, 0F, 0F, 226, 170, 206, a);
            vertex(consumer, pose, x1, bottom, z1, 1F, 0F, 226, 170, 206, a);
            vertex(consumer, pose, x1, top, z1, 1F, 1F, 226, 170, 206, a);
            vertex(consumer, pose, x0, top, z0, 0F, 1F, 226, 170, 206, a);
        }
    }

    private static void drawLightning(VertexConsumer consumer, PoseStack.Pose pose, float time, float alpha) {
        var cycle = (int) (time / 109F);
        var angle = cycle * 2.39996F;
        var startRadius = 15F + (cycle & 3) * 3F;
        var endRadius = startRadius + 25F;
        var x0 = Mth.cos(angle) * startRadius;
        var z0 = Mth.sin(angle) * startRadius;
        var x1 = Mth.cos(angle + 0.45F) * endRadius;
        var z1 = Mth.sin(angle + 0.45F) * endRadius;
        var y = 36F;
        lightningPath(consumer, pose, x0, z0, x1, z1, y, 2.4F, 107, 65, 210, alpha * 0.38F, cycle);
        lightningPath(consumer, pose, x0, z0, x1, z1, y - 0.06F, 0.68F, 217, 174, 255, alpha, cycle);
        var bx = (x0 + x1) * 0.5F;
        var bz = (z0 + z1) * 0.5F;
        lightningPath(
            consumer,
            pose,
            bx,
            bz,
            bx + Mth.cos(angle + 1.3F) * 15F,
            bz + Mth.sin(angle + 1.3F) * 15F,
            y - 0.12F,
            0.48F,
            177,
            120,
            255,
            alpha * 0.7F,
            cycle + 17
        );
    }

    private static void lightningPath(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float startX,
        float startZ,
        float endX,
        float endZ,
        float height,
        float width,
        int red,
        int green,
        int blue,
        float alpha,
        int seed
    ) {
        final int steps = 11;
        var prevX = startX;
        var prevZ = startZ;
        for (int i = 1; i <= steps; i++) {
            var t = i / (float) steps;
            var jitter = Mth.sin(t * Mth.PI) * 2.1F;
            var x = Mth.lerp(t, startX, endX) + jitter * Mth.sin(seed * 7.1F + i * 3.7F);
            var z = Mth.lerp(t, startZ, endZ) + jitter * Mth.cos(seed * 3.9F + i * 5.2F);
            var dx = x - prevX;
            var dz = z - prevZ;
            var inv = Mth.invSqrt(dx * dx + dz * dz + 0.00001F) * width * 0.5F;
            var nx = -dz * inv;
            var nz = dx * inv;
            var a = Math.round(255 * Mth.clamp(alpha, 0F, 1F));
            vertex(consumer, pose, prevX - nx, height, prevZ - nz, 0F, 0F, red, green, blue, a);
            vertex(consumer, pose, x - nx, height, z - nz, 1F, 0F, red, green, blue, a);
            vertex(consumer, pose, x + nx, height, z + nz, 1F, 0F, red, green, blue, a);
            vertex(consumer, pose, prevX + nx, height, prevZ + nz, 0F, 0F, red, green, blue, a);
            prevX = x;
            prevZ = z;
        }
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0)
            .setNormal(pose, 0F, -1F, 0F);
    }
}

package mod.azure.atomicdawn.client.render.powerarmor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;

public final class PowerArmorTextures {

    public static final Identifier TEXTURE = AtomicDawn.id("textures/entity/power_armor/power_armor.png");

    public static final Identifier PAINT_MASK = AtomicDawn.id("textures/entity/power_armor/power_armor_paint_mask.png");

    private static final int MAX_CACHED = 48;

    private static final Map<String, Identifier> CACHE = new LinkedHashMap<>(16, 0.75F, true);

    @Nullable
    private static NativeImage base;

    @Nullable
    private static NativeImage mask;

    private static boolean failed;

    private PowerArmorTextures() {}

    public static Identifier texture(@Nullable PowerArmorEntity armor) {
        if (armor == null || failed) {
            return TEXTURE;
        }
        var colors = new int[PowerArmorSlot.COUNT];
        var painted = false;
        for (var slot : PowerArmorSlot.all()) {
            var color = PowerArmorPieceItem.paint(armor.getPiece(slot));
            colors[slot.ordinal()] = color;
            painted |= color != PowerArmorPieceItem.NO_PAINT;
        }
        if (!painted) {
            return TEXTURE;
        }
        var key = key(colors);
        var cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var created = create(key, colors);
        return created == null ? TEXTURE : created;
    }

    private static String key(int[] colors) {
        var builder = new StringBuilder();
        for (var color : colors) {
            builder.append(
                color == PowerArmorPieceItem.NO_PAINT ? "x" : String.format(Locale.ROOT, "%06x", color & 0xFFFFFF)
            );
            builder.append('_');
        }
        return builder.toString();
    }

    private static boolean load() {
        if (base != null && mask != null) {
            return true;
        }
        var resources = Minecraft.getInstance().getResourceManager();
        try (
            var baseStream = resources.getResource(TEXTURE).orElseThrow().open();
            var maskStream = resources.getResource(PAINT_MASK).orElseThrow().open()
        ) {
            base = NativeImage.read(baseStream);
            mask = NativeImage.read(maskStream);
            return true;
        } catch (IOException | RuntimeException exception) {
            AtomicDawn.LOGGER.warn(
                "Power armor paint disabled: could not load {} / {}",
                TEXTURE,
                PAINT_MASK,
                exception
            );
            failed = true;
            return false;
        }
    }

    private static int maskSlot(int argb) {
        if ((argb >>> 24) < 128) {
            return -1;
        }
        var r = ((argb >> 16) & 0xFF) > 127;
        var g = ((argb >> 8) & 0xFF) > 127;
        var b = (argb & 0xFF) > 127;
        if (r && !g && !b) {
            return PowerArmorSlot.HELMET.ordinal();
        }
        if (!r && g && !b) {
            return PowerArmorSlot.TORSO.ordinal();
        }
        if (!r && !g && b) {
            return PowerArmorSlot.LEFT_ARM.ordinal();
        }
        if (r && g && !b) {
            return PowerArmorSlot.RIGHT_ARM.ordinal();
        }
        if (r && !g && b) {
            return PowerArmorSlot.LEFT_LEG.ordinal();
        }
        if (!r && g && b) {
            return PowerArmorSlot.RIGHT_LEG.ordinal();
        }
        return -1;
    }

    @Nullable
    private static Identifier create(String key, int[] colors) {
        if (!load() || base == null || mask == null) {
            return null;
        }
        var width = base.getWidth();
        var height = base.getHeight();
        var texture = new DynamicTexture(() -> "Atomic Dawn power armor paint", width, height, true);
        var pixels = texture.getPixels();
        if (pixels == null) {
            return null;
        }
        var sameSize = mask.getWidth() == width && mask.getHeight() == height;
        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var argb = base.getPixel(x, y);
                var slot = sameSize ? maskSlot(mask.getPixel(x, y)) : -1;
                if (slot >= 0 && colors[slot] != PowerArmorPieceItem.NO_PAINT) {
                    argb = tint(argb, colors[slot]);
                }
                pixels.setPixel(x, y, argb);
            }
        }
        texture.upload();
        var id = AtomicDawn.id(
            "dynamic/power_armor/" + Integer.toHexString(Arrays.hashCode(colors)) + "_" + CACHE.size()
        );
        Minecraft.getInstance().getTextureManager().register(id, texture);
        CACHE.put(key, id);
        if (CACHE.size() > MAX_CACHED) {
            var eldest = CACHE.entrySet().iterator().next();
            Minecraft.getInstance().getTextureManager().release(eldest.getValue());
            CACHE.remove(eldest.getKey());
        }
        return id;
    }

    private static int tint(int argb, int color) {
        var a = argb >>> 24;
        var r = (argb >> 16) & 0xFF;
        var g = (argb >> 8) & 0xFF;
        var b = argb & 0xFF;
        var luminance = (r * 0.3F + g * 0.59F + b * 0.11F) / 255F;
        var shade = 0.35F + luminance * 0.9F;
        var tr = Math.min(255, Math.round(((color >> 16) & 0xFF) * shade));
        var tg = Math.min(255, Math.round(((color >> 8) & 0xFF) * shade));
        var tb = Math.min(255, Math.round((color & 0xFF) * shade));
        return a << 24 | tr << 16 | tg << 8 | tb;
    }

    public static void clear() {
        var manager = Minecraft.getInstance().getTextureManager();
        CACHE.values().forEach(manager::release);
        CACHE.clear();
        if (base != null) {
            base.close();
            base = null;
        }
        if (mask != null) {
            mask.close();
            mask = null;
        }
        failed = false;
    }
}

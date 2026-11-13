package mod.azure.atomicdawn.client.pipboy.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;

public final class PipBoyMapRenderer {

    public static final int[] ZOOMS = { 1, 2, 4, 8 };

    private static final int UNKNOWN = 0;

    private static final int CEILING_SCAN = 64;

    private static final int DIRTY_BUDGET = 2400;

    private static final int SWEEP_BUDGET = 600;

    private int cols;

    private int rows;

    private int[] colors = new int[0];

    private boolean[] dirty = new boolean[0];

    private int sweep;

    private int originX = Integer.MIN_VALUE;

    private int originZ = Integer.MIN_VALUE;

    private int appliedZoom = -1;

    private boolean appliedColor;

    private int appliedHue;

    @Nullable
    private ResourceKey<Level> dimension;

    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    private static final Identifier TEXTURE_ID = AtomicDawn.id("pipboy_map");

    @Nullable
    private DynamicTexture texture;

    private int textureWidth;

    private int textureHeight;

    private boolean textureDirty = true;

    private int textureUnknown;

    public int zoomIndex = 1;

    public int panX;

    public int panZ;

    public int zoom() {
        return ZOOMS[Mth.clamp(zoomIndex, 0, ZOOMS.length - 1)];
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    public void resize(int newCols, int newRows) {
        if (newCols == cols && newRows == rows) {
            return;
        }
        cols = Math.max(1, newCols);
        rows = Math.max(1, newRows);
        colors = new int[cols * rows];
        dirty = new boolean[cols * rows];
        Arrays.fill(dirty, true);
        originX = Integer.MIN_VALUE;
        sweep = 0;
        textureDirty = true;
    }

    public void center() {
        panX = 0;
        panZ = 0;
    }

    public void update(ClientLevel level, LocalPlayer player) {
        if (cols == 0 || rows == 0) {
            return;
        }
        var zoom = zoom();
        var colorMode = AtomicDawn.CONFIG.clientPipboy.PIPBOY_MAP_COLOR;
        var hue = PipBoyTheme.text();
        if (level.dimension() != dimension || zoom != appliedZoom || colorMode != appliedColor || hue != appliedHue) {
            dimension = level.dimension();
            appliedZoom = zoom;
            appliedColor = colorMode;
            appliedHue = hue;
            invalidate();
        }
        var centerX = Math.floorDiv(Mth.floor(player.getX()) + panX, zoom) * zoom;
        var centerZ = Math.floorDiv(Mth.floor(player.getZ()) + panZ, zoom) * zoom;
        var newOriginX = centerX - (cols / 2) * zoom;
        var newOriginZ = centerZ - (rows / 2) * zoom;
        if (originX == Integer.MIN_VALUE) {
            originX = newOriginX;
            originZ = newOriginZ;
            invalidate();
        } else if (newOriginX != originX || newOriginZ != originZ) {
            shift((newOriginX - originX) / zoom, (newOriginZ - originZ) / zoom);
            originX = newOriginX;
            originZ = newOriginZ;
        }

        var hasCeiling = level.dimensionType().hasCeiling();
        var playerY = Mth.floor(player.getY());
        var budget = DIRTY_BUDGET;
        for (var i = 0; i < dirty.length && budget > 0; i++) {
            if (dirty[i]) {
                set(i, sample(level, i % cols, i / cols, hasCeiling, playerY));
                dirty[i] = false;
                budget--;
            }
        }
        for (var n = 0; n < SWEEP_BUDGET; n++) {
            sweep = (sweep + 1) % colors.length;
            if (!dirty[sweep]) {
                set(sweep, sample(level, sweep % cols, sweep / cols, hasCeiling, playerY));
            }
        }
    }

    private void set(int index, int color) {
        if (colors[index] != color) {
            colors[index] = color;
            textureDirty = true;
        }
    }

    private void invalidate() {
        Arrays.fill(colors, UNKNOWN);
        Arrays.fill(dirty, true);
        textureDirty = true;
    }

    private void shift(int dx, int dz) {
        if (Math.abs(dx) >= cols || Math.abs(dz) >= rows) {
            invalidate();
            return;
        }
        var nextColors = new int[colors.length];
        var nextDirty = new boolean[dirty.length];
        Arrays.fill(nextDirty, true);
        for (var row = 0; row < rows; row++) {
            var sourceRow = row + dz;
            if (sourceRow < 0 || sourceRow >= rows) {
                continue;
            }
            for (var col = 0; col < cols; col++) {
                var sourceCol = col + dx;
                if (sourceCol < 0 || sourceCol >= cols) {
                    continue;
                }
                var from = sourceRow * cols + sourceCol;
                nextColors[row * cols + col] = colors[from];
                nextDirty[row * cols + col] = dirty[from];
            }
        }
        colors = nextColors;
        dirty = nextDirty;
        textureDirty = true;
    }

    private int sample(ClientLevel level, int col, int row, boolean hasCeiling, int playerY) {
        var zoom = zoom();
        var x = originX + col * zoom;
        var z = originZ + row * zoom;
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return UNKNOWN;
        }
        var y = surface(level, x, z, hasCeiling, playerY);
        if (y == Integer.MIN_VALUE) {
            return UNKNOWN;
        }
        cursor.set(x, y, z);
        var state = level.getBlockState(cursor);
        var mapColor = state.getMapColor(level, cursor);
        if (mapColor == MapColor.NONE) {
            return UNKNOWN;
        }
        var shade = 1F;
        if (level.hasChunk(x >> 4, (z - zoom) >> 4)) {
            var north = surface(level, x, z - zoom, hasCeiling, playerY);
            if (north != Integer.MIN_VALUE) {
                shade = y > north ? 1.15F : y < north ? 0.78F : 1F;
            }
        }
        if (mapColor == MapColor.WATER) {
            shade *= 0.9F;
        }
        var rgb = mapColor.col;
        var r = (rgb >> 16) & 0xFF;
        var g = (rgb >> 8) & 0xFF;
        var b = rgb & 0xFF;
        if (appliedColor) {
            return 0xFF000000 | channel(r * shade) << 16 | channel(g * shade) << 8 | channel(b * shade);
        }
        var luminance = (0.3F * r + 0.59F * g + 0.11F * b) / 255F * shade;
        var factor = 0.18F + 0.82F * Math.clamp(luminance, 0F, 1.2F);
        var hr = (appliedHue >> 16) & 0xFF;
        var hg = (appliedHue >> 8) & 0xFF;
        var hb = appliedHue & 0xFF;
        return 0xFF000000 | channel(hr * factor) << 16 | channel(hg * factor) << 8 | channel(hb * factor);
    }

    private static int channel(float value) {
        return Mth.clamp(Math.round(value), 0, 255);
    }

    private int surface(ClientLevel level, int x, int z, boolean hasCeiling, int playerY) {
        if (!hasCeiling) {
            var y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            for (var step = 0; step < 6; step++) {
                cursor.set(x, y - step, z);
                if (level.getBlockState(cursor).getMapColor(level, cursor) != MapColor.NONE) {
                    return y - step;
                }
            }
            return y;
        }
        var airAbove = false;
        for (var step = 0; step < CEILING_SCAN; step++) {
            var y = playerY + 2 - step;
            cursor.set(x, y, z);
            var solid = !level.getBlockState(cursor).isAir();
            if (solid && airAbove) {
                return y;
            }
            airAbove = !solid;
        }
        return Integer.MIN_VALUE;
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int cell) {
        if (cols == 0 || rows == 0) {
            return;
        }
        uploadIfNeeded();
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(cell, cell);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, 0, 0, 0F, 0F, cols, rows, cols, rows);
        graphics.pose().popMatrix();
    }

    private void uploadIfNeeded() {
        var unknown = PipBoyTheme.panel();
        if (texture == null || textureWidth != cols || textureHeight != rows) {
            close();
            texture = new DynamicTexture(() -> "Atomic Dawn Pip-Boy map", cols, rows, true);
            textureWidth = cols;
            textureHeight = rows;
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
            textureDirty = true;
        }
        if (!textureDirty && unknown == textureUnknown) {
            return;
        }
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return;
        }
        for (var row = 0; row < rows; row++) {
            for (var col = 0; col < cols; col++) {
                var color = colors[row * cols + col];
                pixels.setPixel(col, row, color == UNKNOWN ? unknown : color);
            }
        }
        texture.upload();
        textureUnknown = unknown;
        textureDirty = false;
    }

    public void close() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
    }
}

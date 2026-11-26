package mod.azure.atomicdawn.client.hud.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.Rect;

public final class HudLayout {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int FILE_VERSION = 1;

    private static final long SEEN_WINDOW_MILLIS = 750L;

    private static final Map<HudElement, HudPlacement> PLACEMENTS = new EnumMap<>(HudElement.class);

    private static final Map<HudElement, Rect> LAST_RECTS = new EnumMap<>(HudElement.class);

    private static final Map<HudElement, Long> LAST_SEEN = new EnumMap<>(HudElement.class);

    private static boolean loaded;

    private static boolean editing;

    private HudLayout() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("atomicdawn-hud.json");
    }

    public static boolean push(ADGuiGraphics graphics, HudElement element, int x, int y, int width, int height) {
        ensureLoaded();
        LAST_RECTS.put(element, new Rect(x, y, width, height));
        LAST_SEEN.put(element, System.currentTimeMillis());
        var placement = PLACEMENTS.get(element);
        if (placement != null && placement.hidden() && !editing) {
            return false;
        }
        graphics.push();
        if (placement != null) {
            var window = Minecraft.getInstance().getWindow();
            var target = place(placement, width, height, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            graphics.translate(target.x(), target.y());
            graphics.scale(placement.scale(), placement.scale());
            graphics.translate(-x, -y);
        }
        return true;
    }

    public static void pop(ADGuiGraphics graphics) {
        graphics.pop();
    }

    public static boolean customized(HudElement element) {
        ensureLoaded();
        return PLACEMENTS.containsKey(element);
    }

    public static HudPlacement placement(HudElement element) {
        ensureLoaded();
        return PLACEMENTS.get(element);
    }

    public static void set(HudElement element, HudPlacement placement) {
        ensureLoaded();
        if (placement == null) {
            PLACEMENTS.remove(element);
        } else {
            PLACEMENTS.put(element, placement);
        }
    }

    public static Map<HudElement, HudPlacement> snapshot() {
        ensureLoaded();
        var copy = new EnumMap<HudElement, HudPlacement>(HudElement.class);
        copy.putAll(PLACEMENTS);
        return copy;
    }

    public static void restore(Map<HudElement, HudPlacement> snapshot) {
        PLACEMENTS.clear();
        PLACEMENTS.putAll(snapshot);
    }

    public static void setEditing(boolean value) {
        editing = value;
    }

    public static boolean active(HudElement element) {
        var seen = LAST_SEEN.get(element);
        return seen != null && System.currentTimeMillis() - seen < SEEN_WINDOW_MILLIS;
    }

    public static Rect defaultRect(HudElement element) {
        var rect = LAST_RECTS.get(element);
        return rect != null && active(element) ? rect : element.fallback();
    }

    public static Rect screenRect(HudElement element) {
        var base = defaultRect(element);
        var placement = placement(element);
        if (placement == null) {
            return base;
        }
        var window = Minecraft.getInstance().getWindow();
        var origin = place(
            placement,
            base.width(),
            base.height(),
            window.getGuiScaledWidth(),
            window.getGuiScaledHeight()
        );
        return new Rect(
            Math.round(origin.x()),
            Math.round(origin.y()),
            Math.round(base.width() * placement.scale()),
            Math.round(base.height() * placement.scale())
        );
    }

    public static HudPlacement fromScreen(
        float left,
        float top,
        float scaledWidth,
        float scaledHeight,
        float scale,
        boolean hidden,
        int screenWidth,
        int screenHeight
    ) {
        var anchor = HudAnchor.nearest(
            left + scaledWidth / 2F,
            top + scaledHeight / 2F,
            screenWidth,
            screenHeight
        );
        var offsetX = Math.round(left + anchor.fx * scaledWidth - anchor.fx * screenWidth);
        var offsetY = Math.round(top + anchor.fy * scaledHeight - anchor.fy * screenHeight);
        return new HudPlacement(anchor, offsetX, offsetY, scale, hidden);
    }

    private record Origin(
        float x,
        float y
    ) {}

    private static Origin place(HudPlacement placement, int width, int height, int screenWidth, int screenHeight) {
        var scaledWidth = width * placement.scale();
        var scaledHeight = height * placement.scale();
        var anchor = placement.anchor();
        var x = anchor.fx * screenWidth + placement.offsetX() - anchor.fx * scaledWidth;
        var y = anchor.fy * screenHeight + placement.offsetY() - anchor.fy * scaledHeight;
        x = Math.clamp(x, 0F, Math.max(0F, screenWidth - scaledWidth));
        y = Math.clamp(y, 0F, Math.max(0F, screenHeight - scaledHeight));
        return new Origin(Math.round(x), Math.round(y));
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    public static void load() {
        PLACEMENTS.clear();
        var path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            var root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            var elements = root.getAsJsonObject("elements");
            if (elements == null) {
                return;
            }
            for (var entry : elements.entrySet()) {
                var element = HudElement.byId(entry.getKey());
                if (element == null || !entry.getValue().isJsonObject()) {
                    continue;
                }
                var json = entry.getValue().getAsJsonObject();
                PLACEMENTS.put(
                    element,
                    new HudPlacement(
                        HudAnchor.byId(json.has("anchor") ? json.get("anchor").getAsString() : ""),
                        json.has("x") ? json.get("x").getAsInt() : 0,
                        json.has("y") ? json.get("y").getAsInt() : 0,
                        json.has("scale") ? json.get("scale").getAsFloat() : 1F,
                        json.has("hidden") && json.get("hidden").getAsBoolean()
                    )
                );
            }
        } catch (IOException | RuntimeException exception) {
            AtomicDawn.LOGGER.warn("Could not read HUD layout from {}", path, exception);
        }
    }

    public static void save() {
        ensureLoaded();
        var root = new JsonObject();
        root.addProperty("version", FILE_VERSION);
        var elements = new JsonObject();
        for (var entry : PLACEMENTS.entrySet()) {
            var placement = entry.getValue();
            var json = new JsonObject();
            json.addProperty("anchor", placement.anchor().id());
            json.addProperty("x", placement.offsetX());
            json.addProperty("y", placement.offsetY());
            json.addProperty("scale", placement.scale());
            json.addProperty("hidden", placement.hidden());
            elements.add(entry.getKey().id(), json);
        }
        root.add("elements", elements);
        var path = file();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            AtomicDawn.LOGGER.warn("Could not save HUD layout to {}", path, exception);
        }
    }
}

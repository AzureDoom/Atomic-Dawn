package mod.azure.atomicdawn.config;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class ADColors {

    private static final ConcurrentHashMap<String, Integer> CACHE = new ConcurrentHashMap<>();

    private ADColors() {}

    public static int argb(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        var parsed = CACHE.computeIfAbsent(value, ADColors::parse);
        return parsed == null ? fallback : parsed;
    }

    public static int rgb(String value, int fallback) {
        return argb(value, 0xFF000000 | fallback) & 0xFFFFFF;
    }

    private static Integer parse(String raw) {
        var text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("#")) {
            text = text.substring(1);
        } else if (text.startsWith("0x")) {
            text = text.substring(2);
        }
        if (text.length() != 6 && text.length() != 8) {
            return null;
        }
        try {
            var value = Long.parseLong(text, 16);
            return text.length() == 6 ? (int) (0xFF000000L | value) : (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

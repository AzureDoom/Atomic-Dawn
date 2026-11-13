package mod.azure.atomicdawn.pipboy;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class PipBoyNames {

    private PipBoyNames() {}

    public static String pretty(String path) {
        var last = path.substring(path.lastIndexOf('/') + 1).replace('.', '_');
        var builder = new StringBuilder();
        for (var word : last.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return builder.toString();
    }

    public static String key(String prefix, Identifier id) {
        return prefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static MutableComponent named(String prefix, Identifier id) {
        return Component.translatableWithFallback(key(prefix, id), pretty(id.getPath()));
    }

    public static MutableComponent fromKey(String key) {
        if (key.isEmpty()) {
            return Component.empty();
        }
        return Component.translatableWithFallback(key, pretty(key));
    }

    public static MutableComponent location(Identifier structure) {
        return named("location", structure);
    }

    public static MutableComponent biome(Identifier biome) {
        return named("biome", biome);
    }

    public static MutableComponent quest(Identifier quest) {
        return named("quest", quest);
    }

    public static MutableComponent holotape(Identifier holotape) {
        return named("holotape", holotape);
    }

    public static MutableComponent tag(String registry, Identifier tag) {
        return Component.translatableWithFallback(
            key("tag." + registry, tag),
            "#" + pretty(tag.getPath())
        );
    }
}

package mod.azure.atomicdawn.camp;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public final class CampBlueprints {

    public static final String PERSONAL = "atomicdawn_personal";

    public record Ref(
        Identifier id,
        CampBlueprint blueprint,
        boolean personal
    ) {}

    private CampBlueprints() {}

    public static boolean personal(Identifier id) {
        return id.getNamespace().equals(PERSONAL);
    }

    public static Identifier personalId(String key) {
        return ADRegistryUtil.id(PERSONAL, key);
    }

    public static String slug(String name) {
        var builder = new StringBuilder();
        for (var c : name.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                builder.append(c);
            } else if (Character.isWhitespace(c) || c == '.') {
                builder.append('_');
            }
        }
        var slug = builder.toString().replaceAll("_+", "_");
        while (slug.startsWith("_")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("_")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug.length() > CampBlueprint.MAX_NAME_LENGTH ? slug.substring(0, CampBlueprint.MAX_NAME_LENGTH) : slug;
    }

    public static List<Holder.Reference<CampBlueprint>> datapack(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.CAMP_BLUEPRINT)
            .map(
                registry -> registry.listElements()
                    .sorted(Comparator.comparing(holder -> holder.key().identifier().toString()))
                    .toList()
            )
            .orElse(List.of());
    }

    public static List<Ref> all(Player player) {
        var list = new ArrayList<Ref>();
        CampBlueprintLibrary.of(player)
            .blueprints()
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> list.add(new Ref(personalId(entry.getKey()), entry.getValue(), true)));
        for (var holder : datapack(player.level().registryAccess())) {
            list.add(new Ref(holder.key().identifier(), holder.value(), false));
        }
        return list;
    }

    public static Optional<CampBlueprint> get(Player player, Identifier id) {
        if (personal(id)) {
            return CampBlueprintLibrary.of(player).get(id.getPath());
        }
        return ADRegistryUtil.optionalRegistry(player.level().registryAccess(), ADRegistries.CAMP_BLUEPRINT)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.CAMP_BLUEPRINT, id)))
            .map(Holder.Reference::value);
    }
}

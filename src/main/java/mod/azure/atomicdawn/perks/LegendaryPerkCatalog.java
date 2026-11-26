package mod.azure.atomicdawn.perks;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public final class LegendaryPerkCatalog {

    private static final Comparator<Holder.Reference<LegendaryPerkDefinition>> ORDER =
        Comparator.<Holder.Reference<LegendaryPerkDefinition>>comparingInt(holder -> holder.value().sortOrder())
            .thenComparing(holder -> holder.key().identifier().toString());

    private LegendaryPerkCatalog() {}

    public static ResourceKey<LegendaryPerkDefinition> key(Identifier id) {
        return ResourceKey.create(ADRegistries.LEGENDARY_PERK, id);
    }

    public static Optional<LegendaryPerkDefinition> get(RegistryAccess access, Identifier id) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.LEGENDARY_PERK)
            .flatMap(registry -> registry.get(key(id)))
            .map(Holder::value)
            .filter(LegendaryPerkDefinition::enabled);
    }

    public static List<Holder.Reference<LegendaryPerkDefinition>> all(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.LEGENDARY_PERK)
            .map(registry -> registry.listElements().filter(holder -> holder.value().enabled()).sorted(ORDER).toList())
            .orElse(List.of());
    }
}

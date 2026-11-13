package mod.azure.atomicdawn.perks;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.special.SpecialAttribute;

public final class PerkCatalog {

    private static final Comparator<Holder.Reference<PerkDefinition>> ORDER =
        Comparator.<Holder.Reference<PerkDefinition>>comparingInt(holder -> holder.value().special().ordinal())
            .thenComparingInt(holder -> holder.value().sortOrder())
            .thenComparing(holder -> holder.key().identifier().toString());

    private PerkCatalog() {}

    public static ResourceKey<PerkDefinition> key(Identifier id) {
        return ResourceKey.create(ADRegistries.PERK, id);
    }

    public static Optional<PerkDefinition> get(RegistryAccess access, Identifier id) {
        return access.lookup(ADRegistries.PERK)
            .flatMap(registry -> registry.get(key(id)))
            .map(Holder::value)
            .filter(PerkDefinition::enabled);
    }

    public static List<Holder.Reference<PerkDefinition>> all(RegistryAccess access) {
        return access.lookup(ADRegistries.PERK)
            .map(registry -> registry.listElements().filter(holder -> holder.value().enabled()).sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static List<Holder.Reference<PerkDefinition>> byCategory(RegistryAccess access, SpecialAttribute special) {
        return all(access).stream().filter(holder -> holder.value().special() == special).toList();
    }
}

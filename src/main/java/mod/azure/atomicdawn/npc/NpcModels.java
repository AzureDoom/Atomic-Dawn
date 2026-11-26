package mod.azure.atomicdawn.npc;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public final class NpcModels {

    public static final EntityDimensions FALLBACK = EntityDimensions.scalable(0.6F, 1.8F).withEyeHeight(1.62F);

    private static final float MAX_WIDTH = 4F;

    private static final float MAX_HEIGHT = 6F;

    private NpcModels() {}

    public static Optional<NpcModelDefinition> get(RegistryAccess access, Identifier id) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.NPC_MODEL)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.NPC_MODEL, id)))
            .map(Holder::value);
    }

    public static List<Identifier> ids(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.NPC_MODEL)
            .map(
                registry -> registry.listElementIds()
                    .map(ResourceKey::identifier)
                    .sorted(Comparator.comparing(Identifier::toString))
                    .toList()
            )
            .orElse(List.of());
    }

    public static EntityDimensions dimensions(RegistryAccess access, NpcAppearance appearance) {
        EntityDimensions base;
        if (appearance.kind() == NpcAppearance.Kind.AZURE) {
            base = access == null
                ? FALLBACK
                : get(access, appearance.model()).map(NpcModelDefinition::dimensions).orElse(FALLBACK);
        } else {
            base = BuiltInRegistries.ENTITY_TYPE.getOptional(appearance.model())
                .filter(type -> !BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(NpcIds.NPC_TYPE))
                .map(EntityType::getDimensions)
                .orElse(FALLBACK);
        }
        if (base.width() > MAX_WIDTH || base.height() > MAX_HEIGHT) {
            var factor = Math.min(MAX_WIDTH / base.width(), MAX_HEIGHT / base.height());
            base = base.scale(factor);
        }
        return base.scale(appearance.scale());
    }
}

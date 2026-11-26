package mod.azure.atomicdawn.limb;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.helper.ADEntityUtil;
import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.vats.BodyPart;

public final class LimbProfiles {

    public static final TagKey<EntityType<?>> NO_LIMBS = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("no_limbs")
    );

    private static final Map<EntityType<?>, LimbProfile> CACHE = new ConcurrentHashMap<>();

    private LimbProfiles() {}

    public static void invalidate() {
        CACHE.clear();
    }

    public static LimbProfile of(Entity entity) {
        return CACHE.computeIfAbsent(entity.getType(), type -> lookup(entity, type));
    }

    private static LimbProfile lookup(Entity entity, EntityType<?> type) {
        var holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
        var best = LimbProfile.DEFAULT;
        var registry = ADRegistryUtil.optionalRegistry(entity.level().registryAccess(), ADRegistries.LIMB_PROFILE);
        if (registry.isEmpty()) {
            return best;
        }
        for (var profile : registry.get().listElements().toList()) {
            var value = profile.value();
            if (value.priority() >= best.priority() && value.entities().contains(holder)) {
                best = value;
            }
        }
        return best;
    }

    public static boolean immune(Entity entity) {
        return ADEntityUtil.isIn(entity, NO_LIMBS) || of(entity).immune();
    }

    public static @Nullable BodyPart classify(Entity entity, String name, String bone, Predicate<String> hasTag) {
        var mapped = of(entity).boneMatch(name, bone);
        return mapped != null ? mapped : BodyPart.classify(name, bone, hasTag);
    }

    public static @Nullable BodyPart present(Entity entity, BodyPart part) {
        var profile = of(entity);
        if (profile.has(part)) {
            return part;
        }
        var substitute = switch (part) {
            case LEFT_ARM -> BodyPart.LEFT_LEG;
            case RIGHT_ARM -> BodyPart.RIGHT_LEG;
            default -> BodyPart.TORSO;
        };
        if (profile.has(substitute)) {
            return substitute;
        }
        if (profile.has(BodyPart.TORSO)) {
            return BodyPart.TORSO;
        }
        return profile.parts().isEmpty() ? null : profile.parts().getFirst();
    }
}

package mod.azure.atomicdawn.helper;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ADEntityUtil {

    private ADEntityUtil() {}

    public static boolean hurt(Entity target, ServerLevel level, DamageSource source, float amount) {
        return target.hurtServer(level, source, amount);
    }

    public static void knockback(
        LivingEntity target,
        double strength,
        double x,
        double z,
        DamageSource source,
        float damage
    ) {
        target.knockback(strength, x, z, source, damage);
    }

    @Nullable
    public static <T extends Entity> T create(EntityType<T> type, Level level, EntitySpawnReason reason) {
        return type.create(level, reason);
    }

    public static Holder<EntityType<?>> typeHolder(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());
    }

    public static boolean isIn(Entity entity, TagKey<EntityType<?>> tag) {
        return typeHolder(entity).is(tag);
    }
}

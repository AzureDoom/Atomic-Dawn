package mod.azure.atomicdawn.armor.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADEntityUtil;
import mod.azure.atomicdawn.helper.ADRegistryUtil;

@SuppressWarnings("NullableProblems")
public record ArmorEffect(
    ArmorEffectType type,
    float amount,
    Optional<String> target,
    List<Holder<MobEffect>> effects
) {

    public static final Codec<ArmorEffect> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            ArmorEffectType.CODEC.fieldOf("type").forGetter(ArmorEffect::type),
            Codec.FLOAT.optionalFieldOf("amount", 1F).forGetter(ArmorEffect::amount),
            Codec.STRING.optionalFieldOf("target").forGetter(ArmorEffect::target),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                .listOf()
                .optionalFieldOf("effects", List.of())
                .forGetter(ArmorEffect::effects)
        ).apply(instance, ArmorEffect::new)
    );

    public boolean targets(Entity attacker) {
        if (target.isEmpty()) {
            return true;
        }
        if (attacker == null) {
            return false;
        }
        var value = target.get();
        if (value.startsWith("#")) {
            var id = ADRegistryUtil.tryParse(value.substring(1));
            return id != null && ADEntityUtil.isIn(attacker, TagKey.create(Registries.ENTITY_TYPE, id));
        }
        return switch (value) {
            case "player" -> attacker instanceof Player;
            case "animal" -> attacker instanceof Animal;
            default -> false;
        };
    }
}

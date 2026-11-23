package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.mutation.MutationService;
import mod.azure.atomicdawn.thirst.Thirst;
import mod.azure.atomicdawn.thirst.ThirstService;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;
import mod.azure.atomicdawn.weapon.legendary.LegendaryHooks;

public record ScalingBonusBehavior(
    Source source,
    float maxBonus,
    float minBonus,
    float units
) implements LegendaryBehavior {

    public static final MapCodec<ScalingBonusBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Source.CODEC.fieldOf("source").forGetter(ScalingBonusBehavior::source),
            Codec.floatRange(-1, 10).optionalFieldOf("max_bonus", 0.3F).forGetter(ScalingBonusBehavior::maxBonus),
            Codec.floatRange(-1, 10).optionalFieldOf("min_bonus", 0F).forGetter(ScalingBonusBehavior::minBonus),
            Codec.floatRange(0.001F, 100000F).optionalFieldOf("units", 1F).forGetter(ScalingBonusBehavior::units)
        ).apply(instance, ScalingBonusBehavior::new)
    );

    @Override
    public MapCodec<ScalingBonusBehavior> codec() {
        return CODEC;
    }

    @Override
    public float damageMultiplier(LegendaryContext context) {
        var fraction = Math.clamp(source.fraction(context, units), 0F, 1F);
        return Math.max(0F, 1F + minBonus + (maxBonus - minBonus) * fraction);
    }

    public enum Source implements StringRepresentable {

        KILL_STREAK("kill_streak"),
        CURRENCY("currency"),
        LOW_ARMOR("low_armor"),
        HEALTH("health"),
        HARMFUL_EFFECTS("harmful_effects"),
        FERAL("feral"),
        LUCIDITY("lucidity"),
        NOURISHMENT("nourishment"),
        MUTATIONS("mutations"),
        NIGHT("night");

        public static final Codec<Source> CODEC = StringRepresentable.fromEnum(Source::values);

        private final String name;

        Source(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        float fraction(LegendaryContext context, float units) {
            var player = context.attacker();
            return switch (this) {
                case KILL_STREAK -> context.shooter().killStreak(context.level().getGameTime()) / units;
                case CURRENCY -> LegendaryHooks.currencyHeld(player) / units;
                case LOW_ARMOR -> 1F - (float) player.getAttributeValue(Attributes.ARMOR) / units;
                case HEALTH -> player.getHealth() / Math.max(1F, player.getMaxHealth());
                case HARMFUL_EFFECTS -> player.getActiveEffects()
                    .stream()
                    .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    .count() / units;
                case FERAL -> GhoulService.isGhoul(player) ? 1F - GhoulService.stabilityFraction(player) : 0F;
                case LUCIDITY -> GhoulService.isGhoul(player) ? GhoulService.stabilityFraction(player) : 0F;
                case NOURISHMENT -> nourishment(player);
                case MUTATIONS -> MutationService.get(player).size() / units;
                case NIGHT -> LegendaryHooks.nightness(context.level());
            };
        }

        private static float nourishment(Player player) {
            if (GhoulService.isGhoul(player)) {
                return GhoulService.glowFraction(player);
            }
            var food = player.getFoodData().getFoodLevel() / 20F;
            if (!ThirstService.enabled()) {
                return food;
            }
            return (food + ThirstService.thirst(player) / Thirst.MAX) / 2F;
        }
    }
}

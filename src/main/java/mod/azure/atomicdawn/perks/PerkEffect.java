package mod.azure.atomicdawn.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.weapon.definition.WeaponCategory;

public record PerkEffect(
    PerkEffectType type,
    float value,
    List<WeaponCategory> categories,
    Optional<Boolean> automatic
) {

    public static final Codec<PerkEffect> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            PerkEffectType.CODEC.fieldOf("type").forGetter(PerkEffect::type),
            Codec.floatRange(-1000F, 1000F).fieldOf("value").forGetter(PerkEffect::value),
            WeaponCategory.CODEC.listOf().optionalFieldOf("categories", List.of()).forGetter(PerkEffect::categories),
            Codec.BOOL.optionalFieldOf("automatic").forGetter(PerkEffect::automatic)
        ).apply(instance, PerkEffect::new)
    );

    public boolean matches(WeaponCategory category, boolean isAutomatic) {
        if (!categories.isEmpty() && !categories.contains(category)) {
            return false;
        }
        return automatic.map(required -> required == isAutomatic).orElse(true);
    }

    public MutableComponent describe() {
        var shown = type.reduction() ? -value : value;
        var formatted = type.percent()
            ? String.format(Locale.ROOT, "%+.0f%%", shown * 100F)
            : Math.abs(shown - Math.round(shown)) < 0.01F
                ? String.format(Locale.ROOT, type.signed() ? "%+.0f" : "%.0f", shown)
                : String.format(Locale.ROOT, type.signed() ? "%+.1f" : "%.1f", shown);
        var line = Component.translatable(type.translationKey(), formatted);
        if (!categories.isEmpty() || automatic.isPresent()) {
            var filter = Component.literal(" (");
            for (var i = 0; i < categories.size(); i++) {
                if (i > 0) {
                    filter.append(", ");
                }
                filter.append(
                    Component.translatable("weapon_category.atomicdawn." + categories.get(i).getSerializedName())
                );
            }
            automatic.ifPresent(auto -> {
                if (!categories.isEmpty()) {
                    filter.append(", ");
                }
                filter.append(
                    Component.translatable(
                        auto ? "perk_filter.atomicdawn.automatic" : "perk_filter.atomicdawn.non_automatic"
                    )
                );
            });
            line.append(filter.append(")"));
        }
        return line;
    }
}

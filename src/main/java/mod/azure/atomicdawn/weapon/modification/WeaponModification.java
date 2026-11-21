package mod.azure.atomicdawn.weapon.modification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.definition.FireMode;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public record WeaponModification(
    ModSlot slot,
    HolderSet<WeaponDefinition> weapons,
    List<StatModifier> modifiers,
    Optional<List<FireMode>> fireModes,
    List<String> showBones,
    Optional<Identifier> texture,
    List<SizedIngredient> cost,
    boolean requiresUnlock,
    Optional<WeaponScope> scope
) {

    public static final Codec<WeaponModification> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            ModSlot.CODEC.fieldOf("slot").forGetter(WeaponModification::slot),
            RegistryCodecs.holderSet(ADRegistries.WEAPON_DEFINITION)
                .fieldOf("weapons")
                .forGetter(WeaponModification::weapons),
            StatModifier.CODEC.listOf()
                .optionalFieldOf("modifiers", List.of())
                .forGetter(WeaponModification::modifiers),
            FireMode.CODEC.listOf().optionalFieldOf("fire_modes").forGetter(WeaponModification::fireModes),
            Codec.STRING.listOf().optionalFieldOf("show_bones", List.of()).forGetter(WeaponModification::showBones),
            Identifier.CODEC.optionalFieldOf("texture").forGetter(WeaponModification::texture),
            SizedIngredient.NESTED_CODEC.listOf()
                .optionalFieldOf("cost", List.of())
                .forGetter(WeaponModification::cost),
            Codec.BOOL.optionalFieldOf("requires_unlock", false).forGetter(WeaponModification::requiresUnlock),
            WeaponScope.CODEC.optionalFieldOf("scope").forGetter(WeaponModification::scope)
        ).apply(instance, WeaponModification::new)
    );
}

package mod.azure.atomicdawn.weapon.modification;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;

import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;

public final class ModNames {

    private ModNames() {}

    public static MutableComponent mod(ResourceKey<WeaponModification> key) {
        var id = key.identifier();
        return Component.translatable("weapon_mod." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
    }

    public static MutableComponent legendary(ResourceKey<LegendaryEffect> key) {
        return Component.translatable(LegendaryEffect.translationKey(key));
    }

    public static MutableComponent legendaryDescription(ResourceKey<LegendaryEffect> key) {
        return Component.translatable(LegendaryEffect.translationKey(key) + ".desc");
    }
}

package mod.azure.atomicdawn.client.render;

import mod.azure.azurelib.model.AzBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;

public final class WeaponModelParts {

    private static final Map<ResourceKey<WeaponDefinition>, Set<String>> MANAGED = new HashMap<>();

    private static RegistryAccess cachedAccess;

    private WeaponModelParts() {}

    public static void applyVisibility(ItemStack stack, AzBakedModel model) {
        PowerArmorArmBones.apply(model);
        var level = Minecraft.getInstance().level;
        if (level == null || !(stack.getItem() instanceof WeaponItem weapon)) {
            return;
        }
        var base = Weapons.baseDefinition(level, stack);
        if (base == null) {
            return;
        }
        var access = level.registryAccess();
        var managed = managedBones(access, weapon.definitionKey());
        if (managed.isEmpty()) {
            return;
        }
        var installed = ModificationResolver.resolve(
            access,
            weapon.definitionKey(),
            base,
            ModificationResolver.installed(stack, base)
        );
        var visible = ModificationResolver.visibleBones(installed);
        for (var boneName : managed) {
            var bone = model.getBoneOrNull(boneName);
            if (bone != null) {
                bone.setHidden(!visible.contains(boneName));
            }
        }
    }

    public static Optional<Identifier> textureOverride(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(stack.getItem() instanceof WeaponItem weapon)) {
            return Optional.empty();
        }
        var base = Weapons.baseDefinition(level, stack);
        if (base == null) {
            return Optional.empty();
        }
        var installed = ModificationResolver.resolve(
            level.registryAccess(),
            weapon.definitionKey(),
            base,
            ModificationResolver.installed(stack, base)
        );
        return ModificationResolver.textureOverride(installed);
    }

    private static Set<String> managedBones(RegistryAccess access, ResourceKey<WeaponDefinition> weaponKey) {
        if (access != cachedAccess) {
            MANAGED.clear();
            cachedAccess = access;
        }
        return MANAGED.computeIfAbsent(weaponKey, key -> ModificationResolver.managedBones(access, key));
    }
}

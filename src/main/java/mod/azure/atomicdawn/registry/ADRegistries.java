package mod.azure.atomicdawn.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.ArmorRecipe;
import mod.azure.atomicdawn.armor.upgrade.ArmorLegendary;
import mod.azure.atomicdawn.armor.upgrade.ArmorMod;
import mod.azure.atomicdawn.camp.CampBlueprint;
import mod.azure.atomicdawn.camp.CampBuildable;
import mod.azure.atomicdawn.crafting.TinkerRecipe;
import mod.azure.atomicdawn.limb.LimbProfile;
import mod.azure.atomicdawn.npc.NpcModelDefinition;
import mod.azure.atomicdawn.npc.dialogue.NpcDialogue;
import mod.azure.atomicdawn.perks.LegendaryPerkDefinition;
import mod.azure.atomicdawn.perks.PerkDefinition;
import mod.azure.atomicdawn.pipboy.bestiary.BestiaryEntry;
import mod.azure.atomicdawn.pipboy.holotape.Holotape;
import mod.azure.atomicdawn.powerarmor.PowerArmorRecipe;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorLegendary;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorMod;
import mod.azure.atomicdawn.quest.QuestDefinition;
import mod.azure.atomicdawn.radio.RadioStation;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;
import mod.azure.atomicdawn.weapon.modification.WeaponModification;

public final class ADRegistries {

    public static final ResourceKey<Registry<WeaponDefinition>> WEAPON_DEFINITION = ResourceKey.createRegistryKey(
        AtomicDawn.id("weapon")
    );

    public static final ResourceKey<Registry<WeaponModification>> WEAPON_MODIFICATION = ResourceKey
        .createRegistryKey(AtomicDawn.id("weapon_mod"));

    public static final ResourceKey<Registry<LegendaryEffect>> LEGENDARY_EFFECT = ResourceKey
        .createRegistryKey(AtomicDawn.id("legendary_effect"));

    public static final ResourceKey<Registry<TinkerRecipe>> TINKER_RECIPE = ResourceKey
        .createRegistryKey(AtomicDawn.id("tinker_recipe"));

    public static final ResourceKey<Registry<PerkDefinition>> PERK = ResourceKey.createRegistryKey(
        AtomicDawn.id("perk")
    );

    public static final ResourceKey<Registry<LegendaryPerkDefinition>> LEGENDARY_PERK = ResourceKey
        .createRegistryKey(AtomicDawn.id("legendary_perk"));

    public static final ResourceKey<Registry<RadioStation>> RADIO_STATION = ResourceKey.createRegistryKey(
        AtomicDawn.id("radio_station")
    );

    public static final ResourceKey<Registry<QuestDefinition>> QUEST = ResourceKey.createRegistryKey(
        AtomicDawn.id("quest")
    );

    public static final ResourceKey<Registry<BestiaryEntry>> BESTIARY = ResourceKey.createRegistryKey(
        AtomicDawn.id("bestiary")
    );

    public static final ResourceKey<Registry<Holotape>> HOLOTAPE = ResourceKey.createRegistryKey(
        AtomicDawn.id("holotape")
    );

    public static final ResourceKey<Registry<PowerArmorRecipe>> POWER_ARMOR_RECIPE = ResourceKey.createRegistryKey(
        AtomicDawn.id("power_armor_recipe")
    );

    public static final ResourceKey<Registry<PowerArmorMod>> POWER_ARMOR_MOD = ResourceKey.createRegistryKey(
        AtomicDawn.id("power_armor_mod")
    );

    public static final ResourceKey<Registry<PowerArmorLegendary>> POWER_ARMOR_LEGENDARY = ResourceKey
        .createRegistryKey(AtomicDawn.id("power_armor_legendary"));

    public static final ResourceKey<Registry<NpcModelDefinition>> NPC_MODEL = ResourceKey.createRegistryKey(
        AtomicDawn.id("npc_model")
    );

    public static final ResourceKey<Registry<NpcDialogue>> NPC_DIALOGUE = ResourceKey.createRegistryKey(
        AtomicDawn.id("npc_dialogue")
    );

    public static final ResourceKey<Registry<ArmorRecipe>> ARMOR_RECIPE = ResourceKey.createRegistryKey(
        AtomicDawn.id("armor_recipe")
    );

    public static final ResourceKey<Registry<ArmorMod>> ARMOR_MOD = ResourceKey.createRegistryKey(
        AtomicDawn.id("armor_mod")
    );

    public static final ResourceKey<Registry<ArmorLegendary>> ARMOR_LEGENDARY = ResourceKey.createRegistryKey(
        AtomicDawn.id("armor_legendary")
    );

    public static <T> ResourceKey<T> key(ResourceKey<Registry<T>> registry, Identifier id) {
        return ResourceKey.create(registry, id);
    }

    public static final ResourceKey<Registry<CampBuildable>> CAMP_BUILDABLE = ResourceKey.createRegistryKey(
        AtomicDawn.id("camp_buildable")
    );

    public static final ResourceKey<Registry<CampBlueprint>> CAMP_BLUEPRINT = ResourceKey.createRegistryKey(
        AtomicDawn.id("camp_blueprint")
    );

    public static final ResourceKey<Registry<LimbProfile>> LIMB_PROFILE = ResourceKey.createRegistryKey(
        AtomicDawn.id("limb_profile")
    );

    private ADRegistries() {}
}

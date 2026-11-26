package mod.azure.atomicdawn;

import com.mojang.logging.LogUtils;
import mod.azure.azurelib.AzureLibMod;
import mod.azure.azurelib.animation.cache.AzIdentityRegistry;
import mod.azure.azurelib.config.ConfigHolder;
import mod.azure.azurelib.config.format.ConfigFormats;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
import org.slf4j.Logger;

import mod.azure.atomicdawn.armor.ArmorRecipe;
import mod.azure.atomicdawn.armor.upgrade.ArmorEvents;
import mod.azure.atomicdawn.armor.upgrade.ArmorLegendary;
import mod.azure.atomicdawn.armor.upgrade.ArmorMod;
import mod.azure.atomicdawn.camp.CampAttachments;
import mod.azure.atomicdawn.camp.CampBlueprint;
import mod.azure.atomicdawn.camp.CampBuildable;
import mod.azure.atomicdawn.camp.CampPlanLootModifier;
import mod.azure.atomicdawn.config.ADConfig;
import mod.azure.atomicdawn.crafting.TinkerRecipe;
import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.item.NukaColaEffects;
import mod.azure.atomicdawn.junk.ScrapRecipe;
import mod.azure.atomicdawn.limb.LimbProfile;
import mod.azure.atomicdawn.mutation.MutationService;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.npc.NpcModelDefinition;
import mod.azure.atomicdawn.npc.dialogue.NpcDialogue;
import mod.azure.atomicdawn.perks.LegendaryPerkDefinition;
import mod.azure.atomicdawn.perks.PerkDefinition;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.pipboy.bestiary.BestiaryEntry;
import mod.azure.atomicdawn.pipboy.holotape.Holotape;
import mod.azure.atomicdawn.powerarmor.PowerArmorRecipe;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorLegendary;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorMod;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgradeEvents;
import mod.azure.atomicdawn.quest.QuestDefinition;
import mod.azure.atomicdawn.radio.RadioStation;
import mod.azure.atomicdawn.registry.*;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.story.CompanionProfile;
import mod.azure.atomicdawn.story.Faction;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehaviors;
import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;
import mod.azure.atomicdawn.weapon.legendary.LegendaryHooks;
import mod.azure.atomicdawn.weapon.modification.WeaponModification;
import mod.azure.atomicdawn.worldgen.ADWorldgen;

@SuppressWarnings("unused")
@Mod(AtomicDawn.MOD_ID)
public final class AtomicDawn {

    public static final String MOD_ID = "atomicdawn";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static ADConfig CONFIG;

    public static ConfigHolder<ADConfig> CONFIG_HOLDER;

    public AtomicDawn(IEventBus modBus, ModContainer container) {
        ADDataComponents.REGISTER.register(modBus);
        ADFluids.TYPES.register(modBus);
        ADFluids.FLUIDS.register(modBus);
        ADBlocks.REGISTER.register(modBus);
        ADEntities.REGISTER.register(modBus);
        ADMobEffects.REGISTER.register(modBus);
        ADItems.REGISTER.register(modBus);
        ADBlockEntities.REGISTER.register(modBus);
        ADStructureTypes.REGISTER.register(modBus);
        ADStructureTypes.PIECES.register(modBus);
        ADMenus.REGISTER.register(modBus);
        ADSounds.REGISTER.register(modBus);
        ADAttachments.REGISTER.register(modBus);
        CampAttachments.REGISTER.register(modBus);
        CampPlanLootModifier.REGISTER.register(modBus);
        ADCreativeTabs.REGISTER.register(modBus);
        LegendaryBehaviors.REGISTER.register(modBus);
        ADDataSerializers.REGISTER.register(modBus);

        modBus.addListener(LegendaryBehaviors::registerRegistry);
        modBus.addListener(AtomicDawn::registerDatapackRegistries);
        modBus.addListener(AtomicDawn::commonSetup);
        modBus.addListener(ADNetwork::register);
        SpecialStatResolver.registerTemporarySource(PowerArmorUpgradeEvents::specialBonus);
        SpecialStatResolver.registerTemporarySource(MutationService::specialBonus);
        SpecialStatResolver.registerTemporarySource(ArmorEvents::specialBonus);
        SpecialStatResolver.registerTemporarySource(PerkHooks::specialBonus);
        SpecialStatResolver.registerTemporarySource(NukaColaEffects::specialBonus);
        SpecialStatResolver.registerTemporarySource(LegendaryHooks::specialBonus);
        modBus.addListener((ModConfigEvent.Loading ignored) -> SpecialStatResolver.invalidateAll());
        modBus.addListener((ModConfigEvent.Reloading ignored) -> SpecialStatResolver.invalidateAll());

        CONFIG_HOLDER = AzureLibMod.registerConfig(ADConfig.class, ConfigFormats.json());
        CONFIG = CONFIG_HOLDER.getConfigInstance();
    }

    public static Identifier id(String path) {
        return ADRegistryUtil.id(MOD_ID, path);
    }

    private static void registerDatapackRegistries(NewDatapackRegistryEvent event) {
        event.worldRegistry(ADRegistries.WEAPON_DEFINITION, WeaponDefinition.CODEC, WeaponDefinition.CODEC);
        event.worldRegistry(ADRegistries.WEAPON_MODIFICATION, WeaponModification.CODEC, WeaponModification.CODEC);
        event.worldRegistry(ADRegistries.LEGENDARY_EFFECT, LegendaryEffect.CODEC, LegendaryEffect.CODEC);
        event.worldRegistry(ADRegistries.TINKER_RECIPE, TinkerRecipe.CODEC, TinkerRecipe.CODEC);
        event.worldRegistry(ADRegistries.PERK, PerkDefinition.CODEC, PerkDefinition.CODEC);
        event.worldRegistry(
            ADRegistries.LEGENDARY_PERK,
            LegendaryPerkDefinition.CODEC,
            LegendaryPerkDefinition.CODEC
        );
        event.worldRegistry(ADRegistries.RADIO_STATION, RadioStation.CODEC, RadioStation.CODEC);
        event.worldRegistry(ADRegistries.QUEST, QuestDefinition.CODEC, QuestDefinition.CODEC);
        event.worldRegistry(ADRegistries.BESTIARY, BestiaryEntry.CODEC, BestiaryEntry.CODEC);
        event.worldRegistry(ADRegistries.HOLOTAPE, Holotape.CODEC, Holotape.CODEC);
        event.worldRegistry(ADRegistries.POWER_ARMOR_RECIPE, PowerArmorRecipe.CODEC, PowerArmorRecipe.CODEC);
        event.worldRegistry(ADRegistries.POWER_ARMOR_MOD, PowerArmorMod.CODEC, PowerArmorMod.CODEC);
        event.worldRegistry(ADRegistries.POWER_ARMOR_LEGENDARY, PowerArmorLegendary.CODEC, PowerArmorLegendary.CODEC);
        event.worldRegistry(ADRegistries.NPC_MODEL, NpcModelDefinition.CODEC, NpcModelDefinition.CODEC);
        event.worldRegistry(ADRegistries.NPC_DIALOGUE, NpcDialogue.CODEC, NpcDialogue.CODEC);
        event.worldRegistry(ADRegistries.ARMOR_RECIPE, ArmorRecipe.CODEC, ArmorRecipe.CODEC);
        event.worldRegistry(ADRegistries.ARMOR_MOD, ArmorMod.CODEC, ArmorMod.CODEC);
        event.worldRegistry(ADRegistries.ARMOR_LEGENDARY, ArmorLegendary.CODEC, ArmorLegendary.CODEC);
        event.worldRegistry(ADRegistries.CAMP_BUILDABLE, CampBuildable.CODEC, CampBuildable.CODEC);
        event.worldRegistry(ADRegistries.CAMP_BLUEPRINT, CampBlueprint.CODEC, CampBlueprint.NETWORK_CODEC);
        event.worldRegistry(ADRegistries.LIMB_PROFILE, LimbProfile.CODEC, LimbProfile.CODEC);
        event.worldRegistry(ADRegistries.SCRAP_RECIPE, ScrapRecipe.CODEC, ScrapRecipe.CODEC);
        event.worldRegistry(ADRegistries.FACTION, Faction.CODEC, Faction.CODEC);
        event.worldRegistry(ADRegistries.COMPANION, CompanionProfile.CODEC, CompanionProfile.CODEC);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ADItems.weapons().forEach(AzIdentityRegistry::register));
        ADWorldgen.commonSetup(event);
    }
}

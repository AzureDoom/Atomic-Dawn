package mod.azure.atomicdawn.client;

import mod.azure.azurelib.render.item.AzItemRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.camp.CampWorkshopHud;
import mod.azure.atomicdawn.client.effect.NukeEffects;
import mod.azure.atomicdawn.client.fluid.IrradiatedWaterClient;
import mod.azure.atomicdawn.client.ghoul.GhoulClient;
import mod.azure.atomicdawn.client.ghoul.GhoulHud;
import mod.azure.atomicdawn.client.hud.PartyHud;
import mod.azure.atomicdawn.client.hud.WaypointHud;
import mod.azure.atomicdawn.client.hud.WeaponHud;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.client.network.ClientPayloadHandlers;
import mod.azure.atomicdawn.client.npc.NpcRenderer;
import mod.azure.atomicdawn.client.pipboy.PipBoyArmLayer;
import mod.azure.atomicdawn.client.pipboy.PipBoyNotificationHud;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorHud;
import mod.azure.atomicdawn.client.radiation.RadiationHud;
import mod.azure.atomicdawn.client.render.WeaponClientExtensions;
import mod.azure.atomicdawn.client.render.WeaponItemRenderer;
import mod.azure.atomicdawn.client.render.ammo.AmmoRenderer;
import mod.azure.atomicdawn.client.render.entity.MiniNukeRenderer;
import mod.azure.atomicdawn.client.render.powerarmor.PowerArmorItemRenderers;
import mod.azure.atomicdawn.client.render.powerarmor.PowerArmorRenderer;
import mod.azure.atomicdawn.client.render.powerarmor.PowerArmorStationRenderer;
import mod.azure.atomicdawn.client.render.workbench.ArmorBenchItemRenderer;
import mod.azure.atomicdawn.client.render.workbench.ArmorBenchRenderer;
import mod.azure.atomicdawn.client.render.workbench.TinkersBenchItemRenderer;
import mod.azure.atomicdawn.client.render.workbench.TinkersBenchRenderer;
import mod.azure.atomicdawn.client.render.workbench.WorkbenchItemRenderer;
import mod.azure.atomicdawn.client.render.workbench.WorkbenchRenderer;
import mod.azure.atomicdawn.client.screen.AmmoContainerScreen;
import mod.azure.atomicdawn.client.screen.ArmorBenchScreen;
import mod.azure.atomicdawn.client.screen.PowerArmorStationScreen;
import mod.azure.atomicdawn.client.screen.TinkersBenchScreen;
import mod.azure.atomicdawn.client.screen.WorkbenchScreen;
import mod.azure.atomicdawn.client.vats.VatsHud;
import mod.azure.atomicdawn.client.weapon.ScopeClient;
import mod.azure.atomicdawn.registry.ADBlockEntities;
import mod.azure.atomicdawn.registry.ADEntities;
import mod.azure.atomicdawn.registry.ADFluids;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.registry.ADMenus;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class AtomicDawnClient {

    private AtomicDawnClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(
            () -> ADItems.weapons().forEach(item -> {
                var id = BuiltInRegistries.ITEM.getKey(item);
                AzItemRendererRegistry.register(item, () -> new WeaponItemRenderer(id));
            })
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(ADItems.WEAPONS_WORKBENCH.get(), WorkbenchItemRenderer::new)
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(ADItems.TINKERS_BENCH.get(), TinkersBenchItemRenderer::new)
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(ADItems.ARMOR_BENCH.get(), ArmorBenchItemRenderer::new)
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(
                ADItems.POWER_ARMOR_STATION.get(),
                PowerArmorItemRenderers.Station::new
            )
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(ADItems.POWER_ARMOR_FIST.get(), PowerArmorItemRenderers.Fist::new)
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(
                ADItems.AMMO_10MM.get(),
                () -> new AmmoRenderer(AtomicDawn.id("ammo_10mm"))
            )
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(
                ADItems.AMMO_308.get(),
                () -> new AmmoRenderer(AtomicDawn.id("ammo_308"))
            )
        );
        event.enqueueWork(
            () -> AzItemRendererRegistry.register(
                ADItems.SHOTGUN_SHELL.get(),
                () -> new AmmoRenderer(AtomicDawn.id("shotgun_shell"))
            )
        );
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ADBlockEntities.WEAPONS_WORKBENCH.get(), _ -> new WorkbenchRenderer());
        event.registerBlockEntityRenderer(ADBlockEntities.TINKERS_BENCH.get(), _ -> new TinkersBenchRenderer());
        event.registerBlockEntityRenderer(ADBlockEntities.ARMOR_BENCH.get(), _ -> new ArmorBenchRenderer());
        event.registerEntityRenderer(ADEntities.MINI_NUKE.get(), MiniNukeRenderer::new);
        event.registerEntityRenderer(ADEntities.FALLOUT_CLOUD.get(), NoopRenderer::new);
        event.registerEntityRenderer(ADEntities.POWER_ARMOR.get(), PowerArmorRenderer::new);
        event.registerEntityRenderer(ADEntities.NPC.get(), NpcRenderer::new);
        event.registerBlockEntityRenderer(
            ADBlockEntities.POWER_ARMOR_STATION.get(),
            _ -> new PowerArmorStationRenderer()
        );
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        GhoulClient.addLayers(event);
        PipBoyArmLayer.addLayers(event);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ADMenus.WEAPONS_WORKBENCH.get(), WorkbenchScreen::new);
        event.register(ADMenus.TINKERS_BENCH.get(), TinkersBenchScreen::new);
        event.register(ADMenus.AMMO_CONTAINER.get(), AmmoContainerScreen::new);
        event.register(ADMenus.POWER_ARMOR_STATION.get(), PowerArmorStationScreen::new);
        event.register(ADMenus.ARMOR_BENCH.get(), ArmorBenchScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ADKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, AtomicDawn.id("weapon_hud"), new WeaponHud());
        event.registerAbove(VanillaGuiLayers.HOTBAR, AtomicDawn.id("vats_hud"), new VatsHud());
        event.registerAbove(VanillaGuiLayers.HOTBAR, AtomicDawn.id("radiation_hud"), new RadiationHud());
        event.registerAbove(VanillaGuiLayers.HOTBAR, AtomicDawn.id("ghoul_hud"), new GhoulHud());
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            AtomicDawn.id("pipboy_notifications"),
            new PipBoyNotificationHud()
        );
        event.registerAbove(VanillaGuiLayers.HOTBAR, AtomicDawn.id("power_armor_hud"), new PowerArmorHud());
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, AtomicDawn.id("waypoint_hud"), new WaypointHud());
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, AtomicDawn.id("party_hud"), new PartyHud());
        event.registerAbove(VanillaGuiLayers.HOTBAR, AtomicDawn.id("camp_workshop_hud"), new CampWorkshopHud());
        event.registerBelowAll(AtomicDawn.id("weapon_scope"), new ScopeClient.Overlay());
        event.registerAboveAll(AtomicDawn.id("nuke_flash"), new NukeEffects.FlashLayer());
    }

    @SubscribeEvent
    public static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        ClientPayloadHandlers.register(event);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(WeaponClientExtensions.INSTANCE, ADItems.weapons().toArray(Item[]::new));
        event.registerFluidType(IrradiatedWaterClient.EXTENSIONS, ADFluids.IRRADIATED_WATER_TYPE.get());
    }

    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        IrradiatedWaterClient.registerModel(event);
    }
}

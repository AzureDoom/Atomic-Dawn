package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.item.CampPlanItem;
import mod.azure.atomicdawn.pipboy.holotape.HolotapeService;

@SuppressWarnings("unused")
public final class ADCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.atomicdawn"))
            .icon(() -> new ItemStack(ADItems.TEN_MM_PISTOL.get()))
            .displayItems((parameters, output) -> {
                ADItems.weapons().forEach(output::accept);
                ADItems.ammo().forEach(output::accept);
                output.accept(ADItems.AMMO_CONTAINER.get());
                output.accept(ADItems.REPAIR_KIT.get());
                output.accept(ADItems.BOBBY_PIN.get());
                ADItems.PADLOCKS.values().forEach(padlock -> output.accept(padlock.get()));
                output.accept(ADItems.LEGENDARY_MODULE.get());
                output.accept(ADItems.RADAWAY.get());
                output.accept(ADItems.FERAL_GHOUL_SPAWN_EGG.get());
                output.accept(ADItems.SCORCHED_SPAWN_EGG.get());
                output.accept(ADItems.SUPER_MUTANT_SPAWN_EGG.get());
                output.accept(ADItems.SCORCHBEAST_SPAWN_EGG.get());
                output.accept(ADItems.MISTER_HANDY_SPAWN_EGG.get());
                output.accept(ADItems.BRAHMIN_SPAWN_EGG.get());
                output.accept(ADItems.RAD_X.get());
                output.accept(ADItems.STIMPAK.get());
                output.accept(ADItems.DOCTORS_BAG.get());
                ADItems.NUKA_COLAS.values().forEach(cola -> output.accept(cola.get()));
                output.accept(ADItems.BRAHMIN_MILK.get());
                output.accept(ADItems.DIRTY_WATER.get());
                output.accept(ADItems.BOILED_WATER.get());
                output.accept(ADItems.GHOULIFICATION_TREATMENT.get());
                output.accept(ADItems.HUMAN_RESTORATION_TREATMENT.get());
                output.accept(ADItems.FERAL_STABILIZER.get());
                output.accept(ADItems.NPC_WAND.get());
                ADItems.SERUMS.values().forEach(serum -> output.accept(serum.get()));
                output.accept(ADItems.IRRADIATED_WATER_BUCKET.get());
                ADItems.JUNK.values().forEach(junk -> output.accept(junk.get()));
                output.accept(ADItems.POWER_ARMOR_STATION.get());
                output.accept(ADItems.WEAPONS_WORKBENCH.get());
                output.accept(ADItems.TINKERS_BENCH.get());
                output.accept(ADItems.ARMOR_BENCH.get());
                output.accept(ADItems.CAMP_MODULE.get());
                output.accept(ADItems.CAMP_STASH.get());
                output.accept(ADItems.SCRAPBOX.get());
                output.accept(ADItems.VAULT_DOOR.get());
                output.accept(ADItems.WASTE_BARREL.get());
                output.accept(ADItems.LEAKING_WASTE_BARREL.get());
                output.accept(ADItems.TOXIC_SLUDGE.get());
                output.accept(ADItems.FISSURE_ROCK.get());
                output.accept(ADItems.FISSURE_SCAR.get());
                output.accept(ADItems.FISSURE_VENT.get());
                output.accept(ADItems.POWER_ARMOR_CHASSIS.get());
                output.accept(ADItems.FUSION_CORE.get());
                parameters.holders()
                    .lookup(ADRegistries.HOLOTAPE)
                    .ifPresent(
                        registry -> registry.listElements()
                            .forEach(holder -> output.accept(HolotapeService.stack(holder.key().identifier())))
                    );
                CampPlans.all(parameters.holders()).forEach(plan -> output.accept(CampPlanItem.stack(plan)));
                ADItems.powerArmorPieces().forEach(output::accept);
                ADItems.armorPieces().forEach(output::accept);
            })
            .build()
    );

    private ADCreativeTabs() {}
}

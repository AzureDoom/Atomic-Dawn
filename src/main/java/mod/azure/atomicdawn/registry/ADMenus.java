package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.crafting.AmmoContainerMenu;
import mod.azure.atomicdawn.crafting.ArmorBenchMenu;
import mod.azure.atomicdawn.crafting.TinkersBenchMenu;
import mod.azure.atomicdawn.crafting.WorkbenchMenu;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationMenu;

public final class ADMenus {

    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(
        Registries.MENU,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<MenuType<?>, MenuType<WorkbenchMenu>> WEAPONS_WORKBENCH = REGISTER.register(
        "weapons_workbench",
        () -> IMenuTypeExtension.create(WorkbenchMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<TinkersBenchMenu>> TINKERS_BENCH = REGISTER.register(
        "tinkers_bench",
        () -> IMenuTypeExtension.create(TinkersBenchMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<AmmoContainerMenu>> AMMO_CONTAINER = REGISTER.register(
        "ammo_container",
        () -> IMenuTypeExtension.create(AmmoContainerMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<PowerArmorStationMenu>> POWER_ARMOR_STATION = REGISTER
        .register("power_armor_station", () -> IMenuTypeExtension.create(PowerArmorStationMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ArmorBenchMenu>> ARMOR_BENCH = REGISTER.register(
        "armor_bench",
        () -> IMenuTypeExtension.create(ArmorBenchMenu::new)
    );

    private ADMenus() {}
}

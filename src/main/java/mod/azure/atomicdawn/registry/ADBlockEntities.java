package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.block.ArmorBenchBlockEntity;
import mod.azure.atomicdawn.block.TinkersBenchBlockEntity;
import mod.azure.atomicdawn.block.WeaponsWorkbenchBlockEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlockEntity;
import mod.azure.atomicdawn.vault.VaultDoorBlockEntity;

public final class ADBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(
        Registries.BLOCK_ENTITY_TYPE,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeaponsWorkbenchBlockEntity>> WEAPONS_WORKBENCH =
        REGISTER.register(
            "weapons_workbench",
            () -> new BlockEntityType<>(WeaponsWorkbenchBlockEntity::new, Set.of(ADBlocks.WEAPONS_WORKBENCH.get()))
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TinkersBenchBlockEntity>> TINKERS_BENCH =
        REGISTER.register(
            "tinkers_bench",
            () -> new BlockEntityType<>(TinkersBenchBlockEntity::new, Set.of(ADBlocks.TINKERS_BENCH.get()))
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerArmorStationBlockEntity>> POWER_ARMOR_STATION =
        REGISTER.register(
            "power_armor_station",
            () -> new BlockEntityType<>(PowerArmorStationBlockEntity::new, Set.of(ADBlocks.POWER_ARMOR_STATION.get()))
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VaultDoorBlockEntity>> VAULT_DOOR =
        REGISTER.register(
            "vault_door",
            () -> new BlockEntityType<>(VaultDoorBlockEntity::new, Set.of(ADBlocks.VAULT_DOOR.get()))
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArmorBenchBlockEntity>> ARMOR_BENCH =
        REGISTER.register(
            "armor_bench",
            () -> new BlockEntityType<>(ArmorBenchBlockEntity::new, Set.of(ADBlocks.ARMOR_BENCH.get()))
        );

    private ADBlockEntities() {}
}

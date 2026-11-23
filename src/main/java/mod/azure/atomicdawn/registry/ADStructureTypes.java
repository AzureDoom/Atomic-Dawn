package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.worldgen.structure.MountainVaultStructure;

public final class ADStructureTypes {

    public static final DeferredRegister<StructureType<?>> REGISTER = DeferredRegister.create(
        Registries.STRUCTURE_TYPE,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<StructureType<?>, StructureType<MountainVaultStructure>> MOUNTAIN_VAULT =
        REGISTER.register("mountain_vault", () -> () -> MountainVaultStructure.CODEC);

    private ADStructureTypes() {}
}

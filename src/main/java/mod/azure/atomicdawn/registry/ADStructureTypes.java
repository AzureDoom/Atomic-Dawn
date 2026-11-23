package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.worldgen.structure.FissurePiece;
import mod.azure.atomicdawn.worldgen.structure.FissureStructure;
import mod.azure.atomicdawn.worldgen.structure.MountainVaultStructure;
import mod.azure.atomicdawn.worldgen.structure.RoadStructure;

public final class ADStructureTypes {

    public static final DeferredRegister<StructureType<?>> REGISTER = DeferredRegister.create(
        Registries.STRUCTURE_TYPE,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<StructureType<?>, StructureType<MountainVaultStructure>> MOUNTAIN_VAULT =
        REGISTER.register("mountain_vault", () -> () -> MountainVaultStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<FissureStructure>> FISSURE = REGISTER.register(
        "fissure",
        () -> () -> FissureStructure.CODEC
    );

    public static final DeferredHolder<StructureType<?>, StructureType<RoadStructure>> ROAD = REGISTER.register(
        "road",
        () -> () -> RoadStructure.CODEC
    );

    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(
        Registries.STRUCTURE_PIECE,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> FISSURE_PIECE = PIECES.register(
        "fissure",
        () -> (_, tag) -> new FissurePiece(tag)
    );

    private ADStructureTypes() {}
}

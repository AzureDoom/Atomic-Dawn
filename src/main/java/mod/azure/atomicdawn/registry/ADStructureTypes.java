package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.worldgen.structure.CanyonPiece;
import mod.azure.atomicdawn.worldgen.structure.CanyonStructure;
import mod.azure.atomicdawn.worldgen.structure.CraterPiece;
import mod.azure.atomicdawn.worldgen.structure.CraterStructure;
import mod.azure.atomicdawn.worldgen.structure.DryLandStructure;
import mod.azure.atomicdawn.worldgen.structure.FissurePiece;
import mod.azure.atomicdawn.worldgen.structure.FissureStructure;
import mod.azure.atomicdawn.worldgen.structure.MountainVaultStructure;
import mod.azure.atomicdawn.worldgen.structure.PittWorksPiece;
import mod.azure.atomicdawn.worldgen.structure.PittWorksStructure;
import mod.azure.atomicdawn.worldgen.structure.RoadStructure;
import mod.azure.atomicdawn.worldgen.structure.ToxicCavePiece;
import mod.azure.atomicdawn.worldgen.structure.ToxicCaveStructure;

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

    public static final DeferredHolder<StructureType<?>, StructureType<CraterStructure>> CRATER = REGISTER.register(
        "crater",
        () -> () -> CraterStructure.CODEC
    );

    public static final DeferredHolder<StructureType<?>, StructureType<CanyonStructure>> CANYON = REGISTER.register(
        "canyon",
        () -> () -> CanyonStructure.CODEC
    );

    public static final DeferredHolder<StructureType<?>, StructureType<DryLandStructure>> DRY_LAND = REGISTER
        .register("dry_land", () -> () -> DryLandStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<PittWorksStructure>> PITT_WORKS = REGISTER
        .register("pitt_works", () -> () -> PittWorksStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<ToxicCaveStructure>> TOXIC_CAVE = REGISTER
        .register("toxic_cave", () -> () -> ToxicCaveStructure.CODEC);

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

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRATER_PIECE = PIECES.register(
        "crater",
        () -> (_, tag) -> new CraterPiece(tag)
    );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CANYON_PIECE = PIECES.register(
        "canyon",
        () -> (_, tag) -> new CanyonPiece(tag)
    );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> PITT_WORKS_PIECE = PIECES.register(
        "pitt_works",
        () -> (_, tag) -> new PittWorksPiece(tag)
    );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TOXIC_CAVE_PIECE = PIECES.register(
        "toxic_cave",
        () -> (_, tag) -> new ToxicCavePiece(tag)
    );

    private ADStructureTypes() {}
}

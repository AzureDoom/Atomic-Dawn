package mod.azure.atomicdawn.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import mod.azure.atomicdawn.registry.ADStructureTypes;

public class DryLandStructure extends Structure {

    public static final MapCodec<DryLandStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            Structure.CODEC.fieldOf("delegate").forGetter(structure -> structure.delegate),
            Codec.intRange(0, 64).optionalFieldOf("check_radius", 12).forGetter(structure -> structure.checkRadius)
        ).apply(instance, DryLandStructure::new)
    );

    private final Holder<Structure> delegate;

    private final int checkRadius;

    public DryLandStructure(StructureSettings settings, Holder<Structure> delegate, int checkRadius) {
        super(settings);
        this.delegate = delegate;
        this.checkRadius = checkRadius;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var x = chunk.getMiddleBlockX();
        var z = chunk.getMiddleBlockZ();
        for (var i = 0; i < 5; i++) {
            var sx = x + (i == 1 ? checkRadius : i == 2 ? -checkRadius : 0);
            var sz = z + (i == 3 ? checkRadius : i == 4 ? -checkRadius : 0);
            if (wet(context, sx, sz)) {
                return Optional.empty();
            }
        }
        return delegate.value().findValidGenerationPoint(context);
    }

    private static boolean wet(GenerationContext context, int x, int z) {
        var generator = context.chunkGenerator();
        var surface = generator.getFirstFreeHeight(
            x,
            z,
            Heightmap.Types.WORLD_SURFACE_WG,
            context.heightAccessor(),
            context.randomState()
        );
        var floor = generator.getFirstFreeHeight(
            x,
            z,
            Heightmap.Types.OCEAN_FLOOR_WG,
            context.heightAccessor(),
            context.randomState()
        );
        return surface != floor;
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ADStructureTypes.DRY_LAND.get();
    }
}

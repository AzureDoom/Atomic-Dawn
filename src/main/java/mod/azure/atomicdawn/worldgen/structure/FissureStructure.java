package mod.azure.atomicdawn.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import mod.azure.atomicdawn.helper.ADWorldgenUtil;
import mod.azure.atomicdawn.registry.ADStructureTypes;

public class FissureStructure extends Structure {

    public static final MapCodec<FissureStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            IntProviders.codec(8, 96).fieldOf("length").forGetter(structure -> structure.length),
            IntProviders.codec(2, 16).fieldOf("half_width").forGetter(structure -> structure.halfWidth),
            IntProviders.codec(4, 64).fieldOf("depth").forGetter(structure -> structure.depth),
            IntProviders.codec(0, 32).fieldOf("cracks").forGetter(structure -> structure.cracks)
        ).apply(instance, FissureStructure::new)
    );

    private final IntProvider length;

    private final IntProvider halfWidth;

    private final IntProvider depth;

    private final IntProvider cracks;

    public FissureStructure(
        StructureSettings settings,
        IntProvider length,
        IntProvider halfWidth,
        IntProvider depth,
        IntProvider cracks
    ) {
        super(settings);
        this.length = length;
        this.halfWidth = halfWidth;
        this.depth = depth;
        this.cracks = cracks;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var x = chunk.getMiddleBlockX();
        var z = chunk.getMiddleBlockZ();
        var y = ADWorldgenUtil.firstFreeHeight(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
        if (y <= context.chunkGenerator().getSeaLevel() + 1) {
            return Optional.empty();
        }
        var random = context.random();
        var piece = new FissurePiece(
            new BlockPos(x, y - 1, z),
            random.nextFloat() * (float) Math.PI,
            length.sample(random),
            halfWidth.sample(random),
            depth.sample(random),
            cracks.sample(random),
            random.nextLong(),
            context.heightAccessor().getMinY()
        );
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> builder.addPiece(piece)));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ADStructureTypes.FISSURE.get();
    }
}

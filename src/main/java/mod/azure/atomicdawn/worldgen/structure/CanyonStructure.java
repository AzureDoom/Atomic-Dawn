package mod.azure.atomicdawn.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import mod.azure.atomicdawn.helper.ADWorldgenUtil;
import mod.azure.atomicdawn.registry.ADStructureTypes;

public class CanyonStructure extends Structure {

    public static final MapCodec<CanyonStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            IntProviders.codec(32, 192).fieldOf("length").forGetter(structure -> structure.length),
            IntProviders.codec(4, 32).fieldOf("half_width").forGetter(structure -> structure.halfWidth),
            IntProviders.codec(8, 48).fieldOf("depth").forGetter(structure -> structure.depth),
            Codec.BOOL.optionalFieldOf("stream", true).forGetter(structure -> structure.flowing)
        ).apply(instance, CanyonStructure::new)
    );

    private final IntProvider length;

    private final IntProvider halfWidth;

    private final IntProvider depth;

    private final boolean flowing;

    public CanyonStructure(
        StructureSettings settings,
        IntProvider length,
        IntProvider halfWidth,
        IntProvider depth,
        boolean flowing
    ) {
        super(settings);
        this.length = length;
        this.halfWidth = halfWidth;
        this.depth = depth;
        this.flowing = flowing;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var x = chunk.getMiddleBlockX();
        var z = chunk.getMiddleBlockZ();
        var seaLevel = context.chunkGenerator().getSeaLevel();
        var y = height(context, x, z);
        if (y <= seaLevel + 4) {
            return Optional.empty();
        }
        var random = context.random();
        var angle = random.nextFloat() * (float) Math.PI;
        var size = length.sample(random);
        var width = halfWidth.sample(random);
        var cut = depth.sample(random);
        var reach = size / 2F;
        var ax = Math.round(Mth.cos(angle) * reach);
        var az = Math.round(Mth.sin(angle) * reach);
        var first = height(context, x + ax, z + az);
        var second = height(context, x - ax, z - az);
        var lowest = Math.min(y, Math.min(first, second));
        var highest = Math.max(y, Math.max(first, second));
        if (highest - lowest > cut || lowest <= seaLevel + 2) {
            return Optional.empty();
        }
        var floor = lowest - cut;
        if (floor <= context.heightAccessor().getMinY() + 8) {
            return Optional.empty();
        }
        var piece = new CanyonPiece(
            new BlockPos(x, lowest - 1, z),
            angle,
            size,
            width,
            cut,
            flowing,
            random.nextLong(),
            context.heightAccessor().getMinY()
        );
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> builder.addPiece(piece)));
    }

    private static int height(GenerationContext context, int x, int z) {
        return ADWorldgenUtil.firstFreeHeight(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ADStructureTypes.CANYON.get();
    }
}

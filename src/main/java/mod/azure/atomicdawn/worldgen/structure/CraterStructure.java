package mod.azure.atomicdawn.worldgen.structure;

import com.mojang.serialization.Codec;
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

public class CraterStructure extends Structure {

    public static final MapCodec<CraterStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            IntProviders.codec(6, 48).fieldOf("radius").forGetter(structure -> structure.radius),
            IntProviders.codec(3, 32).fieldOf("depth").forGetter(structure -> structure.depth),
            IntProviders.codec(0, 12).fieldOf("rim_height").forGetter(structure -> structure.rimHeight),
            Codec.floatRange(0F, 1F).fieldOf("flood_chance").forGetter(structure -> structure.floodChance)
        ).apply(instance, CraterStructure::new)
    );

    private final IntProvider radius;

    private final IntProvider depth;

    private final IntProvider rimHeight;

    private final float floodChance;

    public CraterStructure(
        StructureSettings settings,
        IntProvider radius,
        IntProvider depth,
        IntProvider rimHeight,
        float floodChance
    ) {
        super(settings);
        this.radius = radius;
        this.depth = depth;
        this.rimHeight = rimHeight;
        this.floodChance = floodChance;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var x = chunk.getMiddleBlockX();
        var z = chunk.getMiddleBlockZ();
        var y = height(context, x, z);
        if (y <= context.chunkGenerator().getSeaLevel() + 1) {
            return Optional.empty();
        }
        var random = context.random();
        var size = radius.sample(random);
        var bowl = depth.sample(random);
        var rim = rimHeight.sample(random);
        var probe = Math.max(4, Math.round(size * 0.75F));
        var lowest = y;
        var highest = y;
        for (var i = 0; i < 4; i++) {
            var sx = x + (i == 0 ? probe : i == 1 ? -probe : 0);
            var sz = z + (i == 2 ? probe : i == 3 ? -probe : 0);
            var sample = height(context, sx, sz);
            lowest = Math.min(lowest, sample);
            highest = Math.max(highest, sample);
        }
        if (highest - lowest > bowl + rim * 2 || lowest <= context.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }
        var piece = new CraterPiece(
            new BlockPos(x, y - 1, z),
            size,
            bowl,
            rim,
            random.nextFloat() < floodChance,
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
        return ADStructureTypes.CRATER.get();
    }
}

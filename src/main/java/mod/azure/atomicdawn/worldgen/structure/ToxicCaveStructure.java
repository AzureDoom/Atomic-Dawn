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

public class ToxicCaveStructure extends Structure {

    public static final MapCodec<ToxicCaveStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            IntProviders.codec(6, 32).fieldOf("radius").forGetter(structure -> structure.radius),
            IntProviders.codec(4, 16).fieldOf("height").forGetter(structure -> structure.height),
            Codec.INT.optionalFieldOf("min_y", -24).forGetter(structure -> structure.minY),
            Codec.INT.optionalFieldOf("max_y", 32).forGetter(structure -> structure.maxY)
        ).apply(instance, ToxicCaveStructure::new)
    );

    private final IntProvider radius;

    private final IntProvider height;

    private final int minY;

    private final int maxY;

    public ToxicCaveStructure(StructureSettings settings, IntProvider radius, IntProvider height, int minY, int maxY) {
        super(settings);
        this.radius = radius;
        this.height = height;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var x = chunk.getMiddleBlockX();
        var z = chunk.getMiddleBlockZ();
        var surface = ADWorldgenUtil.firstFreeHeight(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
        var random = context.random();
        var rx = radius.sample(random);
        var rz = radius.sample(random);
        var ry = height.sample(random);
        var floor = Math.max(minY, context.heightAccessor().getMinY() + ry + 8);
        var ceiling = Math.min(maxY, surface - ry - 8);
        if (ceiling <= floor) {
            return Optional.empty();
        }
        var y = floor + random.nextInt(ceiling - floor + 1);
        var piece = new ToxicCavePiece(new BlockPos(x, y, z), rx, ry, rz, random.nextLong());
        return Optional.of(new GenerationStub(new BlockPos(x, surface, z), builder -> builder.addPiece(piece)));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ADStructureTypes.TOXIC_CAVE.get();
    }
}

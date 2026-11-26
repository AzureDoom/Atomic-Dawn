package mod.azure.atomicdawn.worldgen.structure;

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

public class PittWorksStructure extends Structure {

    public static final MapCodec<PittWorksStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            IntProviders.codec(4, 24).fieldOf("heap_radius").forGetter(structure -> structure.heapRadius),
            IntProviders.codec(3, 20).fieldOf("heap_height").forGetter(structure -> structure.heapHeight),
            IntProviders.codec(0, 4).fieldOf("stacks").forGetter(structure -> structure.stacks),
            IntProviders.codec(8, 48).fieldOf("stack_height").forGetter(structure -> structure.stackHeight)
        ).apply(instance, PittWorksStructure::new)
    );

    private final IntProvider heapRadius;

    private final IntProvider heapHeight;

    private final IntProvider stacks;

    private final IntProvider stackHeight;

    public PittWorksStructure(
        StructureSettings settings,
        IntProvider heapRadius,
        IntProvider heapHeight,
        IntProvider stacks,
        IntProvider stackHeight
    ) {
        super(settings);
        this.heapRadius = heapRadius;
        this.heapHeight = heapHeight;
        this.stacks = stacks;
        this.stackHeight = stackHeight;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        var chunk = context.chunkPos();
        var x = chunk.getMiddleBlockX();
        var z = chunk.getMiddleBlockZ();
        var seaLevel = context.chunkGenerator().getSeaLevel();
        var y = height(context, x, z);
        if (y <= seaLevel + 1) {
            return Optional.empty();
        }
        var random = context.random();
        var radius = heapRadius.sample(random);
        var count = stacks.sample(random);
        var data = new int[count * 5];
        var placed = 0;
        for (var i = 0; i < count; i++) {
            var angle = random.nextFloat() * Mth.PI * 2F;
            var distance = radius + 4 + random.nextInt(8);
            var sx = x + Math.round(Mth.cos(angle) * distance);
            var sz = z + Math.round(Mth.sin(angle) * distance);
            var sy = height(context, sx, sz) - 1;
            if (sy <= seaLevel) {
                continue;
            }
            data[placed * 5] = sx;
            data[placed * 5 + 1] = sy;
            data[placed * 5 + 2] = sz;
            data[placed * 5 + 3] = 2 + random.nextInt(2);
            data[placed * 5 + 4] = stackHeight.sample(random);
            placed++;
        }
        var trimmed = new int[placed * 5];
        System.arraycopy(data, 0, trimmed, 0, trimmed.length);
        var piece = new PittWorksPiece(
            new BlockPos(x, y - 1, z),
            radius,
            heapHeight.sample(random),
            trimmed,
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
        return ADStructureTypes.PITT_WORKS.get();
    }
}

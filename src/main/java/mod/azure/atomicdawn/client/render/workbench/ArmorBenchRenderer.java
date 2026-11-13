package mod.azure.atomicdawn.client.render.workbench;

import net.minecraft.resources.Identifier;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.block.ArmorBenchBlockEntity;

public class ArmorBenchRenderer extends BenchRenderer<ArmorBenchBlockEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/block/armor_bench.geo.json");

    public static final Identifier TEXTURE = AtomicDawn.id("textures/block/armor_bench.png");

    public ArmorBenchRenderer() {
        super(MODEL, TEXTURE);
    }
}

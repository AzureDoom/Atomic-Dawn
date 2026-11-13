package mod.azure.atomicdawn.client.render.workbench;

import net.minecraft.resources.Identifier;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.block.TinkersBenchBlockEntity;

public class TinkersBenchRenderer extends BenchRenderer<TinkersBenchBlockEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/block/tinkers_bench.geo.json");

    public static final Identifier TEXTURE = AtomicDawn.id("textures/block/tinkers_bench.png");

    public TinkersBenchRenderer() {
        super(MODEL, TEXTURE);
    }
}

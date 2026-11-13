package mod.azure.atomicdawn.client.render.workbench;

import net.minecraft.resources.Identifier;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.block.WeaponsWorkbenchBlockEntity;

public class WorkbenchRenderer extends BenchRenderer<WeaponsWorkbenchBlockEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/block/weapons_workbench.geo.json");

    public static final Identifier TEXTURE = AtomicDawn.id("textures/block/weapons_workbench.png");

    public WorkbenchRenderer() {
        super(MODEL, TEXTURE);
    }
}

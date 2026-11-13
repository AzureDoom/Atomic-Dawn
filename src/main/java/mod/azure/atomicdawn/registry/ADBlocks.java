package mod.azure.atomicdawn.registry;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.block.ArmorBenchBlock;
import mod.azure.atomicdawn.block.TinkersBenchBlock;
import mod.azure.atomicdawn.block.WeaponsWorkbenchBlock;
import mod.azure.atomicdawn.pipboy.light.PipBoyLightBlock;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlock;

public final class ADBlocks {

    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(AtomicDawn.MOD_ID);

    public static final DeferredBlock<WeaponsWorkbenchBlock> WEAPONS_WORKBENCH = REGISTER.registerBlock(
        "weapons_workbench",
        WeaponsWorkbenchBlock::new,
        properties -> properties.mapColor(MapColor.METAL)
            .strength(3.5F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .isViewBlocking((_, _, _, _) -> false)
            .isSuffocating((_, _, _) -> false)
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<TinkersBenchBlock> TINKERS_BENCH = REGISTER.registerBlock(
        "tinkers_bench",
        TinkersBenchBlock::new,
        properties -> properties.mapColor(MapColor.METAL)
            .strength(3.5F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .isViewBlocking((_, _, _, _) -> false)
            .isSuffocating((_, _, _) -> false)
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<ArmorBenchBlock> ARMOR_BENCH = REGISTER.registerBlock(
        "armor_bench",
        ArmorBenchBlock::new,
        properties -> properties.mapColor(MapColor.METAL)
            .strength(3.5F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .isViewBlocking((_, _, _, _) -> false)
            .isSuffocating((_, _, _) -> false)
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<PowerArmorStationBlock> POWER_ARMOR_STATION = REGISTER.registerBlock(
        "power_armor_station",
        PowerArmorStationBlock::new,
        properties -> properties.mapColor(MapColor.COLOR_YELLOW)
            .strength(4.0F, 8.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .isViewBlocking((_, _, _, _) -> false)
            .isSuffocating((_, _, _) -> false)
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<LiquidBlock> IRRADIATED_WATER = REGISTER.registerBlock(
        "irradiated_water",
        properties -> new LiquidBlock(ADFluids.IRRADIATED_WATER.get(), properties),
        () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(_ -> 2)
    );

    public static final DeferredBlock<PipBoyLightBlock> PIPBOY_LIGHT = REGISTER.registerBlock(
        "pipboy_light",
        PipBoyLightBlock::new,
        properties -> properties.mapColor(MapColor.NONE)
            .noCollision()
            .noOcclusion()
            .replaceable()
            .instabreak()
            .noLootTable()
            .pushReaction(PushReaction.POPPED)
            .lightLevel(state -> state.getValue(PipBoyLightBlock.LEVEL))
    );

    private ADBlocks() {}
}

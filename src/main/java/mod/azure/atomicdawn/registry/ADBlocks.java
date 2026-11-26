package mod.azure.atomicdawn.registry;

import net.minecraft.world.level.block.Block;
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
import mod.azure.atomicdawn.block.FissureVentBlock;
import mod.azure.atomicdawn.block.TinkersBenchBlock;
import mod.azure.atomicdawn.block.ToxicSludgeBlock;
import mod.azure.atomicdawn.block.WasteBarrelBlock;
import mod.azure.atomicdawn.block.WeaponsWorkbenchBlock;
import mod.azure.atomicdawn.camp.CampModuleBlock;
import mod.azure.atomicdawn.camp.CampStashBlock;
import mod.azure.atomicdawn.junk.ScrapboxBlock;
import mod.azure.atomicdawn.pipboy.light.PipBoyLightBlock;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlock;
import mod.azure.atomicdawn.vault.VaultDoorBlock;

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
            .isViewBlocking((ignored, ignored2, ignored3, ignored4) -> false)
            .isSuffocating((ignored, ignored2, ignored3) -> false)
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
            .isViewBlocking((ignored, ignored2, ignored3, ignored4) -> false)
            .isSuffocating((ignored, ignored2, ignored3) -> false)
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
            .isViewBlocking((ignored, ignored2, ignored3, ignored4) -> false)
            .isSuffocating((ignored, ignored2, ignored3) -> false)
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
            .isViewBlocking((ignored, ignored2, ignored3, ignored4) -> false)
            .isSuffocating((ignored, ignored2, ignored3) -> false)
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<VaultDoorBlock> VAULT_DOOR = REGISTER.registerBlock(
        "vault_door",
        VaultDoorBlock::new,
        properties -> properties.mapColor(MapColor.METAL)
            .strength(-1.0F, 3600000.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .noLootTable()
            .isViewBlocking((ignored, ignored2, ignored3, ignored4) -> false)
            .isSuffocating((ignored, ignored2, ignored3) -> false)
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<LiquidBlock> IRRADIATED_WATER = REGISTER.registerBlock(
        "irradiated_water",
        properties -> new LiquidBlock(ADFluids.IRRADIATED_WATER.get(), properties),
        () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(ignored -> 2)
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

    public static final DeferredBlock<CampModuleBlock> CAMP_MODULE = REGISTER.registerBlock(
        "camp_module",
        CampModuleBlock::new,
        properties -> properties.mapColor(MapColor.METAL)
            .strength(3.5F, 3600000.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .lightLevel(ignored -> 7)
            .noOcclusion()
            .isViewBlocking((ignored, ignored2, ignored3, ignored4) -> false)
            .isSuffocating((ignored, ignored2, ignored3) -> false)
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<CampStashBlock> CAMP_STASH = REGISTER.registerBlock(
        "camp_stash",
        CampStashBlock::new,
        properties -> properties.mapColor(MapColor.WOOD)
            .strength(2.5F, 6.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    public static final DeferredBlock<WasteBarrelBlock> WASTE_BARREL = REGISTER.registerBlock(
        "waste_barrel",
        properties -> new WasteBarrelBlock(properties, false),
        properties -> properties.mapColor(MapColor.COLOR_YELLOW)
            .strength(2.0F, 4.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .lightLevel(ignored -> 3)
    );

    public static final DeferredBlock<WasteBarrelBlock> LEAKING_WASTE_BARREL = REGISTER.registerBlock(
        "leaking_waste_barrel",
        properties -> new WasteBarrelBlock(properties, true),
        properties -> properties.mapColor(MapColor.COLOR_YELLOW)
            .strength(2.0F, 4.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .lightLevel(ignored -> 7)
    );

    public static final DeferredBlock<ToxicSludgeBlock> TOXIC_SLUDGE = REGISTER.registerBlock(
        "toxic_sludge",
        ToxicSludgeBlock::new,
        properties -> properties.mapColor(MapColor.COLOR_LIGHT_GREEN)
            .strength(0.3F)
            .sound(SoundType.SLIME_BLOCK)
            .noOcclusion()
            .speedFactor(0.6F)
            .lightLevel(ignored -> 9)
            .emissiveRendering((ignored) -> true)
            .pushReaction(PushReaction.POPPED)
    );

    public static final DeferredBlock<Block> FISSURE_ROCK = REGISTER.registerBlock(
        "fissure_rock",
        Block::new,
        properties -> properties.mapColor(MapColor.COLOR_BLACK)
            .strength(2.5F, 6.0F)
            .sound(SoundType.BASALT)
            .requiresCorrectToolForDrops()
    );

    public static final DeferredBlock<Block> FISSURE_SCAR = REGISTER.registerBlock(
        "fissure_scar",
        Block::new,
        properties -> properties.mapColor(MapColor.COLOR_LIGHT_GREEN)
            .strength(2.5F, 6.0F)
            .sound(SoundType.BASALT)
            .requiresCorrectToolForDrops()
            .lightLevel(ignored -> 13)
            .emissiveRendering((ignored) -> true)
    );

    public static final DeferredBlock<FissureVentBlock> FISSURE_VENT = REGISTER.registerBlock(
        "fissure_vent",
        FissureVentBlock::new,
        properties -> properties.mapColor(MapColor.COLOR_LIGHT_GREEN)
            .strength(3.0F, 6.0F)
            .sound(SoundType.BASALT)
            .requiresCorrectToolForDrops()
            .lightLevel(ignored -> 15)
            .emissiveRendering((ignored) -> true)
    );

    public static final DeferredBlock<ScrapboxBlock> SCRAPBOX = REGISTER.registerBlock(
        "scrapbox",
        ScrapboxBlock::new,
        properties -> properties.mapColor(MapColor.METAL)
            .strength(3.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .pushReaction(PushReaction.IMMOVEABLE)
    );

    private ADBlocks() {}
}

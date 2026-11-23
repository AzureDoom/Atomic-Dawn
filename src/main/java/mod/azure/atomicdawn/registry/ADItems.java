package mod.azure.atomicdawn.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ammunition.AmmoContainerItem;
import mod.azure.atomicdawn.ammunition.AmmoItem;
import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.armor.ArmorPieceItem;
import mod.azure.atomicdawn.armor.ArmorSet;
import mod.azure.atomicdawn.ghoul.GhoulTreatmentService;
import mod.azure.atomicdawn.item.CampPlanItem;
import mod.azure.atomicdawn.item.FeralStabilizerItem;
import mod.azure.atomicdawn.item.GhoulTreatmentItem;
import mod.azure.atomicdawn.item.HolotapeItem;
import mod.azure.atomicdawn.item.MutationSerumItem;
import mod.azure.atomicdawn.item.NukaCola;
import mod.azure.atomicdawn.item.NukaColaItem;
import mod.azure.atomicdawn.item.PadlockItem;
import mod.azure.atomicdawn.item.RadAwayItem;
import mod.azure.atomicdawn.item.RadRemoverItem;
import mod.azure.atomicdawn.item.RadXItem;
import mod.azure.atomicdawn.item.WaterItem;
import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.lock.LockLevel;
import mod.azure.atomicdawn.mutation.Mutation;
import mod.azure.atomicdawn.npc.NpcWandItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorChassisItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;
import mod.azure.atomicdawn.powerarmor.PowerCoreItem;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinitions;

@SuppressWarnings("unused")
public final class ADItems {

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(AtomicDawn.MOD_ID);

    private static final List<DeferredItem<WeaponItem>> WEAPONS = new ArrayList<>();

    private static final List<DeferredItem<AmmoItem>> AMMO = new ArrayList<>();

    public static final DeferredItem<WeaponItem> TEN_MM_PISTOL = weapon("10mm_pistol", WeaponDefinitions.TEN_MM_PISTOL);

    public static final DeferredItem<WeaponItem> HUNTING_RIFLE = weapon(
        "hunting_rifle",
        WeaponDefinitions.HUNTING_RIFLE
    );

    public static final DeferredItem<WeaponItem> COMBAT_SHOTGUN = weapon(
        "combat_shotgun",
        WeaponDefinitions.COMBAT_SHOTGUN
    );

    public static final DeferredItem<WeaponItem> LASER_RIFLE = weapon("laser_rifle", WeaponDefinitions.LASER_RIFLE);

    public static final DeferredItem<WeaponItem> SUPER_SLEDGE = weapon("super_sledge", WeaponDefinitions.SUPER_SLEDGE);

    public static final DeferredItem<WeaponItem> FAT_MAN = weapon("fat_man", WeaponDefinitions.FAT_MAN);

    public static final DeferredItem<AmmoContainerItem> AMMO_CONTAINER = REGISTER.registerItem(
        "ammo_container",
        AmmoContainerItem::new,
        properties -> properties.stacksTo(1)
    );

    public static final DeferredItem<AmmoItem> AMMO_10MM = ammo("ammo_10mm", 24);

    public static final DeferredItem<AmmoItem> AMMO_308 = ammo("ammo_308", 12);

    public static final DeferredItem<AmmoItem> SHOTGUN_SHELL = ammo("shotgun_shell", 12);

    public static final DeferredItem<AmmoItem> FUSION_CELL = ammo("fusion_cell");

    public static final DeferredItem<AmmoItem> MINI_NUKE = REGISTER.registerItem(
        "mini_nuke",
        AmmoItem::new,
        properties -> properties.stacksTo(8).rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<RadAwayItem> RADAWAY = REGISTER.registerItem(
        "radaway",
        RadAwayItem::new,
        RadAwayItem::properties
    );

    public static final DeferredItem<RadXItem> RAD_X = REGISTER.registerItem(
        "rad_x",
        RadXItem::new,
        RadXItem::properties
    );

    public static final DeferredItem<GhoulTreatmentItem> GHOULIFICATION_TREATMENT = REGISTER.registerItem(
        "ghoulification_treatment",
        properties -> new GhoulTreatmentItem(properties, GhoulTreatmentService.Kind.GHOULIFY),
        GhoulTreatmentItem::properties
    );

    public static final DeferredItem<GhoulTreatmentItem> HUMAN_RESTORATION_TREATMENT = REGISTER.registerItem(
        "human_restoration_treatment",
        properties -> new GhoulTreatmentItem(properties, GhoulTreatmentService.Kind.RESTORE),
        GhoulTreatmentItem::properties
    );

    public static final DeferredItem<NpcWandItem> NPC_WAND = REGISTER.registerItem(
        "npc_wand",
        NpcWandItem::new,
        NpcWandItem::properties
    );

    public static final DeferredItem<FeralStabilizerItem> FERAL_STABILIZER = REGISTER.registerItem(
        "feral_stabilizer",
        FeralStabilizerItem::new,
        FeralStabilizerItem::properties
    );

    public static final DeferredItem<BucketItem> IRRADIATED_WATER_BUCKET = REGISTER.registerItem(
        "irradiated_water_bucket",
        properties -> new BucketItem(ADFluids.IRRADIATED_WATER.get(), properties),
        properties -> properties.craftRemainder(Items.BUCKET).stacksTo(1)
    );

    public static final DeferredItem<WaterItem> DIRTY_WATER = REGISTER.registerItem(
        "dirty_water",
        properties -> new WaterItem(
            properties,
            () -> AtomicDawn.CONFIG.thirst.DIRTY_WATER_THIRST,
            () -> AtomicDawn.CONFIG.radiation.DIRTY_WATER_DRINK_RADS
        ),
        WaterItem::properties
    );

    public static final DeferredItem<WaterItem> BOILED_WATER = REGISTER.registerItem(
        "boiled_water",
        properties -> new WaterItem(
            properties,
            () -> AtomicDawn.CONFIG.thirst.BOILED_WATER_THIRST,
            () -> AtomicDawn.CONFIG.radiation.BOILED_WATER_DRINK_RADS
        ),
        WaterItem::properties
    );

    public static final DeferredItem<Item> REPAIR_KIT = REGISTER.registerSimpleItem(
        "weapon_repair_kit",
        properties -> properties.stacksTo(16)
    );

    public static final DeferredItem<HolotapeItem> HOLOTAPE = REGISTER.registerItem(
        "holotape",
        HolotapeItem::new,
        HolotapeItem::properties
    );

    public static final DeferredItem<Item> BOBBY_PIN = REGISTER.registerSimpleItem(
        "bobby_pin",
        properties -> properties.stacksTo(64)
    );

    public static final Map<LockLevel, DeferredItem<PadlockItem>> PADLOCKS = padlocks();

    public static final DeferredItem<Item> LEGENDARY_MODULE = REGISTER.registerSimpleItem(
        "legendary_module",
        properties -> properties.stacksTo(64).rarity(Rarity.RARE)
    );

    public static final DeferredItem<BlockItem> WEAPONS_WORKBENCH = REGISTER.registerSimpleBlockItem(
        "weapons_workbench",
        ADBlocks.WEAPONS_WORKBENCH
    );

    public static final DeferredItem<BlockItem> TINKERS_BENCH = REGISTER.registerSimpleBlockItem(
        "tinkers_bench",
        ADBlocks.TINKERS_BENCH
    );

    public static final DeferredItem<BlockItem> ARMOR_BENCH = REGISTER.registerSimpleBlockItem(
        "armor_bench",
        ADBlocks.ARMOR_BENCH
    );

    public static final DeferredItem<BlockItem> CAMP_MODULE = REGISTER.registerSimpleBlockItem(
        "camp_module",
        ADBlocks.CAMP_MODULE
    );

    public static final DeferredItem<BlockItem> CAMP_STASH = REGISTER.registerSimpleBlockItem(
        "camp_stash",
        ADBlocks.CAMP_STASH
    );

    public static final DeferredItem<BlockItem> WASTE_BARREL = REGISTER.registerSimpleBlockItem(
        "waste_barrel",
        ADBlocks.WASTE_BARREL
    );

    public static final DeferredItem<BlockItem> LEAKING_WASTE_BARREL = REGISTER.registerSimpleBlockItem(
        "leaking_waste_barrel",
        ADBlocks.LEAKING_WASTE_BARREL
    );

    public static final DeferredItem<BlockItem> TOXIC_SLUDGE = REGISTER.registerSimpleBlockItem(
        "toxic_sludge",
        ADBlocks.TOXIC_SLUDGE
    );

    public static final DeferredItem<BlockItem> FISSURE_ROCK = REGISTER.registerSimpleBlockItem(
        "fissure_rock",
        ADBlocks.FISSURE_ROCK
    );

    public static final DeferredItem<BlockItem> FISSURE_SCAR = REGISTER.registerSimpleBlockItem(
        "fissure_scar",
        ADBlocks.FISSURE_SCAR
    );

    public static final DeferredItem<BlockItem> FISSURE_VENT = REGISTER.registerSimpleBlockItem(
        "fissure_vent",
        ADBlocks.FISSURE_VENT
    );

    public static final DeferredItem<CampPlanItem> CAMP_PLAN = REGISTER.registerItem(
        "camp_plan",
        CampPlanItem::new,
        CampPlanItem::properties
    );

    public static final Map<ArmorSet, Map<ArmorPiece, DeferredItem<ArmorPieceItem>>> ARMOR = armor();

    public static final DeferredItem<BlockItem> POWER_ARMOR_STATION = REGISTER.registerSimpleBlockItem(
        "power_armor_station",
        ADBlocks.POWER_ARMOR_STATION
    );

    public static final DeferredItem<BlockItem> VAULT_DOOR = REGISTER.registerSimpleBlockItem(
        "vault_door",
        ADBlocks.VAULT_DOOR
    );

    public static final DeferredItem<PowerArmorChassisItem> POWER_ARMOR_CHASSIS = REGISTER.registerItem(
        "power_armor_chassis",
        PowerArmorChassisItem::new,
        PowerArmorChassisItem::properties
    );

    public static final DeferredItem<PowerCoreItem> FUSION_CORE = REGISTER.registerItem(
        "fusion_core",
        PowerCoreItem::new,
        PowerCoreItem::properties
    );

    public static final DeferredItem<RadRemoverItem> NUKA_GRAPE = REGISTER.registerItem(
        "nuka_grape",
        properties -> new RadRemoverItem(properties, () -> AtomicDawn.CONFIG.radiation.NUKA_GRAPE_RADS),
        RadRemoverItem::properties
    );

    public static final Map<NukaCola, DeferredItem<NukaColaItem>> NUKA_COLAS = nukaColas();

    public static final DeferredItem<RadRemoverItem> BRAHMIN_MILK = REGISTER.registerItem(
        "brahmin_milk",
        properties -> new RadRemoverItem(properties, () -> AtomicDawn.CONFIG.radiation.BRAHMIN_MILK_RADS),
        RadRemoverItem::properties
    );

    public static final Map<Mutation, DeferredItem<MutationSerumItem>> SERUMS = serums();

    public static final DeferredItem<Item> POWER_ARMOR_FIST = REGISTER.registerSimpleItem(
        "power_armor_fist",
        properties -> properties.stacksTo(1)
    );

    public static final DeferredItem<PowerArmorPieceItem> T45_HELMET = piece("t45_helmet", PowerArmorSlot.HELMET, 400);

    public static final DeferredItem<PowerArmorPieceItem> T45_TORSO = piece("t45_torso", PowerArmorSlot.TORSO, 600);

    public static final DeferredItem<PowerArmorPieceItem> T45_LEFT_ARM = piece(
        "t45_left_arm",
        PowerArmorSlot.LEFT_ARM,
        350
    );

    public static final DeferredItem<PowerArmorPieceItem> T45_RIGHT_ARM = piece(
        "t45_right_arm",
        PowerArmorSlot.RIGHT_ARM,
        350
    );

    public static final DeferredItem<PowerArmorPieceItem> T45_LEFT_LEG = piece(
        "t45_left_leg",
        PowerArmorSlot.LEFT_LEG,
        350
    );

    public static final DeferredItem<PowerArmorPieceItem> T45_RIGHT_LEG = piece(
        "t45_right_leg",
        PowerArmorSlot.RIGHT_LEG,
        350
    );

    private ADItems() {}

    public static Stream<Item> weapons() {
        return WEAPONS.stream().map(DeferredHolder::get);
    }

    private static Map<NukaCola, DeferredItem<NukaColaItem>> nukaColas() {
        var colas = new EnumMap<NukaCola, DeferredItem<NukaColaItem>>(NukaCola.class);
        for (var variant : NukaCola.values()) {
            colas.put(
                variant,
                REGISTER.registerItem(
                    variant.id(),
                    properties -> new NukaColaItem(properties, variant),
                    NukaColaItem::properties
                )
            );
        }
        return Collections.unmodifiableMap(colas);
    }

    private static Map<Mutation, DeferredItem<MutationSerumItem>> serums() {
        var serums = new EnumMap<Mutation, DeferredItem<MutationSerumItem>>(Mutation.class);
        for (var mutation : Mutation.values()) {
            serums.put(
                mutation,
                REGISTER.registerItem(
                    mutation.getSerializedName() + "_serum",
                    properties -> new MutationSerumItem(properties, mutation),
                    MutationSerumItem::properties
                )
            );
        }
        return Collections.unmodifiableMap(serums);
    }

    private static Map<LockLevel, DeferredItem<PadlockItem>> padlocks() {
        var padlocks = new EnumMap<LockLevel, DeferredItem<PadlockItem>>(LockLevel.class);
        for (var level : LockLevel.values()) {
            padlocks.put(
                level,
                REGISTER.registerItem(
                    "padlock_" + level.getSerializedName(),
                    properties -> new PadlockItem(properties, level),
                    properties -> PadlockItem.properties(properties, level)
                )
            );
        }
        return Collections.unmodifiableMap(padlocks);
    }

    public static PadlockItem padlock(LockLevel level) {
        return PADLOCKS.get(level).get();
    }

    private static Map<ArmorSet, Map<ArmorPiece, DeferredItem<ArmorPieceItem>>> armor() {
        var sets = new EnumMap<ArmorSet, Map<ArmorPiece, DeferredItem<ArmorPieceItem>>>(ArmorSet.class);
        for (var set : ArmorSet.values()) {
            var pieces = new EnumMap<ArmorPiece, DeferredItem<ArmorPieceItem>>(ArmorPiece.class);
            for (var piece : ArmorPiece.all()) {
                pieces.put(
                    piece,
                    REGISTER.registerItem(
                        set.itemName(piece),
                        properties -> new ArmorPieceItem(properties, set, piece),
                        properties -> ArmorPieceItem.properties(properties, set, piece)
                    )
                );
            }
            sets.put(set, Collections.unmodifiableMap(pieces));
        }
        return Collections.unmodifiableMap(sets);
    }

    public static Stream<Item> armorPieces() {
        return ARMOR.values().stream().flatMap(pieces -> pieces.values().stream()).map(DeferredHolder::get);
    }

    public static ArmorPieceItem armor(ArmorSet set, ArmorPiece piece) {
        return ARMOR.get(set).get(piece).get();
    }

    public static Stream<Item> ammo() {
        return Stream.concat(AMMO.stream().map(DeferredHolder::get), Stream.of(MINI_NUKE.get()));
    }

    private static DeferredItem<PowerArmorPieceItem> piece(String name, PowerArmorSlot slot, int durability) {
        return REGISTER.registerItem(
            name,
            properties -> new PowerArmorPieceItem(properties, slot, "t45"),
            properties -> properties.durability(durability).rarity(Rarity.UNCOMMON)
        );
    }

    public static Stream<DeferredItem<PowerArmorPieceItem>> powerArmorPieces() {
        return Stream.of(T45_HELMET, T45_TORSO, T45_LEFT_ARM, T45_RIGHT_ARM, T45_LEFT_LEG, T45_RIGHT_LEG);
    }

    private static DeferredItem<WeaponItem> weapon(String name, ResourceKey<WeaponDefinition> definition) {
        var item = REGISTER.registerItem(
            name,
            properties -> new WeaponItem(properties, definition),
            properties -> properties.stacksTo(1)
        );
        WEAPONS.add(item);
        return item;
    }

    private static DeferredItem<AmmoItem> ammo(String name) {
        return ammo(name, 1);
    }

    private static DeferredItem<AmmoItem> ammo(String name, int roundsPerItem) {
        var item = REGISTER.registerItem(
            name,
            properties -> new AmmoItem(properties, roundsPerItem),
            properties -> properties.stacksTo(99)
        );
        AMMO.add(item);
        return item;
    }
}

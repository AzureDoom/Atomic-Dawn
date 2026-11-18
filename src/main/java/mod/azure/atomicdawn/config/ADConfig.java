package mod.azure.atomicdawn.config;

import mod.azure.azurelib.config.Config;
import mod.azure.azurelib.config.Configurable;
import mod.azure.azurelib.config.Configurable.Comment;
import mod.azure.azurelib.config.Configurable.DecimalRange;
import mod.azure.azurelib.config.Configurable.Gui;
import mod.azure.azurelib.config.Configurable.Range;

@Config(id = "atomicdawn")
public class ADConfig {

    @Configurable
    public Weapons weapons = new Weapons();

    @Configurable
    public Workbenches workbenches = new Workbenches();

    @Configurable
    public Vats vats = new Vats();

    @Configurable
    public Radiation radiation = new Radiation();

    @Configurable
    public EnvironmentalRadiation environmentalRadiation = new EnvironmentalRadiation();

    @Configurable
    public PowerArmor powerArmor = new PowerArmor();

    @Configurable
    public Armor armor = new Armor();

    @Configurable
    public Special special = new Special();

    @Configurable
    public Perks perks = new Perks();

    @Configurable
    public PipBoy pipboy = new PipBoy();

    @Configurable
    public KillCam killCam = new KillCam();

    @Configurable
    public ClientPipBoy clientPipboy = new ClientPipBoy();

    @Configurable
    public ClientVats clientVats = new ClientVats();

    @Configurable
    public Mutations mutations = new Mutations();

    @Configurable
    public Party party = new Party();

    @Configurable
    public ClientParty clientParty = new ClientParty();

    @Configurable
    public SkylineVisuals skylineVisuals = new SkylineVisuals();

    @Configurable
    public Ghoul ghoul = new Ghoul();

    @Configurable
    public ClientGhoul clientGhoul = new ClientGhoul();

    @Configurable
    public Locks locks = new Locks();

    @Configurable
    public Camp camp = new Camp();

    @Configurable
    public NukaColas nukaCola = new NukaColas();

    public static class Weapons {

        @Comment("Global multiplier applied to all Atomic Dawn weapon damage.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double DAMAGE_MULTIPLIER = 1.0;

        @Comment("Pulling the trigger on an empty magazine starts a reload when reserve ammo is available.")
        @Configurable
        @Configurable.Synchronized
        public boolean AUTO_RELOAD_ON_EMPTY = true;

        @Comment("Spawn block/entity impact particles for hitscan shots.")
        @Configurable
        @Configurable.Synchronized
        public boolean IMPACT_PARTICLES = true;

        @Comment(
            "How many ticks early a shot request may arrive and still be accepted. Absorbs network jitter without letting clients exceed a weapon's fire rate on average."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 5.0)
        public double FIRE_TIMING_TOLERANCE = 1.0;

    }

    public static class Workbenches {

        @Comment(
            "Chance that scrapping a weapon teaches one random locked modification for it (installed mods are always learned)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double SCRAP_LEARN_CHANCE = 0.5;

        @Comment(
            "Legendary modules needed to apply an effect at the workbench, per star (1★ = this, 2★ = 2x, 3★ = 3x)."
        )
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 64)
        public int LEGENDARY_MODULES_PER_STAR = 2;

        @Comment("Chance a hostile mob killed by a player drops a legendary module.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double LEGENDARY_MODULE_DROP_CHANCE = 0.04;

        @Comment("Chance a hostile mob killed by a player drops a random legendary weapon.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double LEGENDARY_WEAPON_DROP_CHANCE = 0.005;

        @Comment("Chance a hostile mob killed by a player drops a random piece of legendary armor.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double LEGENDARY_ARMOR_DROP_CHANCE = 0.005;

    }

    public static class Vats {

        @Comment("Maximum Action Points.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 10000.0)
        public double MAX_ACTION_POINTS = 100.0;

        @Comment("Action Points regenerated per second while not in V.A.T.S.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double AP_REGEN_PER_SECOND = 12.0;

        @Comment("Ticks after spending AP before regeneration resumes.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 1200)
        public int AP_REGEN_DELAY_TICKS = 30;

        @Comment("Critical meter gained per V.A.T.S. hit (meter is full at 100).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double CRITICAL_GAIN_PER_HIT = 15.0;

        @Comment("Half-angle in degrees around the crosshair in which V.A.T.S. can acquire targets.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 90.0)
        public double VATS_TARGET_CONE = 25.0;

        @Comment("Upper limit for any V.A.T.S. hit chance (criticals always hit).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double VATS_MAX_HIT_CHANCE = 0.95;

        @Comment("V.A.T.S. leg hits briefly slow the target and arm hits briefly weaken it.")
        @Configurable
        @Configurable.Synchronized
        public boolean VATS_LIMB_EFFECTS = true;

    }

    public static class Radiation {

        @Comment(
            "Whether explosive weapon projectiles (Fat Man, ...) can destroy blocks. Weapon JSON can also turn it off per weapon."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean EXPLOSIONS_BREAK_BLOCKS = true;

        @Comment(
            "Radius in blocks around a nuclear detonation in which water turns into irradiated water (0 disables)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 32.0)
        public double NUKE_WATER_RADIUS = 16.0;

        @Comment("How long the fallout cloud left by a nuclear detonation lingers (0 disables).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 72000)
        public int FALLOUT_DURATION_TICKS = 2400;

        @Comment("Rads at which radiation sickness is at its worst.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 100000.0)
        public double MAX_RADS = 1000.0;

        @Comment("Fraction of max health removed at maxRads (scales linearly; at least one heart always remains).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double RADS_MAX_HEALTH_LOSS = 0.9;

        @Comment("Rads that wear off on their own per second (0 = only RadAway and death clear them).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double RADS_DECAY_PER_SECOND = 0.0;

        @Comment("Rads per second while standing in irradiated water (doubled when submerged).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double RAD_WATER_RADS_PER_SECOND = 4.0;

        @Comment("Rads received at ground zero of a nuclear detonation (falls off to 0 at twice the blast radius).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double NUKE_RADIATION_BURST = 300.0;

        @Comment("Rads per second at the centre of a fallout cloud (falls off to half at its edge).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double FALLOUT_RADS_PER_SECOND = 10.0;

        @Comment("Rads removed by one RadAway.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double RADAWAY_RADS = 250.0;

        @Comment("Rads removed by one Nuka-Grape (never cures mutations).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double NUKA_GRAPE_RADS = 150.0;

        @Comment("Rads removed by one Brahmin Milk (never cures mutations).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double BRAHMIN_MILK_RADS = 75.0;

        @Comment("Fraction of rads removed when you die (1 clears them all).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double RADS_LOST_ON_DEATH = 0.5;

        @Comment("Damage non-player mobs take per rad they would have received (undead are immune).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double MOB_RADIATION_DAMAGE = 0.04;

    }

    public static class EnvironmentalRadiation {

        @Configurable
        @Configurable.Synchronized
        public boolean BIOME_RADIATION_ENABLED = true;

        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double TOXIC_VALLEY_RADS_PER_SECOND = 0.6;

        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double ASH_HEAP_RADS_PER_SECOND = 0.4;

        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double IRRADIATED_MIRE_RADS_PER_SECOND = 1.3;

        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double CRIMSON_BOG_RADS_PER_SECOND = 1.0;

        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double BURNING_SPRINGS_RADS_PER_SECOND = 1.1;

        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double SKYLINE_VALLEY_RADS_PER_SECOND = 0.3;

        @Configurable
        @Configurable.Synchronized
        public boolean RADIATION_HOTSPOTS_ENABLED = true;

        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double RADIATION_HOTSPOT_RADS_PER_SECOND = 6.0;

        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 12)
        public int RADIATION_HOTSPOT_RADIUS = 6;

    }

    public static class PowerArmor {

        @Comment("Energy stored in a full Power Core.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 10000000)
        public int FUSION_CORE_CAPACITY = 30000;

        @Comment("Core energy drained per tick while walking in Power Armor.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double FUSION_CORE_MOVE_COST = 1.0;

        @Comment("Core energy drained per attack made while in Power Armor.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10000.0)
        public double FUSION_CORE_ATTACK_COST = 12.0;

        @Comment("Movement speed multiplier while piloting Power Armor.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.1, max = 4.0)
        public double POWER_ARMOR_SPEED = 1.0;

        @Comment("Damage reduction with a full, intact set of armor pieces.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double POWER_ARMOR_MAX_REDUCTION = 0.85;

        @Comment("Piece durability lost per point of damage absorbed.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double POWER_ARMOR_CONDITION_PER_DAMAGE = 2.0;

        @Comment("Fraction of a piece's durability restored per repair action.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.01, max = 1.0)
        public double POWER_ARMOR_REPAIR_FRACTION = 0.25;

        @Comment("Fraction of a piece's fabrication cost returned when scrapped (scaled by condition).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double POWER_ARMOR_SCRAP_RETURN = 0.5;

        @Comment("Fall distance (blocks) that triggers the heavy landing.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 3.0, max = 256.0)
        public double POWER_ARMOR_LANDING_HEIGHT = 8.0;

        @Comment("Ticks the armor is locked in place after a heavy landing.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 200)
        public int POWER_ARMOR_LANDING_TICKS = 18;

        @Comment("How many standard mods a single Power Armor piece can hold.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 8)
        public int POWER_ARMOR_MODS_PER_PIECE = 1;

        @Comment("Most damage the legendary effects of a Power Armor suit can remove together.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 0.95)
        public double POWER_ARMOR_LEGENDARY_MAX_REDUCTION = 0.6;

        @Comment("Core energy drained per tick while the jet pack fires.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double POWER_ARMOR_JETPACK_COST = 6.0;

        @Comment("Action Points drained per tick while the Power Armor jet pack fires.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double POWER_ARMOR_JETPACK_AP_COST = 1.5;

    }

    public static class Armor {

        @Comment("Multiplier on the movement-speed penalty of Sturdy and Heavy armor (0 disables it).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 4.0)
        public double ARMOR_WEIGHT_PENALTY_MULTIPLIER = 1.0;

        @Comment("Most damage the legendary and misc effects of worn armor can remove together.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 0.95)
        public double ARMOR_LEGENDARY_MAX_REDUCTION = 0.6;

        @Comment("Fraction of a piece's durability restored per repair at the Armor Bench.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.01, max = 1.0)
        public double ARMOR_REPAIR_FRACTION = 0.25;

        @Comment("Fraction of a piece's crafting cost returned when scrapped (scaled by condition).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double ARMOR_SCRAP_RETURN = 0.5;

        @Comment("Action Points drained per tick while an armor jet pack fires.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double ARMOR_JETPACK_AP_COST = 1.5;

        @Comment("Upward thrust per tick of an armor jet pack, multiplied by the mod's strength.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 4.0)
        public double ARMOR_JETPACK_THRUST = 1.0;

    }

    public static class Special {

        @Comment("Master switch for S.P.E.C.I.A.L. progression and its gameplay effects.")
        @Configurable
        @Configurable.Synchronized
        public boolean SPECIAL_ENABLED = true;

        @Comment("Base points every attribute starts with.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 15)
        public int SPECIAL_STARTING_POINTS = 1;

        @Comment("Highest base value an attribute can be allocated to.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 100)
        public int SPECIAL_MAX_BASE = 15;

        @Comment("Clamp for effective (base + temporary) attribute values.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 200)
        public int SPECIAL_MAX_EFFECTIVE = 20;

        @Comment("Last character level that awards an attribute point.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 10000)
        public int SPECIAL_ATTRIBUTE_LEVEL_CAP = 50;

        @Comment("Maximum character level (0 = uncapped; levels past the attribute cap still award perk picks).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 10000)
        public int SPECIAL_MAX_LEVEL = 0;

        @Comment("Attribute points awarded per level up to attributeLevelCap.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 10)
        public int SPECIAL_POINTS_PER_LEVEL = 1;

        @Comment("Number of saved loadouts per character (existing extra loadouts are never deleted).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 8)
        public int SPECIAL_LOADOUT_SLOTS = 2;

        @Comment(
            "Allow switching loadouts and respeccing away from a loadout station (still requires being out of combat)."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean SPECIAL_ALLOW_ANYWHERE_SWITCH = false;

        @Comment("Seconds without dealing or taking damage before an anywhere-switch is allowed.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 600)
        public int SPECIAL_OUT_OF_COMBAT_SWITCH_SECONDS = 20;

        @Comment(
            "Seconds after combat during which cards cannot be equipped/unequipped and points cannot be allocated (0 disables)."
        )
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 600)
        public int SPECIAL_BUILD_EDIT_COMBAT_LOCK_SECONDS = 8;

        @Comment(
            "Blocks within which a block tagged #atomicdawn:loadout_station counts as a loadout station (0 = no stations)."
        )
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 16)
        public int SPECIAL_STATION_RADIUS = 4;

        @Comment("Melee/unarmed damage bonus per Strength point above the starting value.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double SPECIAL_STRENGTH_MELEE_PER_POINT = 0.02;

        @Comment("Max health per Endurance point above the starting value (1 = half a heart).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double SPECIAL_ENDURANCE_HEALTH_PER_POINT = 1.0;

        @Comment("Max Action Points per Agility point above the starting value.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double SPECIAL_AGILITY_AP_PER_POINT = 2.0;

        @Comment("Critical meter gain bonus per Luck point above the starting value.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double SPECIAL_LUCK_CRIT_GAIN_PER_POINT = 0.01;

        @Comment("V.A.T.S. hit chance added per Perception point above the starting value (never applied to 0% shots).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 0.1)
        public double SPECIAL_PERCEPTION_HIT_CHANCE_PER_POINT = 0.005;

        @Comment("Weapon condition loss reduction per Intelligence point above the starting value.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 0.1)
        public double SPECIAL_INTELLIGENCE_CONDITION_PER_POINT = 0.01;

        @Comment("Atomic Dawn XP for killing a hostile mob, before the health bonus.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 100000)
        public int XP_KILL_BASE = 10;

        @Comment("Additional Atomic Dawn XP per point of the victim's max health.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double XP_KILL_PER_HEALTH = 1.0;

        @Comment("Anti-farm budget: most kill XP a player can earn per rolling minute (0 = unlimited).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 10000000)
        public int XP_KILL_PER_MINUTE_CAP = 1500;

    }

    public static class Perks {

        @Comment("Master switch for perk cards. Owned cards are kept when disabled.")
        @Configurable
        @Configurable.Synchronized
        public boolean PERKS_ENABLED = true;

        @Comment("Perk picks a new character starts with.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 100)
        public int PERKS_STARTING_PICKS = 2;

        @Comment("Cards granted to new characters, as \"namespace:id\" or \"namespace:id@rank\".")
        @Configurable
        @Configurable.Synchronized
        public String[] PERKS_STARTER_CARDS = new String[0];

        @Comment("Whether team-aura cards (e.g. Inspirational) affect allied players.")
        @Configurable
        @Configurable.Synchronized
        public boolean PERKS_TEAM_AURAS = true;

        @Comment("Radius in blocks for team-aura cards.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 256.0)
        public double PERKS_TEAM_AURA_RADIUS = 32.0;

        @Comment("Cap on the aggregate additive direct-damage bonus from S.P.E.C.I.A.L. and perks.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double PERKS_MAX_DAMAGE_BONUS = 0.75;

        @Comment("Cap on the aggregate critical damage bonus.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double PERKS_MAX_CRITICAL_DAMAGE_BONUS = 1.0;

        @Comment("Cap on the aggregate V.A.T.S. hit chance bonus.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double PERKS_MAX_VATS_HIT_BONUS = 0.25;

        @Comment("Floor for the V.A.T.S. AP cost multiplier.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double PERKS_MIN_AP_COST_MULTIPLIER = 0.4;

        @Comment("Cap on radiation resistance from perks.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double PERKS_MAX_RADIATION_RESISTANCE = 0.85;

        @Comment("Cap on weapon condition loss reduction.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double PERKS_MAX_CONDITION_REDUCTION = 0.75;

        @Comment("Cap on the combined personal + team XP bonus.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double PERKS_MAX_XP_BONUS = 1.0;

        @Comment("Cap on the aggregate incoming damage reduction from perk cards.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 0.95)
        public double PERKS_MAX_DAMAGE_REDUCTION = 0.6;

        @Comment("Cap on the movement speed bonus from perk cards.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 2.0)
        public double PERKS_MAX_SPEED_BONUS = 0.3;

        @Comment("Seconds between activations of once-per-cooldown perks (auto heal, cheat death, survival aid).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 3600)
        public int PERKS_PROC_COOLDOWN_SECONDS = 60;

        @Comment("Health spent per AP when a card converts V.A.T.S. AP cost into health.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double PERKS_VATS_HEALTH_PER_AP = 0.2;

        @Comment("Master switch for legendary perks, perk coins and card scrapping.")
        @Configurable
        @Configurable.Synchronized
        public boolean LEGENDARY_ENABLED = true;

        @Comment(
            "Character levels that unlock each legendary perk slot (at most 6 are used). Fallout 76 uses 50, 75, 100, 150, 200, 300."
        )
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 10000)
        public int[] LEGENDARY_SLOT_LEVELS = new int[] { 25, 50, 75, 100, 125, 150 };

        @Comment("Perk coins granted for each rank of a regular perk card that is scrapped.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 1000)
        public int LEGENDARY_COINS_PER_SCRAPPED_RANK = 2;

    }

    public static class PipBoy {

        @Comment("Whether players can open the Pip-Boy.")
        @Configurable
        @Configurable.Synchronized
        public boolean PIPBOY_ENABLED = true;

        @Comment("Initialise every player's Pip-Boy on first join.")
        @Configurable
        @Configurable.Synchronized
        public boolean PIPBOY_GRANT_ON_FIRST_JOIN = true;

        @Comment("Send Pip-Boy notifications (level up, new perk picks, ...).")
        @Configurable
        @Configurable.Synchronized
        public boolean PIPBOY_NOTIFICATIONS = true;

        @Comment("Maximum favorited item types per player.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 256)
        public int PIPBOY_MAX_FAVORITES = 48;

        @Comment(
            "Whether the Pip-Boy MAP tab renders terrain (only chunks already loaded on the client are ever shown)."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean PIPBOY_ENABLE_MAP = true;

        @Comment("Whether players can switch on the Pip-Boy light (a real light source that follows them).")
        @Configurable
        @Configurable.Synchronized
        public boolean PIPBOY_LIGHT_ENABLED = true;

        @Comment("Brightness of the Pip-Boy light (vanilla light level).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 15)
        public int PIPBOY_LIGHT_LEVEL = 13;

        @Comment("Whether the Pip-Boy RADIO tab can tune stations.")
        @Configurable
        @Configurable.Synchronized
        public boolean PIPBOY_ENABLE_RADIO = true;

        @Comment("Atomic Dawn XP awarded for discovering a new location.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 100000)
        public int PIPBOY_DISCOVERY_XP = 25;

        @Comment("Maximum map waypoints per player.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 256)
        public int PIPBOY_MAX_WAYPOINTS = 64;

        @Comment("How often (in ticks) each player is checked for nearby locations.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 10, max = 1200)
        public int PIPBOY_LOCATION_SCAN_TICKS = 40;

    }

    public static class KillCam {

        public enum KillCamMode {
            OFF,
            VATS,
            ALWAYS
        }

        @Comment("When the camera follows your fired explosive rounds (mini nukes) until they detonate.")
        @Configurable(side = Configurable.Side.CLIENT)
        public KillCamMode KILL_CAM = KillCamMode.VATS;

        @Comment("How far behind the round the kill cam sits, in blocks.")
        @Configurable(side = Configurable.Side.CLIENT)
        @DecimalRange(min = 0.5, max = 8.0)
        public double KILL_CAM_DISTANCE = 2.25;

        @Comment("Ticks the kill cam lingers on the detonation before returning to the player.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 200)
        public int KILL_CAM_HOLD_TICKS = 30;

    }

    public static class ClientPipBoy {

        @Comment("Show the Pip-Boy fullscreen without the device frame (identical features).")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_FULLSCREEN = false;

        public enum PipBoyLayout {
            FALLOUT_76,
            FALLOUT_4,
            NEW_VEGAS,
            FALLOUT_3,
            FALLOUT_1_2
        }

        @Comment(
            "Pip-Boy interface layout and device style: FALLOUT_76, FALLOUT_4, NEW_VEGAS, FALLOUT_3 or FALLOUT_1_2."
        )
        @Configurable(side = Configurable.Side.CLIENT)
        public PipBoyLayout PIPBOY_LAYOUT = PipBoyLayout.FALLOUT_76;

        @Comment("High-contrast Pip-Boy palette.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_HIGH_CONTRAST = false;

        @Comment("CRT scanline intensity (0 disables).")
        @Configurable(side = Configurable.Side.CLIENT)
        @DecimalRange(min = 0.0, max = 1.0)
        public double PIPBOY_SCANLINES = 0.35;

        @Comment("Pip-Boy display colour (default Pip-Boy green #1AFF80).")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue
        public String PIPBOY_COLOR = "#1AFF80";

        @Comment("Play Pip-Boy interface sounds.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_SOUNDS = true;

        @Comment("Reopen the Pip-Boy on the last viewed tab (otherwise STAT).")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_REMEMBER_TAB = true;

        @Comment("How long Pip-Boy notifications stay on screen (0 mutes them).")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 1200)
        public int PIPBOY_NOTIFICATION_TICKS = 100;

        @Comment("Pip-Boy radio volume (also scaled by the Jukebox/Note Blocks volume slider).")
        @Configurable(side = Configurable.Side.CLIENT)
        @DecimalRange(min = 0.0, max = 1.0)
        public double PIPBOY_RADIO_VOLUME = 0.8;

        @Comment("Keep the radio playing after the Pip-Boy is closed.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_RADIO_WHEN_CLOSED = true;

        @Comment("Stop vanilla background music while the radio is playing.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_RADIO_PAUSES_MUSIC = true;

        @Comment("Render the Pip-Boy map in terrain colours instead of the monochrome display colour.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_MAP_COLOR = false;

        @Comment("Play the Pip-Boy start-up sequence (CRT warm-up, PIP-OS boot text, emblem) and power-off animation.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_BOOT_SEQUENCE = true;

        @Comment("Reopening within this many seconds of closing skips the full boot and only warms up the screen.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 86400)
        public int PIPBOY_BOOT_COOLDOWN_SECONDS = 300;

        @Comment("Show a compass strip at the top of the screen with your Pip-Boy waypoints.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean WAYPOINT_HUD = false;

        @Comment("Width of the waypoint compass in GUI pixels.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 80, max = 480)
        public int WAYPOINT_HUD_WIDTH = 200;

        @Comment("Distance of the waypoint compass from the top of the screen (raise it if boss bars overlap).")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 200)
        public int WAYPOINT_HUD_Y = 2;

        @Comment("Hide waypoints farther away than this many blocks (0 shows all in the dimension).")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 100000)
        public int WAYPOINT_HUD_MAX_DISTANCE = 0;

        @Comment("Show the name of the waypoint you face when it is within this many degrees of the centre.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 90)
        public int WAYPOINT_HUD_LABEL_ANGLE = 10;

        @Comment("Add the distance in blocks to the waypoint label.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean WAYPOINT_HUD_DISTANCE = true;

        public enum PipBoyArm {
            LEFT,
            RIGHT,
            MAIN_HAND,
            OFF_HAND
        }

        @Comment("Render the Pip-Boy on players' forearms.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_ARM_DEVICE = true;

        @Comment("Which arm the Pip-Boy is worn on. MAIN_HAND/OFF_HAND follow each player's own main-hand setting.")
        @Configurable(side = Configurable.Side.CLIENT)
        public PipBoyArm PIPBOY_ARM = PipBoyArm.LEFT;

        @Comment("Light the worn Pip-Boy's screen in the Pip-Boy colour (glows in the dark).")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PIPBOY_ARM_SCREEN_GLOW = true;

        public enum PipBoyFirstPersonMode {
            OFF,
            BARE_ARM,
            UNARMED_BOTH_ARMS
        }

        @Comment(
            "First-person Pip-Boy. BARE_ARM: only on the arm vanilla already shows (empty main hand, maps). UNARMED_BOTH_ARMS: when both hands are empty, also raise the off arm so an off-arm Pip-Boy is visible."
        )
        @Configurable(side = Configurable.Side.CLIENT)
        public PipBoyFirstPersonMode PIPBOY_ARM_FIRST_PERSON = PipBoyFirstPersonMode.UNARMED_BOTH_ARMS;

    }

    public static class Mutations {

        @Comment("Whether mutations exist at all.")
        @Configurable
        @Configurable.Synchronized
        public boolean MUTATIONS_ENABLED = true;

        @Comment("Chance per rad received to gain a random mutation.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double MUTATION_CHANCE_PER_RAD = 0.0015;

        @Comment("Most mutations a player can hold at once (Carnivore and Herbivore exclude each other).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 19)
        public int MUTATION_MAX = 18;

        @Comment("Minutes a mutation serum suppresses that mutation's negative effects.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 10080)
        public int MUTATION_SERUM_MINUTES = 60;

        @Comment("Chance each RadAway removes one random mutation.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double RADAWAY_MUTATION_CURE_CHANCE = 0.3;

    }

    public static class Ghoul {

        @Comment(
            "Whether the playable Ghoul system exists at all. Disabling keeps saved forms but turns every Ghoul mechanic off."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean GHOUL_ENABLED = true;

        @Comment("Whether players can become Ghouls through treatments or quests (operators can always use commands).")
        @Configurable
        @Configurable.Synchronized
        public boolean ALLOW_TRANSFORMATION = true;

        @Comment("Whether Ghouls can be restored to human through treatment.")
        @Configurable
        @Configurable.Synchronized
        public boolean ALLOW_REVERSION = true;

        @Comment(
            "Real-world hours (server wall clock) required between completed form changes (0 disables the cooldown)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 8760.0)
        public double FORM_CHANGE_COOLDOWN_HOURS = 24.0;

        @Comment("Minimum Atomic Dawn level required to become a Ghoul.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 10000)
        public int MINIMUM_LEVEL = 0;

        @Comment("Dimensions (\"namespace:id\") in which form changes are not allowed.")
        @Configurable
        @Configurable.Synchronized
        public String[] BLOCKED_DIMENSIONS = new String[0];

        @Comment("Maximum Glow before perk bonuses.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 10000.0)
        public double GLOW_MAXIMUM = 100.0;

        @Comment("Fraction of post-resistance radiation converted into healing, then Glow.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double GLOW_CONVERSION = 0.35;

        @Comment("Glow lost per second while online.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double GLOW_DECAY_PER_SECOND = 0.1;

        @Comment("Max health added at full Glow (scales in 10% steps).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double GLOW_MAX_HEALTH_BONUS = 20.0;

        @Comment("Maximum Feral stability.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 10000.0)
        public double MAXIMUM_STABILITY = 100.0;

        @Comment("Stability lost every 120 seconds while alive and online.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double STABILITY_DECAY_PER_120_SECONDS = 1.0;

        @Comment("Stability restored by a Feral Stabilizer.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10000.0)
        public double STABILIZER_RESTORE = 35.0;

        @Comment("Melee damage multiplier at zero stability.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 10.0)
        public double FERAL_MELEE_MULTIPLIER = 1.5;

        @Comment("Ranged accuracy multiplier at zero stability (lower widens spread).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.05, max = 1.0)
        public double FERAL_RANGED_ACCURACY_MULTIPLIER = 0.65;

        @Comment("V.A.T.S. hit chance multiplier at zero stability.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double FERAL_VATS_ACCURACY_MULTIPLIER = 0.70;

        @Comment("AP regeneration multiplier at zero stability.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double FERAL_AP_REGEN_MULTIPLIER = 0.8;

        @Comment("Extra damage taken at zero stability (0.15 = +15%).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 5.0)
        public double FERAL_DAMAGE_TAKEN_BONUS = 0.15;

        @Comment("Pause Glow and stability decay in creative mode.")
        @Configurable
        @Configurable.Synchronized
        public boolean PAUSE_DECAY_IN_CREATIVE = true;

        @Comment("Warn Ghouls when stability drops to 40, 20 and 0.")
        @Configurable
        @Configurable.Synchronized
        public boolean STABILITY_WARNINGS = true;

        @Comment("Ghouls do not lose food from exhaustion (saturation from eating still wears off).")
        @Configurable
        @Configurable.Synchronized
        public boolean DISABLE_EXHAUSTION_HUNGER_LOSS = true;

        @Comment("Allow Ghouls to choose between appearance variants.")
        @Configurable
        @Configurable.Synchronized
        public boolean ALLOW_APPEARANCE_VARIANTS = true;

    }

    public static class ClientGhoul {

        @Comment("Show the Glow and Feral stability meters while you are a Ghoul.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean GHOUL_HUD = true;

        @Comment("Render Ghoul appearance layers on players.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean GHOUL_COSMETICS = true;

        @Comment("Play Geiger clicks when radiation charges your Glow.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean GHOUL_GEIGER = true;

    }

    public static class Party {

        @Comment("Whether players can form parties with /party.")
        @Configurable
        @Configurable.Synchronized
        public boolean PARTY_ENABLED = true;

        @Comment("Most players in one party.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 2, max = 32)
        public int PARTY_MAX_SIZE = 4;

        @Comment("Whether party members can hurt each other.")
        @Configurable
        @Configurable.Synchronized
        public boolean PARTY_FRIENDLY_FIRE = false;

        @Comment("How long a party invite stays open, in seconds.")
        @Configurable
        @Range(min = 5, max = 3600)
        public int PARTY_INVITE_SECONDS = 60;

    }

    public static class ClientParty {

        @Comment("Show your party members' health and distance on the HUD.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PARTY_HUD = true;

        @Comment("Distance of the party list from the top of the screen.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 400)
        public int PARTY_HUD_Y = 20;

        @Comment("Show party members on the waypoint compass.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean PARTY_ON_COMPASS = true;

    }

    public static class ClientVats {

        @Comment("Main V.A.T.S. HUD colour: titles, AP bar, selected body part, hits.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR = "#FF7CFC9A";

        @Comment("Dimmed V.A.T.S. colour: unselected body parts, hints, an uncharged critical meter.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_DIM = "#B07CFC9A";

        @Comment("Warning colour: not enough AP, no target, the AP cost marker.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_WARN = "#FFFFB347";

        @Comment("Critical colour: a charged or armed critical.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_CRITICAL = "#FFFFE066";

        @Comment("Miss colour: misses, 0% body parts and the target's health bar.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_MISS = "#FFFF5050";

        @Comment("Panel background behind the AP/CRIT meters and body part labels.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_BACKGROUND = "#80000000";

        @Comment("Empty part of the AP, CRIT and target health bars.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_BAR_BACKGROUND = "#60FFFFFF";

        @Comment("Full-screen tint while V.A.T.S. is active.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_TINT = "#2234FF6A";

        @Comment("Fill of the box around the selected target.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_TARGET_FILL = "#3034FF6A";

        @Comment("Outline of the box around the selected target.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String VATS_COLOR_TARGET_OUTLINE = "#FF34FF6A";

    }

    public static class SkylineVisuals {

        @Comment("Render the swirling storm vortex over Skyline Valley.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean SKYLINE_SKY = true;

        @Comment("Render the Skyline Valley storm: the sky vortex, falling embers and lightning arcs.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean SKYLINE_STORM_VISUALS = true;

        @Comment("Skyline Valley storm particle detail (embers and lightning arcs).")
        @Configurable(side = Configurable.Side.CLIENT)
        @Range(min = 0, max = 3)
        public int SKYLINE_STORM_DETAIL = 1;

        @Comment("Rotation speed multiplier for the Skyline Valley vortex.")
        @Configurable(side = Configurable.Side.CLIENT)
        @DecimalRange(min = 0.0, max = 10.0)
        public double SKYLINE_SKY_SPIN = 1.0;

    }

    public static class Locks {

        @Comment(
            "Whether padlocks and lockpicking are active. Disabling leaves saved locks in place but stops enforcing them."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean LOCKS_ENABLED = true;

        @Comment("Stop players who do not own a lock from breaking the locked block.")
        @Configurable
        @Configurable.Synchronized
        public boolean LOCKS_PROTECT_BLOCKS = true;

        @Comment("Creative mode players ignore locks and can remove any lock.")
        @Configurable
        @Configurable.Synchronized
        public boolean LOCKS_CREATIVE_BYPASS = true;

        @Comment(
            "Whether players can pick locks placed by other players (locks without an owner can always be picked)."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean LOCKS_ALLOW_PICKING_PLAYER_LOCKS = true;

        @Comment("Require the Picklock perk tier for Advanced, Expert and Master locks.")
        @Configurable
        @Configurable.Synchronized
        public boolean LOCKPICK_REQUIRE_PERKS = true;

        @Comment("Multiplier for the size of the hidden sweet spot.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.25, max = 4.0)
        public double LOCKPICK_SWEET_SPOT_SCALE = 1.0;

        @Comment("Multiplier for how long a bobby pin survives being forced.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.25, max = 10.0)
        public double LOCKPICK_PIN_DURABILITY_SCALE = 1.0;

        @Comment("Atomic Dawn XP per lock tier for a successful pick (Novice x1 through Master x4).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 10000)
        public int LOCKPICK_XP = 10;

    }

    public static class Camp {

        @Comment("Whether players can deploy C.A.M.P. modules.")
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_ENABLED = true;

        @Comment("Horizontal radius of a C.A.M.P. claim, in blocks.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 8, max = 128)
        public int CAMP_RADIUS = 24;

        @Comment("How far above the module a claim extends.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 4, max = 256)
        public int CAMP_HEIGHT_UP = 32;

        @Comment("How far below the module a claim extends.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 256)
        public int CAMP_HEIGHT_DOWN = 16;

        @Comment("Minimum horizontal distance between two C.A.M.P. modules. Never smaller than twice the radius.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 4096)
        public int CAMP_MIN_SPACING = 64;

        @Comment("C.A.M.P. slots per player. Only one can be deployed at a time.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 10)
        public int CAMP_SLOTS = 2;

        @Comment("Build budget for each C.A.M.P.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 50, max = 100000)
        public int CAMP_BUDGET = 500;

        @Comment("Dimensions (\"namespace:id\") where modules may be deployed. Empty allows every dimension.")
        @Configurable
        @Configurable.Synchronized
        public String[] CAMP_DIMENSIONS = new String[] { "minecraft:overworld" };

        @Comment("Creative mode players can manage and remove any C.A.M.P.")
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_CREATIVE_BYPASS = true;

        @Comment("How far from the player Workshop Mode can place or scrap objects.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 4, max = 64)
        public int CAMP_BUILD_REACH = 16;

        @Comment("Fraction of an object's material cost returned when it is scrapped.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double CAMP_SCRAP_REFUND = 0.5;

        @Comment("Buildables that name a plan stay locked until the player learns that plan.")
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_REQUIRE_PLANS = true;

        @Comment("Catalogue blocks the owner places by hand inside their territory become C.A.M.P. objects.")
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_TRACK_MANUAL = true;

        @Comment("Explosions can't destroy C.A.M.P. modules or objects.")
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_PROTECT_EXPLOSIONS = true;

        @Comment(
            "Scrapping an object gives back the block itself, keeping its contents and data, instead of part of its material cost."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_SCRAP_RETURNS_BLOCK = true;

        @Comment("Objects each player can keep in C.A.M.P. storage.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 1024)
        public int CAMP_STORAGE_SLOTS = 256;

        @Comment("Personal blueprints each player can keep. 0 disables saving blueprints.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 100)
        public int CAMP_BLUEPRINT_SLOTS = 10;

        @Comment("Also write every saved blueprint as a datapack-ready JSON file on the server.")
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_BLUEPRINT_EXPORT = true;

        @Comment(
            "Write exported blueprints straight into the world's datapacks folder so they load as global blueprints on the next restart. When off they go to <world>/atomicdawn_blueprints."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean CAMP_BLUEPRINT_EXPORT_TO_DATAPACKS = false;

    }

    public static class NukaColas {

        @Comment("Health restored by Nuka-Cola, Nuka-Cola Orange, Nuka-Cola Wild and Nuka-Cola Vaccinated.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double NUKA_COLA_HEALTH = 4.0;

        @Comment("Action Points restored by Nuka-Cola, Nuka-Cola Orange, Nuka-Cola Wild and Nuka-Cola Vaccinated.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10000.0)
        public double NUKA_COLA_AP = 25.0;

        @Comment("Rads received from Nuka-Cola, Nuka-Cola Orange, Nuka-Cola Wild and Nuka-Cola Vaccinated.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double NUKA_COLA_RADS = 5.0;

        @Comment("Health restored by Nuka-Cola Cranberry.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double NUKA_CRANBERRY_HEALTH = 4.0;

        @Comment("Bonus XP fraction granted while Nuka-Cola Cranberry is active (0.1 = +10%).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double NUKA_CRANBERRY_XP_BONUS = 0.1;

        @Comment("Minutes the Nuka-Cola Cranberry XP bonus lasts.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 1440)
        public int NUKA_CRANBERRY_MINUTES = 20;

        @Comment("Health restored by Nuka-Cherry.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double NUKA_CHERRY_HEALTH = 10.0;

        @Comment("Action Points restored by Nuka-Cherry.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10000.0)
        public double NUKA_CHERRY_AP = 25.0;

        @Comment("Rads received from Nuka-Cherry.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double NUKA_CHERRY_RADS = 4.0;

        @Comment("Health restored by Nuka-Cola Quantum.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double NUKA_QUANTUM_HEALTH = 20.0;

        @Comment("Action Points restored by Nuka-Cola Quantum.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10000.0)
        public double NUKA_QUANTUM_AP = 100.0;

        @Comment("Rads received from Nuka-Cola Quantum.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double NUKA_QUANTUM_RADS = 10.0;

        @Comment("Seconds the Nuka-Cola Dark effect lasts.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 86400)
        public int NUKA_DARK_SECONDS = 180;

        @Comment("Strength and Endurance gained while Nuka-Cola Dark is active.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 15)
        public int NUKA_DARK_BONUS = 1;

        @Comment("Perception lost while Nuka-Cola Dark is active.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 15)
        public int NUKA_DARK_PENALTY = 1;

        @Comment("Minutes the Nuka-Cola Twist S.P.E.C.I.A.L. boost lasts.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 1440)
        public int NUKA_TWIST_MINUTES = 10;

        @Comment("Points added to a random S.P.E.C.I.A.L. stat by Nuka-Cola Twist.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 15)
        public int NUKA_TWIST_BONUS = 2;

    }

}

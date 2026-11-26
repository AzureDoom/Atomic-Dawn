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
    public Worldgen worldgen = new Worldgen();

    @Configurable
    public Limbs limbs = new Limbs();

    @Configurable
    public Junk junk = new Junk();

    @Configurable
    public Story story = new Story();

    @Configurable
    public ClientLimbs clientLimbs = new ClientLimbs();

    @Configurable
    public Thirst thirst = new Thirst();

    @Configurable
    public FeralGhouls feralGhouls = new FeralGhouls();

    @Configurable
    public Scorched scorched = new Scorched();

    @Configurable
    public SuperMutants superMutants = new SuperMutants();

    @Configurable
    public Scorchbeast scorchbeast = new Scorchbeast();

    @Configurable
    public MisterHandy misterHandy = new MisterHandy();

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
    public ClientRadStorm clientRadStorm = new ClientRadStorm();

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

        @Comment("Max V.A.T.S. targeting distance for ranged weapons; a shorter weapon range still applies.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 4.0, max = 256.0)
        public double VATS_MAX_RANGE = 48.0;

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

        @Comment("Rads received for each bottle of Dirty Water collected.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double DIRTY_WATER_COLLECT_RADS = 5.0;

        @Comment("Rads received from drinking Dirty Water.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double DIRTY_WATER_DRINK_RADS = 15.0;

        @Comment("Rads received from drinking Boiled Water.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double BOILED_WATER_DRINK_RADS = 5.0;

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

        @Comment("Rads received from eating raw beef butchered from a brahmin. Cooking removes them.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100000.0)
        public double RAW_BRAHMIN_BEEF_RADS = 15.0;

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

        @Comment("Nuclear detonations leave an irradiated blast zone behind.")
        @Configurable
        @Configurable.Synchronized
        public boolean BLAST_ZONES_ENABLED = true;

        @Comment(
            "Rads per second at the centre of a fresh blast zone (falls off to a quarter at its edge and fades as it decays)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double BLAST_ZONE_RADS_PER_SECOND = 3.0;

        @Comment("Blast zone radius as a multiple of the detonation's blast radius.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double BLAST_ZONE_RADIUS_MULTIPLIER = 3.0;

        @Comment("Ticks a blast zone stays irradiated (24000 = one Minecraft day).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 10000000)
        public int BLAST_ZONE_DURATION_TICKS = 72000;

        @Comment("Rain over the wasteland can turn into a green rad storm.")
        @Configurable
        @Configurable.Synchronized
        public boolean RAD_STORMS_ENABLED = true;

        @Comment("Chance that a new spell of rain is a rad storm.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double RAD_STORM_CHANCE = 0.35;

        @Comment("Rads per second while exposed to the open sky in a wasteland biome during a rad storm.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double RAD_STORM_RADS_PER_SECOND = 1.5;

        @Comment("Fissure sites and waste disposal spots give off radiation.")
        @Configurable
        @Configurable.Synchronized
        public boolean RADIATION_SITES_ENABLED = true;

        @Comment(
            "Rads per second at the centre of a fissure site or waste disposal spot (falls off to a quarter at its edge)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double RADIATION_SITE_RADS_PER_SECOND = 2.5;

        @Comment("Rads per second while standing in ordinary water in a wasteland biome (doubled when submerged).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double WASTELAND_WATER_RADS_PER_SECOND = 1.0;

        @Comment("Waste barrels, toxic sludge and fissure blocks irradiate players standing near them.")
        @Configurable
        @Configurable.Synchronized
        public boolean RADIATION_HOTSPOTS_ENABLED = true;

        @Comment("Rads per second right next to a radiation hotspot block (falls off with distance).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double RADIATION_HOTSPOT_RADS_PER_SECOND = 6.0;

        @Comment("Distance in blocks searched around each player for radiation hotspot blocks.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 12)
        public int RADIATION_HOTSPOT_RADIUS = 6;

        @Comment("Some wasteland biomes give off constant background radiation.")
        @Configurable
        @Configurable.Synchronized
        public boolean BIOME_RADIATION_ENABLED = true;

        @Comment(
            "Rads per second under open sky in lightly irradiated biomes (biome tag atomicdawn:ambient_radiation_low)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double BIOME_RADIATION_LOW_RADS_PER_SECOND = 0.02;

        @Comment(
            "Rads per second under open sky in heavily irradiated biomes such as the Glowing Sea (biome tag atomicdawn:ambient_radiation_high)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double BIOME_RADIATION_HIGH_RADS_PER_SECOND = 1.0;

        @Comment("Multiplier applied to biome radiation while sheltered with no open sky above.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double BIOME_RADIATION_SHELTER_MULTIPLIER = 0.5;

        @Comment(
            "Rads per second at the centre of extreme radiation sites such as The Glow (falls off to a quarter at its edge)."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double EXTREME_SITE_RADS_PER_SECOND = 10.0;

        @Comment("Toxic cloud sites such as the Sierra Madre poison players while they are inside.")
        @Configurable
        @Configurable.Synchronized
        public boolean TOXIC_CLOUDS_ENABLED = true;

    }

    public static class Worldgen {

        @Comment("Highways and dirt roads skip the pieces that would run through other surface structures.")
        @Configurable
        public boolean ROADS_AVOID_STRUCTURES = true;

        @Comment("Blocks of clearance kept between roads and the start chunk of other structures.")
        @Configurable
        @Range(min = 0, max = 128)
        public int ROAD_STRUCTURE_CLEARANCE = 16;

    }

    public static class Story {

        @Comment("Experience awarded the first time a dialogue skill check succeeds (checks can override this).")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 100000)
        public int DIALOGUE_CHECK_XP = 25;

        @Comment("Success chance of a chance-based dialogue check when the player's stat exactly matches the target.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double DIALOGUE_CHANCE_AT_TARGET = 0.5;

        @Comment("Extra success chance per point of Luck on chance-based dialogue checks.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 0.1)
        public double DIALOGUE_LUCK_BONUS = 0.01;

        @Comment("Lowest possible success chance of a chance-based dialogue check.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double DIALOGUE_CHANCE_MIN = 0.05;

        @Comment("Highest possible success chance of a chance-based dialogue check.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double DIALOGUE_CHANCE_MAX = 0.95;

        @Comment("Multiplier applied to every faction reputation change.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double REPUTATION_MULTIPLIER = 1.0;

        @Comment("Show a message whenever faction reputation changes.")
        @Configurable
        @Configurable.Synchronized
        public boolean REPUTATION_NOTIFICATIONS = true;

        @Comment("Factions whose rank is marked hostile attack the player and refuse to talk.")
        @Configurable
        @Configurable.Synchronized
        public boolean FACTION_HOSTILITY = true;

        @Comment("Show companion reactions such as 'Piper liked that.'")
        @Configurable
        @Configurable.Synchronized
        public boolean COMPANION_REACTIONS = true;

        @Comment("Blocks within which companions notice and react to what the player does.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 1, max = 256)
        public int COMPANION_REACTION_RANGE = 32;

    }

    public static class Junk {

        @Comment("Workbenches, C.A.M.P. building and armor benches can use materials stored in your Scrapbox.")
        @Configurable
        @Configurable.Synchronized
        public boolean JUNK_STORAGE_CRAFTING = true;

        @Comment("Junk is broken down automatically when crafting needs a component it contains.")
        @Configurable
        @Configurable.Synchronized
        public boolean AUTO_SCRAP_JUNK = true;

        @Comment("Deposit All also stores crafting components such as ingots and leather, not only junk.")
        @Configurable
        @Configurable.Synchronized
        public boolean JUNK_DEPOSIT_COMPONENTS = true;

    }

    public static class Limbs {

        @Comment("Enable the crippled limb system.")
        @Configurable
        @Configurable.Synchronized
        public boolean LIMBS_ENABLED = true;

        @Comment("Players can have their limbs crippled.")
        @Configurable
        @Configurable.Synchronized
        public boolean PLAYER_LIMBS = true;

        @Comment("Mobs, including AzureLib geo mobs, can have their limbs crippled.")
        @Configurable
        @Configurable.Synchronized
        public boolean MOB_LIMBS = true;

        @Comment("Health of each limb as a fraction of the entity's max health.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.05, max = 10.0)
        public double LIMB_HEALTH_FRACTION = 0.5;

        @Comment("Multiplier applied to damage dealt to limbs.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double LIMB_DAMAGE_MULTIPLIER = 1.0;

        @Comment("Extra limb damage multiplier for limbs targeted in V.A.T.S.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double VATS_LIMB_DAMAGE_MULTIPLIER = 1.5;

        @Comment("Multiplier for fall damage dealt to both legs.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double FALL_LIMB_DAMAGE_MULTIPLIER = 1.0;

        @Comment("Multiplier for explosion damage dealt to every limb.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double EXPLOSION_LIMB_DAMAGE_MULTIPLIER = 0.5;

        @Comment("Players inside power armor cannot have their limbs damaged.")
        @Configurable
        @Configurable.Synchronized
        public boolean POWER_ARMOR_PROTECTS_LIMBS = true;

        @Comment("Seconds after a limb hit before limbs start to recover.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 3600)
        public int LIMB_REGEN_DELAY_SECONDS = 10;

        @Comment("Limb condition a player recovers per second (1.0 = full limb).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double PLAYER_LIMB_REGEN_PER_SECOND = 0.005;

        @Comment("Limb condition a mob recovers per second (1.0 = full limb).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double MOB_LIMB_REGEN_PER_SECOND = 0.01;

        @Comment(
            "Crippled player limbs recover on their own. When false they need a stimpak, doctor's bag, regeneration or sleep."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean PLAYER_CRIPPLED_SELF_HEAL = false;

        @Comment("Crippled mob limbs recover on their own over time.")
        @Configurable
        @Configurable.Synchronized
        public boolean MOB_CRIPPLED_SELF_HEAL = true;

        @Comment("Sleeping in a bed restores every limb.")
        @Configurable
        @Configurable.Synchronized
        public boolean SLEEP_HEALS_LIMBS = true;

        @Comment("Movement speed lost with one crippled leg.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double LEG_SPEED_PENALTY = 0.2;

        @Comment("Movement speed lost with both legs crippled.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double BOTH_LEGS_SPEED_PENALTY = 0.5;

        @Comment("Jump strength lost per crippled leg.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double LEG_JUMP_PENALTY = 0.25;

        @Comment("Players cannot sprint with both legs crippled.")
        @Configurable
        @Configurable.Synchronized
        public boolean BOTH_LEGS_BLOCK_SPRINT = true;

        @Comment("Attack damage lost per crippled arm.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double ARM_DAMAGE_PENALTY = 0.2;

        @Comment("Extra weapon spread per crippled arm.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 10.0)
        public double ARM_SPREAD_PENALTY = 0.5;

        @Comment("V.A.T.S. hit chance multiplier with a crippled head.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double HEAD_VATS_HIT_MULTIPLIER = 0.75;

        @Comment("Follow range a mob loses with a crippled head.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double HEAD_FOLLOW_RANGE_PENALTY = 0.5;

        @Comment("A crippled head makes the player's vision sway.")
        @Configurable
        @Configurable.Synchronized
        public boolean HEAD_CRIPPLED_NAUSEA = false;

        @Comment("Action Point regeneration multiplier with a crippled torso.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double TORSO_AP_REGEN_MULTIPLIER = 0.5;

        @Comment("Damage taken multiplier with a crippled torso.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 1.0, max = 10.0)
        public double TORSO_DAMAGE_TAKEN_MULTIPLIER = 1.1;

        @Comment("Health restored by a stimpak.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1000.0)
        public double STIMPAK_HEALTH = 8.0;

        @Comment("Limb condition a stimpak restores to every limb, including crippled ones.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double STIMPAK_LIMB_RESTORE = 0.5;

    }

    public static class ClientLimbs {

        @Comment("Show the limb condition figure on the HUD while a limb is damaged.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean LIMB_HUD = true;

        @Comment("Keep the limb condition figure on the HUD even when every limb is healthy.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean LIMB_HUD_ALWAYS = false;

    }

    public static class FeralGhouls {

        @Comment("Max health of a feral ghoul.")
        @Configurable
        @DecimalRange(min = 1.0, max = 1024.0)
        public double MAX_HEALTH = 30.0;

        @Comment("Claw damage of a feral ghoul.")
        @Configurable
        @DecimalRange(min = 0.0, max = 256.0)
        public double ATTACK_DAMAGE = 4.0;

        @Comment("Radiation applied to whatever a feral ghoul hits.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1000.0)
        public double RADS_PER_HIT = 4.0;

        @Comment("How far a feral ghoul can see a target.")
        @Configurable
        @DecimalRange(min = 4.0, max = 128.0)
        public double DETECTION_RANGE = 24.0;

        @Comment("Blocks a target must come within to wake a dormant feral ghoul.")
        @Configurable
        @Range(min = 1, max = 64)
        public int WAKE_RANGE = 6;

        @Comment("Blocks within which a screaming feral ghoul alerts the rest of its pack.")
        @Configurable
        @Range(min = 0, max = 128)
        public int ALERT_RANGE = 16;

        @Comment("Chance a naturally spawned feral ghoul lies dormant until something comes close.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double DORMANT_CHANCE = 0.3;

        @Comment("Feral ghouls can leap at targets from mid range.")
        @Configurable
        public boolean LEAP_ENABLED = true;

        @Comment("Feral ghouls ignore players who have turned into ghouls.")
        @Configurable
        public boolean IGNORE_GHOUL_PLAYERS = true;

        @Comment("Feral ghouls hunt villagers and wandering traders.")
        @Configurable
        public boolean ATTACK_VILLAGERS = true;

    }

    public static class Scorched {

        @Comment("Max health of a Scorched.")
        @Configurable
        @DecimalRange(min = 1.0, max = 1024.0)
        public double MAX_HEALTH = 40.0;

        @Comment("Unarmed melee damage of a Scorched. Held vanilla weapons add their own damage.")
        @Configurable
        @DecimalRange(min = 0.0, max = 256.0)
        public double ATTACK_DAMAGE = 3.0;

        @Comment("Multiplier applied to Atomic Dawn gun damage dealt by Scorched.")
        @Configurable
        @DecimalRange(min = 0.0, max = 10.0)
        public double GUN_DAMAGE_MULTIPLIER = 0.6;

        @Comment("Extra spread in degrees added to Scorched gunfire.")
        @Configurable
        @DecimalRange(min = 0.0, max = 45.0)
        public double AIM_SPREAD = 3.0;

        @Comment("How far an active Scorched can see a target.")
        @Configurable
        @DecimalRange(min = 4.0, max = 128.0)
        public double DETECTION_RANGE = 32.0;

        @Comment("How far a dormant Scorched can see a target.")
        @Configurable
        @DecimalRange(min = 1.0, max = 64.0)
        public double DORMANT_DETECTION_RANGE = 12.0;

        @Comment("Blocks within which Scorched share targets through the hive mind.")
        @Configurable
        @Range(min = 0, max = 128)
        public int HIVE_RANGE = 32;

        @Comment("Minimum ticks between hive-mind broadcasts from a single Scorched.")
        @Configurable
        @Range(min = 5, max = 1200)
        public int HIVE_ALERT_INTERVAL = 40;

        @Comment("Closest distance a ranged Scorched tries to keep from its target.")
        @Configurable
        @Range(min = 2, max = 64)
        public int MIN_ENGAGE_RANGE = 8;

        @Comment("Furthest distance a ranged Scorched fires from.")
        @Configurable
        @Range(min = 4, max = 96)
        public int MAX_ENGAGE_RANGE = 24;

        @Comment("Seconds a Scorched searches a lost target's last known position.")
        @Configurable
        @Range(min = 1, max = 120)
        public int INVESTIGATE_SECONDS = 15;

        @Comment("Smallest patrol radius around a Scorched's fissure site.")
        @Configurable
        @Range(min = 4, max = 128)
        public int PATROL_RADIUS_MIN = 24;

        @Comment("Largest patrol radius around a Scorched's fissure site.")
        @Configurable
        @Range(min = 4, max = 128)
        public int PATROL_RADIUS_MAX = 48;

        @Comment("Blocks from its fissure site after which a Scorched gives up a chase and returns home.")
        @Configurable
        @Range(min = 16, max = 256)
        public int LEASH_RANGE = 96;

        @Comment("Fewest seconds of inactivity before a Scorched goes dormant.")
        @Configurable
        @Range(min = 1, max = 3600)
        public int DORMANCY_MIN_SECONDS = 30;

        @Comment("Most seconds of inactivity before a Scorched goes dormant.")
        @Configurable
        @Range(min = 1, max = 3600)
        public int DORMANCY_MAX_SECONDS = 90;

        @Comment("Chance a naturally spawned Scorched starts dormant.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double DORMANT_SPAWN_CHANCE = 0.6;

        @Comment("Chance a standard Scorched or officer spawns with a gun.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double GUN_CHANCE = 0.55;

        @Comment("Chance a Scorched without a gun spawns with a vanilla melee weapon instead of fists.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double MELEE_WEAPON_CHANCE = 0.6;

        @Comment("Most naturally spawned Scorched allowed at one fissure site at a time.")
        @Configurable
        @Range(min = 0, max = 64)
        public int MAX_PER_FISSURE = 5;

        @Comment("Scorched only spawn naturally within this many blocks of their fissure's centre.")
        @Configurable
        @DecimalRange(min = 2.0, max = 64.0)
        public double FISSURE_SPAWN_RADIUS = 14.0;

        @Comment("Seconds a fissure site waits between natural Scorched spawns once it is below its limit.")
        @Configurable
        @Range(min = 0, max = 3600)
        public int FISSURE_RESPAWN_SECONDS = 90;

        @Comment("Chance a Scorched spawns as an officer.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double OFFICER_CHANCE = 0.12;

        @Comment("Holiday Scorched appear in late December and Spooky Scorched around Halloween.")
        @Configurable
        public boolean SEASONAL_VARIANTS = true;

        @Comment("Chance a Scorched spawns as the seasonal variant while its season is active.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double SEASONAL_CHANCE = 0.35;

        @Comment("Scorched hunt villagers and wandering traders.")
        @Configurable
        public boolean ATTACK_VILLAGERS = true;

    }

    public static class MisterHandy {

        @Comment("Max health of a Mister Handy.")
        @Configurable
        @DecimalRange(min = 1.0, max = 2048.0)
        public double MAX_HEALTH = 60.0;

        @Comment("Armor points of a Mister Handy.")
        @Configurable
        @DecimalRange(min = 0.0, max = 30.0)
        public double ARMOR = 10.0;

        @Comment("How far a Mister Handy can see a target. Halved while its optics are crippled.")
        @Configurable
        @DecimalRange(min = 4.0, max = 128.0)
        public double DETECTION_RANGE = 24.0;

        @Comment("Blocks from its spawn point after which a Mister Handy gives up a chase.")
        @Configurable
        @Range(min = 8, max = 256)
        public int LEASH_RANGE = 48;

        @Comment("Patrol radius around a Mister Handy's spawn point.")
        @Configurable
        @Range(min = 2, max = 64)
        public int PATROL_RADIUS = 12;

        @Comment("Seconds a Mister Handy scans the area where it lost its target.")
        @Configurable
        @Range(min = 1, max = 120)
        public int INVESTIGATE_SECONDS = 15;

        @Comment("Damage of each buzz saw hit. The saw hits four times per attack.")
        @Configurable
        @DecimalRange(min = 0.0, max = 128.0)
        public double SAW_DAMAGE = 3.5;

        @Comment("Damage of the claw strike, which also briefly slows the target.")
        @Configurable
        @DecimalRange(min = 0.0, max = 128.0)
        public double CLAW_DAMAGE = 7.0;

        @Comment("Range in blocks of the flamer.")
        @Configurable
        @DecimalRange(min = 1.0, max = 24.0)
        public double FLAMER_RANGE = 6.5;

        @Comment("Width in degrees of the flamer's cone.")
        @Configurable
        @DecimalRange(min = 5.0, max = 180.0)
        public double FLAMER_CONE_DEGREES = 40.0;

        @Comment("Damage dealt every 4 ticks to anything inside the flamer's cone.")
        @Configurable
        @DecimalRange(min = 0.0, max = 64.0)
        public double FLAMER_DAMAGE = 2.0;

        @Comment("Seconds the flamer sets targets on fire for.")
        @Configurable
        @DecimalRange(min = 0.0, max = 30.0)
        public double FLAMER_BURN_SECONDS = 4.0;

        @Comment("Ticks between flamer bursts.")
        @Configurable
        @Range(min = 20, max = 2400)
        public int FLAMER_COOLDOWN_TICKS = 100;

        @Comment("Chance a natural spawn attempt produces a Mister Handy.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double SPAWN_CHANCE = 0.5;

        @Comment("Most Mister Handies allowed within 48 blocks before natural spawning stops.")
        @Configurable
        @Range(min = 0, max = 32)
        public int MAX_NEARBY = 2;

    }

    public static class Scorchbeast {

        @Comment("Max health of a Scorchbeast.")
        @Configurable
        @DecimalRange(min = 1.0, max = 4096.0)
        public double MAX_HEALTH = 400.0;

        @Comment("Armor points of a Scorchbeast.")
        @Configurable
        @DecimalRange(min = 0.0, max = 30.0)
        public double ARMOR = 12.0;

        @Comment("How far a Scorchbeast can spot a target.")
        @Configurable
        @DecimalRange(min = 8.0, max = 160.0)
        public double DETECTION_RANGE = 64.0;

        @Comment("Blocks from its fissure after which a Scorchbeast abandons a target.")
        @Configurable
        @Range(min = 32, max = 512)
        public int LEASH_RANGE = 160;

        @Comment("Chance an unoccupied fissure site near a player gets a Scorchbeast on each spawn check.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double SPAWN_CHANCE = 0.35;

        @Comment("Seconds between Scorchbeast spawn checks around each player.")
        @Configurable
        @Range(min = 5, max = 3600)
        public int SPAWN_INTERVAL_SECONDS = 45;

        @Comment("Minutes before a fissure site can spawn another Scorchbeast after its last one appeared.")
        @Configurable
        @Range(min = 0, max = 1440)
        public int SITE_RESPAWN_MINUTES = 15;

        @Comment("Fewest seconds a Scorchbeast stays airborne before it may land to fight on the ground.")
        @Configurable
        @Range(min = 1, max = 600)
        public int MIN_AIR_SECONDS = 20;

        @Comment("Fewest seconds a Scorchbeast stays grounded before it may take off again.")
        @Configurable
        @Range(min = 1, max = 600)
        public int MIN_GROUND_SECONDS = 12;

        @Comment("Height above its target that a Scorchbeast circles at.")
        @Configurable
        @DecimalRange(min = 4.0, max = 64.0)
        public double CRUISE_ALTITUDE = 14.0;

        @Comment("Radius of the circle a Scorchbeast flies around its target.")
        @Configurable
        @DecimalRange(min = 4.0, max = 64.0)
        public double CIRCLE_RADIUS = 18.0;

        @Comment("A grounded Scorchbeast takes off when its target is further than this.")
        @Configurable
        @DecimalRange(min = 4.0, max = 96.0)
        public double TAKEOFF_DISTANCE = 22.0;

        @Comment("Furthest distance the sonic attack is used from.")
        @Configurable
        @DecimalRange(min = 8.0, max = 128.0)
        public double SONIC_RANGE = 48.0;

        @Comment("Speed in blocks per tick of each sonic projectile.")
        @Configurable
        @DecimalRange(min = 0.2, max = 5.0)
        public double SONIC_SPEED = 1.4;

        @Comment("Aim spread in blocks applied to each sonic projectile.")
        @Configurable
        @DecimalRange(min = 0.0, max = 8.0)
        public double SONIC_SPREAD = 1.0;

        @Comment("Explosion radius of each sonic projectile.")
        @Configurable
        @DecimalRange(min = 0.5, max = 12.0)
        public double SONIC_BLAST_RADIUS = 3.0;

        @Comment("Damage at the centre of each sonic projectile's explosion.")
        @Configurable
        @DecimalRange(min = 0.0, max = 256.0)
        public double SONIC_BLAST_DAMAGE = 12.0;

        @Comment("Sonic projectiles also break blocks like a mob explosion.")
        @Configurable
        public boolean SONIC_BREAKS_BLOCKS = false;

        @Comment("Ticks between sonic attacks. Values below 200 are raised to 200.")
        @Configurable
        @Range(min = 200, max = 6000)
        public int SONIC_COOLDOWN_TICKS = 200;

        @Comment("Horizontal distance within which a Scorchbeast starts a crop dusting run.")
        @Configurable
        @DecimalRange(min = 4.0, max = 128.0)
        public double DUST_TRIGGER_RANGE = 40.0;

        @Comment("Height above the target a Scorchbeast flies during a crop dusting run.")
        @Configurable
        @DecimalRange(min = 3.0, max = 32.0)
        public double DUST_ALTITUDE = 4.5;

        @Comment("Speed in blocks per tick of the crop dusting dive.")
        @Configurable
        @DecimalRange(min = 0.3, max = 4.0)
        public double DUST_DIVE_SPEED = 1.7;

        @Comment("Radius of each radioactive cloud released while crop dusting.")
        @Configurable
        @DecimalRange(min = 1.0, max = 16.0)
        public double DUST_CLOUD_RADIUS = 4.5;

        @Comment("Seconds each radioactive cloud lingers.")
        @Configurable
        @Range(min = 1, max = 120)
        public int DUST_CLOUD_SECONDS = 8;

        @Comment("Rads per second inside a crop dusting cloud. Applies through blocks.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1000.0)
        public double DUST_RADS_PER_SECOND = 6.0;

        @Comment("Ticks between crop dusting runs.")
        @Configurable
        @Range(min = 20, max = 6000)
        public int DUST_COOLDOWN_TICKS = 400;

        @Comment("Damage of the grounded wing sweep.")
        @Configurable
        @DecimalRange(min = 0.0, max = 256.0)
        public double WING_SWEEP_DAMAGE = 18.0;

        @Comment("Reach in blocks of the wing sweep beyond the Scorchbeast's body.")
        @Configurable
        @DecimalRange(min = 1.0, max = 16.0)
        public double WING_SWEEP_RANGE = 5.0;

        @Comment("Width in degrees of the wing sweep's frontal arc.")
        @Configurable
        @DecimalRange(min = 10.0, max = 360.0)
        public double WING_SWEEP_ARC = 150.0;

        @Comment("Knockback strength of the wing sweep.")
        @Configurable
        @DecimalRange(min = 0.0, max = 5.0)
        public double WING_SWEEP_KNOCKBACK = 1.8;

        @Comment("Furthest distance a grounded Scorchbeast lunges from.")
        @Configurable
        @DecimalRange(min = 8.0, max = 48.0)
        public double LUNGE_RANGE = 18.0;

        @Comment("Damage dealt when a lunge connects.")
        @Configurable
        @DecimalRange(min = 0.0, max = 256.0)
        public double LUNGE_DAMAGE = 14.0;

        @Comment("Ticks between lunges.")
        @Configurable
        @Range(min = 20, max = 6000)
        public int LUNGE_COOLDOWN_TICKS = 140;

        @Comment("Radius of the grounded shockwave scream.")
        @Configurable
        @DecimalRange(min = 1.0, max = 32.0)
        public double SHOCKWAVE_RADIUS = 8.0;

        @Comment("Damage at the centre of the shockwave scream.")
        @Configurable
        @DecimalRange(min = 0.0, max = 256.0)
        public double SHOCKWAVE_DAMAGE = 10.0;

        @Comment("Ticks between shockwave screams.")
        @Configurable
        @Range(min = 20, max = 6000)
        public int SHOCKWAVE_COOLDOWN_TICKS = 400;

        @Comment("Scorched summoned by each shockwave scream.")
        @Configurable
        @Range(min = 0, max = 8)
        public int SUMMONS_PER_SHOCKWAVE = 2;

        @Comment("Most summoned Scorched allowed near a Scorchbeast at once.")
        @Configurable
        @Range(min = 0, max = 16)
        public int MAX_ACTIVE_SUMMONS = 4;

    }

    public static class SuperMutants {

        @Comment("Max health of a Super Mutant.")
        @Configurable
        @DecimalRange(min = 1.0, max = 2048.0)
        public double MAX_HEALTH = 90.0;

        @Comment("Unarmed punch damage of a Super Mutant. Held vanilla weapons add their own damage.")
        @Configurable
        @DecimalRange(min = 0.0, max = 256.0)
        public double ATTACK_DAMAGE = 8.0;

        @Comment("Armor points of a Super Mutant.")
        @Configurable
        @DecimalRange(min = 0.0, max = 30.0)
        public double ARMOR = 8.0;

        @Comment("Knockback resistance of a Super Mutant (1.0 = immune).")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double KNOCKBACK_RESISTANCE = 0.8;

        @Comment("Multiplier applied to Atomic Dawn melee weapon damage dealt by Super Mutants.")
        @Configurable
        @DecimalRange(min = 0.0, max = 10.0)
        public double MELEE_WEAPON_DAMAGE_MULTIPLIER = 0.8;

        @Comment("Chance an unarmed or melee Super Mutant uses its overhead slam when the slam is off cooldown.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double SLAM_CHANCE = 0.3;

        @Comment("Damage multiplier of the overhead slam compared to a normal hit.")
        @Configurable
        @DecimalRange(min = 0.0, max = 10.0)
        public double SLAM_DAMAGE_MULTIPLIER = 1.6;

        @Comment("Radius in blocks of the overhead slam's area of effect.")
        @Configurable
        @DecimalRange(min = 0.5, max = 8.0)
        public double SLAM_RADIUS = 2.5;

        @Comment("Multiplier applied to Atomic Dawn gun damage dealt by Super Mutants.")
        @Configurable
        @DecimalRange(min = 0.0, max = 10.0)
        public double GUN_DAMAGE_MULTIPLIER = 0.7;

        @Comment("Extra spread in degrees added to Super Mutant gunfire.")
        @Configurable
        @DecimalRange(min = 0.0, max = 45.0)
        public double AIM_SPREAD = 4.0;

        @Comment("How far a Super Mutant can see a target.")
        @Configurable
        @DecimalRange(min = 4.0, max = 128.0)
        public double DETECTION_RANGE = 32.0;

        @Comment(
            "Blocks within which a Super Mutant hears gunshots and investigates them. Explosions carry 1.5 times further."
        )
        @Configurable
        @DecimalRange(min = 0.0, max = 128.0)
        public double HEARING_RANGE = 40.0;

        @Comment("Blocks within which a roaring or wounded Super Mutant calls nearby mutants to join the fight.")
        @Configurable
        @Range(min = 0, max = 128)
        public int PACK_RANGE = 32;

        @Comment("Closest distance a ranged Super Mutant tries to keep from its target before backing off.")
        @Configurable
        @Range(min = 2, max = 64)
        public int MIN_ENGAGE_RANGE = 6;

        @Comment("Furthest distance a ranged Super Mutant fires from.")
        @Configurable
        @Range(min = 4, max = 96)
        public int MAX_ENGAGE_RANGE = 28;

        @Comment("Seconds a Super Mutant searches a lost target's predicted position or a disturbance.")
        @Configurable
        @Range(min = 1, max = 120)
        public int INVESTIGATE_SECONDS = 20;

        @Comment("Patrol radius around a Super Mutant's camp or spawn point.")
        @Configurable
        @Range(min = 4, max = 128)
        public int PATROL_RADIUS = 20;

        @Comment("Blocks from its camp after which a Super Mutant gives up a chase and returns.")
        @Configurable
        @Range(min = 16, max = 256)
        public int LEASH_RANGE = 80;

        @Comment("Fewest seconds between combat roars and taunts.")
        @Configurable
        @Range(min = 1, max = 600)
        public int TAUNT_MIN_SECONDS = 12;

        @Comment("Most seconds between combat roars and taunts.")
        @Configurable
        @Range(min = 1, max = 600)
        public int TAUNT_MAX_SECONDS = 30;

        @Comment("Chance a Super Mutant spawns with a gun.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double RANGED_CHANCE = 0.45;

        @Comment("Chance a Super Mutant without a gun spawns with a melee weapon instead of fists.")
        @Configurable
        @DecimalRange(min = 0.0, max = 1.0)
        public double MELEE_WEAPON_CHANCE = 0.65;

        @Comment("Super Mutants hunt villagers and wandering traders.")
        @Configurable
        public boolean ATTACK_VILLAGERS = true;

    }

    public static class Thirst {

        @Comment("Enable the thirst system. Players dehydrate over time and must drink water.")
        @Configurable
        @Configurable.Synchronized
        public boolean THIRST_ENABLED = false;

        @Comment("Thirst lost per second (out of 20).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double THIRST_DRAIN_PER_SECOND = 0.025;

        @Comment("Multiplier applied to thirst loss while sprinting.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 100.0)
        public double THIRST_SPRINT_MULTIPLIER = 2.0;

        @Comment(
            "Fraction of maximum Action Points kept while fully dehydrated. Dehydrated players also cannot sprint."
        )
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 1.0)
        public double DEHYDRATED_AP_MULTIPLIER = 0.5;

        @Comment("Thirst restored by a vanilla water bottle.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double WATER_BOTTLE_THIRST = 6.0;

        @Comment("Thirst restored by Boiled Water.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double BOILED_WATER_THIRST = 6.0;

        @Comment("Thirst restored by Dirty Water.")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double DIRTY_WATER_THIRST = 5.0;

        @Comment("Thirst restored by drinkable potions other than water (item tag atomicdawn:thirst/potions).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double POTION_THIRST = 4.0;

        @Comment("Thirst restored by milk (item tag atomicdawn:thirst/milk).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double MILK_THIRST = 6.0;

        @Comment("Thirst restored by honey bottles (item tag atomicdawn:thirst/honey).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double HONEY_THIRST = 3.0;

        @Comment("Thirst restored by stews and soups (item tag atomicdawn:thirst/stews).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double STEW_THIRST = 4.0;

        @Comment("Thirst restored by soft drinks such as Nuka-Cola (item tag atomicdawn:thirst/soft_drinks).")
        @Configurable
        @Configurable.Synchronized
        @DecimalRange(min = 0.0, max = 20.0)
        public double SOFT_DRINK_THIRST = 5.0;

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

    public static class ClientRadStorm {

        @Comment("Tint the screen while exposed to a rad storm.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean RAD_STORM_OVERLAY = true;

        @Comment("Rad storm screen tint colour. The alpha channel sets the tint strength at full exposure.")
        @Configurable(side = Configurable.Side.CLIENT)
        @Gui.ColorValue(isARGB = true)
        public String RAD_STORM_OVERLAY_COLOR = "#384CC22A";

        @Comment("Multiplier applied to the rad storm tint strength. 0 hides it, 1 uses the colour's full alpha.")
        @Configurable(side = Configurable.Side.CLIENT)
        @DecimalRange(min = 0.0, max = 1.0)
        public double RAD_STORM_OVERLAY_INTENSITY = 0.5;

        @Comment("Draw the rad storm tint as an edge vignette that keeps the centre of the screen clear.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean RAD_STORM_OVERLAY_VIGNETTE = true;

        @Comment("Show falling green fallout particles during a rad storm.")
        @Configurable(side = Configurable.Side.CLIENT)
        public boolean RAD_STORM_PARTICLES = true;

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

        @Comment("Maximum number of items a player's Stash Box can hold, shared across every Stash Box.")
        @Configurable
        @Configurable.Synchronized
        @Range(min = 0, max = 1000000)
        public int STASH_LIMIT = 1200;

        @Comment(
            "Players can only open Stash Boxes inside their own C.A.M.P. When false any Stash Box opens your own stash."
        )
        @Configurable
        @Configurable.Synchronized
        public boolean STASH_REQUIRES_OWN_CAMP = false;

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

package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
public final class ADSounds {

    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(
        Registries.SOUND_EVENT,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> TEN_MM_FIRE = register("weapon.10mm_pistol.fire");

    public static final DeferredHolder<SoundEvent, SoundEvent> TEN_MM_RELOAD = register("weapon.10mm_pistol.reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> TEN_MM_DRAW = register("weapon.10mm_pistol.draw");

    public static final DeferredHolder<SoundEvent, SoundEvent> DRY_FIRE = register("weapon.dry_fire");

    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_FIRE = register(
        "weapon.hunting_rifle.fire"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_BOLT = register(
        "weapon.hunting_rifle.bolt"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTING_RIFLE_DRAW = register(
        "weapon.hunting_rifle.draw"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> COMBAT_SHOTGUN_FIRE = register(
        "weapon.combat_shotgun.fire"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> COMBAT_SHOTGUN_PUMP = register(
        "weapon.combat_shotgun.pump"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SHELL_INSERT = register("weapon.shell_insert");

    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_RIFLE_FIRE = register("weapon.laser_rifle.fire");

    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_RIFLE_RELOAD = register(
        "weapon.laser_rifle.reload"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_RIFLE_DRAW = register(
        "weapon.laser_rifle.draw"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_SLEDGE_SWING = register(
        "weapon.super_sledge.swing"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_SLEDGE_IMPACT = register(
        "weapon.super_sledge.impact"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_DRAW = register("weapon.heavy_draw");

    private ADSounds() {}

    public static final DeferredHolder<SoundEvent, SoundEvent> FAT_MAN_FIRE = register("weapon.fat_man.fire");

    public static final DeferredHolder<SoundEvent, SoundEvent> FAT_MAN_RELOAD = register("weapon.fat_man.reload");

    public static final DeferredHolder<SoundEvent, SoundEvent> FAT_MAN_DRAW = register("weapon.fat_man.draw");

    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_NUKE_EXPLODE = register("explosion.mini_nuke");

    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_CLICK = register("radiation.geiger");

    public static final DeferredHolder<SoundEvent, SoundEvent> ENTER_VATS = register("vats.ui_vats_enter");

    public static final DeferredHolder<SoundEvent, SoundEvent> VATS_SELECT = register("vats.ui_vats_select");

    public static final DeferredHolder<SoundEvent, SoundEvent> VATS_DESELECT = register("vats.ui_vats_deselect");

    public static final DeferredHolder<SoundEvent, SoundEvent> VATS_SELECT_PART = register(
        "vats.ui_vats_selecttargetpart"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> REPAIR_WEAPON = register("ui.ui_repairweapon_01");

    public static final DeferredHolder<SoundEvent, SoundEvent> HOLOTAPE_START = register("holotape.start");

    public static final DeferredHolder<SoundEvent, SoundEvent> HOLOTAPE_STOP = register("holotape.stop");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_OPEN = register("pipboy.open");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_CLOSE = register("pipboy.close");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_TAB_SWITCH = register("pipboy.tab_switch");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_SELECT = register("pipboy.select");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_BACK = register("pipboy.back");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_ERROR = register("pipboy.error");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_NOTIFICATION = register("pipboy.notification");

    public static final DeferredHolder<SoundEvent, SoundEvent> PIPBOY_BOOT_TYPING = register("pipboy.boot_typing");

    public static final DeferredHolder<SoundEvent, SoundEvent> I_DONT_WANT_SET_THE_WORLD_ON_FIRE = register(
        "music.i_dont_want_to_set_the_world_on_fire"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> FALLOUT_THEME = register("music.fallout_theme");

    public static final DeferredHolder<SoundEvent, SoundEvent> COUNTRY_ROADS = register("music.country_roads");

    public static final DeferredHolder<SoundEvent, SoundEvent> MAYBE = register("music.maybe");

    public static final DeferredHolder<SoundEvent, SoundEvent> BIG_IRON = register("music.big_iron");

    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_ARMOR_LAND = register("power_armor.land");

    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_ARMOR_STEP = register("power_armor.step");

    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_ARMOR_ENTER = register("power_armor.enter");

    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_ARMOR_EXIT = register("power_armor.exit");

    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_ARMOR_CORE = register("power_armor.core_insert");

    public static final DeferredHolder<SoundEvent, SoundEvent> FERAL_GHOUL_ATTACK = register("feral_ghoul.attack");

    public static final DeferredHolder<SoundEvent, SoundEvent> FERAL_GHOUL_DEATH = register("feral_ghoul.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> FERAL_GHOUL_HURT = register("feral_ghoul.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> FERAL_GHOUL_WALK = register("feral_ghoul.walk");

    public static final DeferredHolder<SoundEvent, SoundEvent> FERAL_GHOUL_YELL = register("feral_ghoul.yell");

    public static final DeferredHolder<SoundEvent, SoundEvent> FERAL_GHOUL_AMBIENT = register("feral_ghoul.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_AMBIENT = register("scorched.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_ALERT = register("scorched.alert");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_ATTACK = register("scorched.attack");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_HURT = register("scorched.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_DEATH = register("scorched.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_RADIO = register("scorched.radio");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_JINGLE = register("scorched.jingle");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHED_EERIE = register("scorched.eerie");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_AMBIENT = register("super_mutant.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_ROAR = register("super_mutant.roar");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_TAUNT = register("super_mutant.taunt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_ATTACK = register("super_mutant.attack");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_SLAM = register("super_mutant.slam");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_HURT = register("super_mutant.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_DEATH = register("super_mutant.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_MUTANT_STEP = register("super_mutant.step");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_AMBIENT = register("scorchbeast.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_SCREECH = register("scorchbeast.screech");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_SCREAM = register("scorchbeast.scream");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_SONIC = register("scorchbeast.sonic");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_SONIC_IMPACT = register(
        "scorchbeast.sonic_impact"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_DUST = register("scorchbeast.dust");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_FLAP = register("scorchbeast.flap");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_SWEEP = register("scorchbeast.sweep");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_SHOCKWAVE = register(
        "scorchbeast.shockwave"
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_LAND = register("scorchbeast.land");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_HURT = register("scorchbeast.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_DEATH = register("scorchbeast.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SCORCHBEAST_STEP = register("scorchbeast.step");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTER.register(name, SoundEvent::createVariableRangeEvent);
    }
}

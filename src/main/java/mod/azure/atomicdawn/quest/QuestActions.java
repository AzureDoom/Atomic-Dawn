package mod.azure.atomicdawn.quest;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampManager;
import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.progression.ModUnlocks;
import mod.azure.atomicdawn.story.StoryService;

public final class QuestActions {

    public static final Identifier OPEN_PIPBOY = AtomicDawn.id("open_pipboy");

    public static final Identifier USE_RADAWAY = AtomicDawn.id("use_radaway");

    public static final Identifier CRAFT_WEAPON = AtomicDawn.id("craft_weapon");

    public static final Identifier CRAFT_TINKER = AtomicDawn.id("craft_tinker");

    public static final Identifier REPAIR_WEAPON = AtomicDawn.id("repair_weapon");

    public static final Identifier SCRAP_WEAPON = AtomicDawn.id("scrap_weapon");

    public static final Identifier INSTALL_MOD = AtomicDawn.id("install_mod");

    public static final Identifier ALLOCATE_SPECIAL = AtomicDawn.id("allocate_special");

    public static final Identifier LEARN_PERK = AtomicDawn.id("learn_perk");

    public static final Identifier EQUIP_PERK = AtomicDawn.id("equip_perk");

    public static final Identifier BUILD_CAMP = AtomicDawn.id("build_camp");

    public static final Identifier LEARN_PLAN = AtomicDawn.id("learn_plan");

    public static final Identifier PICK_LOCK = AtomicDawn.id("pick_lock");

    public static final Identifier ABSORB_RADS = AtomicDawn.id("absorb_rads");

    public static final Identifier FIRE_WEAPON = AtomicDawn.id("fire_weapon");

    public static final Identifier USE_VATS = AtomicDawn.id("use_vats");

    public static final Identifier OWN_CAMP = AtomicDawn.id("own_camp");

    public static final Identifier KNOWN_PLANS = AtomicDawn.id("known_plans");

    public static final Identifier KNOWN_MODS = AtomicDawn.id("known_mods");

    private QuestActions() {}

    public static void trigger(Player player, Identifier action) {
        trigger(player, action, 1);
    }

    public static void trigger(Player player, Identifier action, int amount) {
        if (player instanceof ServerPlayer serverPlayer && amount > 0) {
            QuestService.onAction(serverPlayer, action, amount);
            StoryService.onAction(serverPlayer, action);
        }
    }

    static int poll(ServerPlayer player, Identifier action) {
        if (action.equals(ABSORB_RADS)) {
            return clamp(PipBoyService.stats(player).radsAbsorbed());
        }
        if (action.equals(FIRE_WEAPON)) {
            return clamp(PipBoyService.stats(player).shotsFired());
        }
        if (action.equals(USE_VATS)) {
            var stats = PipBoyService.stats(player);
            return clamp(stats.vatsHits() + stats.vatsMisses());
        }
        if (action.equals(OWN_CAMP)) {
            return CampManager.active(player.level().getServer(), player.getUUID()).isPresent() ? 1 : 0;
        }
        if (action.equals(KNOWN_PLANS)) {
            return CampPlans.of(player).known().size();
        }
        if (action.equals(KNOWN_MODS)) {
            return ModUnlocks.known(player).known().size();
        }
        return -1;
    }

    private static int clamp(long value) {
        return (int) Math.clamp(value, 0L, Integer.MAX_VALUE);
    }
}

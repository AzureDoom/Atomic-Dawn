package mod.azure.atomicdawn.camp;

import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.party.PartyService;

public final class CampPermissionService {

    private CampPermissionService() {}

    public static CampRole role(Player player, CampSlot camp) {
        var id = player.getUUID();
        if (camp.ownedBy(id)) {
            return CampRole.OWNER;
        }
        if (camp.access().trusted().containsKey(id)) {
            return CampRole.TRUSTED;
        }
        if (PartyService.party(camp.owner()).map(party -> party.contains(id)).orElse(false)) {
            return CampRole.PARTY;
        }
        return CampRole.VISITOR;
    }

    public static boolean canManage(Player player, CampSlot camp) {
        return role(player, camp) == CampRole.OWNER || adminBypass(player);
    }

    public static boolean canBuild(Player player, CampSlot camp) {
        if (adminBypass(player)) {
            return true;
        }
        return switch (role(player, camp)) {
            case OWNER -> true;
            case TRUSTED -> camp.access().trustedBuild();
            case PARTY -> camp.access().partyBuild();
            case VISITOR -> false;
        };
    }

    public static boolean canUseStations(Player player, CampSlot camp) {
        return role(player, camp) != CampRole.VISITOR || camp.access().publicStations() || adminBypass(player);
    }

    public static boolean canUseStorage(Player player, CampSlot camp) {
        if (adminBypass(player)) {
            return true;
        }
        return switch (role(player, camp)) {
            case OWNER -> true;
            case TRUSTED -> camp.access().trustedStorage();
            case PARTY, VISITOR -> false;
        };
    }

    public static boolean canUse(Player player, CampSlot camp, CampBuildable.Category category) {
        if (adminBypass(player) || role(player, camp) == CampRole.OWNER) {
            return true;
        }
        var role = role(player, camp);
        return switch (category) {
            case STORAGE -> canUseStorage(player, camp);
            case CRAFTING, DOORS -> canUseStations(player, camp);
            case LIGHTING, FURNITURE -> role != CampRole.VISITOR;
            case STRUCTURE, DECOR -> canBuild(player, camp);
        };
    }

    public static boolean adminBypass(Player player) {
        return AtomicDawn.CONFIG.common.settlement.camp.claims.CAMP_CREATIVE_BYPASS && player.isCreative();
    }
}

package mod.azure.atomicdawn.ghoul;

import net.minecraft.world.entity.player.Player;

import java.util.List;

import mod.azure.atomicdawn.perks.PerkDefinition;
import mod.azure.atomicdawn.perks.PerkEffectType;
import mod.azure.atomicdawn.special.SpecialStatResolver;

public final class GhoulPerks {

    public static final String GHOUL_TAG = "ghoul";

    public static final String HUMAN_TAG = "human";

    private GhoulPerks() {}

    public static boolean eligible(PerkDefinition definition, PlayerForm form) {
        return eligible(definition.tags(), form);
    }

    public static boolean eligible(List<String> tags, PlayerForm form) {
        if (tags.contains(GHOUL_TAG)) {
            return form == PlayerForm.GHOUL;
        }
        if (tags.contains(HUMAN_TAG)) {
            return form == PlayerForm.HUMAN;
        }
        return true;
    }

    public static float value(Player player, PerkEffectType type) {
        return SpecialStatResolver.resolve(player).extra(type);
    }
}

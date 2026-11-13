package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import mod.azure.atomicdawn.network.PipBoyStatsPayload;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialResult;

public final class PipBoyClientState {

    public enum PerkFilter {

        ALL,
        OWNED,
        EQUIPPED,
        AVAILABLE,
        LOCKED
    }

    public enum InvSort {

        NAME,
        COUNT
    }

    public static int tab;

    public static final Map<String, String> SUBTABS = new HashMap<>();

    public static final Map<String, Integer> SCROLL = new HashMap<>();

    public static int @Nullable [] pendingSpecial;

    public static int pendingRevision = -1;

    public static SpecialAttribute selectedAttribute = SpecialAttribute.STRENGTH;

    @Nullable
    public static Identifier selectedPerk;

    public static PerkFilter perkFilter = PerkFilter.ALL;

    @Nullable
    public static SpecialAttribute perkCategory;

    @Nullable
    public static Identifier selectedLegendary;

    public static int legendarySlot;

    @Nullable
    public static Identifier selectedItem;

    public static InvSort invSort = InvSort.NAME;

    @Nullable
    public static PipBoyStatsPayload stats;

    @Nullable
    public static Component status;

    public static int statusColor;

    public static long statusUntil;

    public static long lastClosedMillis = -1L;

    private PipBoyClientState() {}

    public static int scroll(String key) {
        return SCROLL.getOrDefault(key, 0);
    }

    public static void setScroll(String key, int value) {
        SCROLL.put(key, Math.max(0, value));
    }

    public static void onResult(SpecialResult result, long now) {
        status = Component.translatable(result.translationKey());
        statusColor = result.success() ? PipBoyTheme.text() : PipBoyTheme.bad();
        statusUntil = now + 80;
        if (result.success()) {
            pendingSpecial = null;
        }
    }

    public static void reset() {
        pendingSpecial = null;
        pendingRevision = -1;
        selectedPerk = null;
        selectedLegendary = null;
        selectedItem = null;
        stats = null;
        status = null;
        lastClosedMillis = -1L;
    }
}

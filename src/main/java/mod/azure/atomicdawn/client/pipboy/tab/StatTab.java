package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.Rect;

public final class StatTab implements PipBoyTab {

    public static final String STATUS = "status";

    public static final String SPECIAL = "special";

    public static final String PERKS = "perks";

    public static final String LEGENDARY = "legendary";

    public static final String EFFECTS = "effects";

    public static final String GHOUL = "ghoul";

    public static final String GENERAL = "general";

    private static final List<String> SUBTABS = List.of(STATUS, SPECIAL, PERKS, LEGENDARY, EFFECTS, GHOUL, GENERAL);

    @Override
    public String id() {
        return "stat";
    }

    @Override
    public List<String> subtabs() {
        return SUBTABS;
    }

    @Override
    public void init(PipBoyScreen screen, Rect area, String subtab) {
        switch (subtab) {
            case SPECIAL -> SpecialPage.init(screen, area);
            case PERKS -> PerksPage.init(screen, area);
            case LEGENDARY -> LegendaryPage.init(screen, area);
            case EFFECTS -> EffectsPage.init(screen, area);
            case GHOUL -> GhoulPage.init(screen, area);
            case GENERAL -> GeneralPage.init(screen, area);
            default -> {}
        }
    }

    @Override
    public void render(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        Rect area,
        String subtab,
        int mouseX,
        int mouseY
    ) {
        switch (subtab) {
            case SPECIAL -> SpecialPage.render(graphics, screen, area);
            case PERKS -> PerksPage.render(graphics, screen, area);
            case LEGENDARY -> LegendaryPage.render(graphics, screen, area);
            case EFFECTS -> EffectsPage.render(graphics, screen, area);
            case GHOUL -> GhoulPage.render(graphics, screen, area);
            case GENERAL -> GeneralPage.render(graphics, screen, area);
            default -> StatusPage.render(graphics, screen, area);
        }
    }

    @Override
    public boolean scroll(PipBoyScreen screen, String subtab, int direction) {
        return switch (subtab) {
            case PERKS -> PerksPage.scroll(direction);
            case LEGENDARY -> LegendaryPage.scroll(direction);
            case EFFECTS -> EffectsPage.scroll(direction);
            default -> false;
        };
    }

    @Override
    public void tick(PipBoyScreen screen, String subtab) {
        if (GENERAL.equals(subtab)) {
            GeneralPage.tick(screen);
        } else if (GHOUL.equals(subtab)) {
            GhoulPage.tick(screen);
        }
    }
}

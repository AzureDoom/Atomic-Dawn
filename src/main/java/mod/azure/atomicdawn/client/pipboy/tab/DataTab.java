package mod.azure.atomicdawn.client.pipboy.tab;

import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.Rect;

public final class DataTab implements PipBoyTab {

    public static final String QUESTS = "quests";

    public static final String LOCATIONS = "locations";

    public static final String BESTIARY = "bestiary";

    public static final String HOLOTAPES = "holotapes";

    public static final String NOTES = "notes";

    public static final String CAMP = "camp";

    public static final String CAMP_ACCESS = "camp_access";

    public static final String FACTIONS = "factions";

    private static final List<String> SUBTABS = List.of(
        QUESTS,
        FACTIONS,
        LOCATIONS,
        CAMP,
        CAMP_ACCESS,
        BESTIARY,
        HOLOTAPES,
        NOTES
    );

    @Override
    public String id() {
        return "data";
    }

    @Override
    public List<String> subtabs() {
        return SUBTABS;
    }

    @Override
    public void init(PipBoyScreen screen, Rect area, String subtab) {
        switch (subtab) {
            case FACTIONS -> FactionsPage.init(screen, area);
            case LOCATIONS -> LocationsPage.init(screen, area);
            case BESTIARY -> BestiaryPage.init(screen, area);
            case HOLOTAPES -> HolotapesPage.init(screen, area);
            case NOTES -> NotesPage.init(screen, area);
            case CAMP -> CampPage.init(screen, area);
            case CAMP_ACCESS -> CampAccessPage.init(screen, area);
            default -> QuestsPage.init(screen, area);
        }
    }

    @Override
    public void render(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect area,
        String subtab,
        int mouseX,
        int mouseY
    ) {
        switch (subtab) {
            case FACTIONS -> FactionsPage.render(graphics, screen, area);
            case LOCATIONS -> LocationsPage.render(graphics, screen, area);
            case BESTIARY -> BestiaryPage.render(graphics, screen, area);
            case HOLOTAPES -> HolotapesPage.render(graphics, screen, area);
            case NOTES -> NotesPage.render(graphics, screen, area);
            case CAMP -> CampPage.render(graphics, screen, area);
            case CAMP_ACCESS -> CampAccessPage.render(graphics, screen, area);
            default -> QuestsPage.render(graphics, screen, area);
        }
    }

    @Override
    public boolean scroll(PipBoyScreen screen, String subtab, int direction) {
        var area = screen.contentArea();
        return switch (subtab) {
            case FACTIONS -> FactionsPage.scroll(direction, area);
            case LOCATIONS -> LocationsPage.scroll(direction, area);
            case BESTIARY -> BestiaryPage.scroll(direction, area);
            case HOLOTAPES -> HolotapesPage.scroll(direction, area);
            case NOTES -> NotesPage.scroll(direction, area);
            case CAMP, CAMP_ACCESS -> false;
            default -> QuestsPage.scroll(direction, area);
        };
    }

    @Override
    public void tick(PipBoyScreen screen, String subtab) {
        if (HOLOTAPES.equals(subtab) && screen.ticks() % 20 == 0) {
            screen.refresh();
        }
        if (CAMP.equals(subtab)) {
            CampPage.tick(screen);
        }
        if (CAMP_ACCESS.equals(subtab)) {
            CampAccessPage.tick(screen);
        }
    }
}

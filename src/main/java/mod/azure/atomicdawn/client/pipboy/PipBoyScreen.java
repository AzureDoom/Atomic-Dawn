package mod.azure.atomicdawn.client.pipboy;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiInput;
import mod.azure.atomicdawn.client.helper.ADScreen;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.client.pipboy.tab.DataTab;
import mod.azure.atomicdawn.client.pipboy.tab.InventoryTab;
import mod.azure.atomicdawn.client.pipboy.tab.MapTab;
import mod.azure.atomicdawn.client.pipboy.tab.PipBoyTab;
import mod.azure.atomicdawn.client.pipboy.tab.RadioTab;
import mod.azure.atomicdawn.client.pipboy.tab.StatTab;
import mod.azure.atomicdawn.config.ADConfig.Client.PipBoy.Display.PipBoyLayout;
import mod.azure.atomicdawn.helper.ADWorldgenUtil;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.special.SpecialXp;
import mod.azure.atomicdawn.weapon.action.VatsActions;

public class PipBoyScreen extends ADScreen {

    public static final List<PipBoyTab> TABS = List.of(
        new StatTab(),
        new InventoryTab(),
        new DataTab(),
        new MapTab(),
        new RadioTab()
    );

    public static final int[] SELECTOR_TABS = { 0, 1, 2 };

    public static final int HOVER_NONE = -1;

    public static final int HOVER_POWER = 100;

    private static final int HEADER_H = 17;

    private static final int SUBTAB_H = 12;

    private static final int FOOTER_H = 14;

    private static final int SHUTDOWN_MS = 260;

    private static final int CLASSIC_CLOCK_H = 22;

    private static final int CLASSIC_STATS_H = 32;

    private static final int CLASSIC_TAB_H = 14;

    private static final int CARD_GAP = 14;

    private static final int CLASSIC_STATUS_H = 11;

    private record SubtabHit(
        String id,
        Rect rect,
        Component label,
        int labelX
    ) {}

    private record StatusLine(
        Component text,
        int color
    ) {}

    private PipBoyHousing.Layout layout = PipBoyHousing.layout(0, 0, true);

    private Rect screenArea = new Rect(0, 0, 0, 0);

    private Rect inner = new Rect(0, 0, 0, 0);

    private Rect content = new Rect(0, 0, 0, 0);

    private PipBoyLayout style = PipBoyLayout.FALLOUT_76;

    private Rect sidebar = new Rect(0, 0, 0, 0);

    private boolean verticalSubtabs;

    private final List<PipBoyButton> buttons = new ArrayList<>();

    private static final long TYPING_REPEAT_MS = 560L;

    private final List<AbstractWidget> vanillaWidgets = new ArrayList<>();

    private final List<AbstractWidget> hiddenUnderTooltip = new ArrayList<>();

    private final List<Rect> tabHits = new ArrayList<>();

    private final List<SubtabHit> subtabHits = new ArrayList<>();

    @Nullable
    private PipBoyBoot boot;

    @Nullable
    private SoundInstance typingSound;

    private int typingSegment = -1;

    private long typingSoundAt;

    private boolean contentReady;

    private long closingSince = -1L;

    private boolean poweredOff;

    @Nullable
    private Object[] lastSynced;

    private long ticks;

    private int lastMouseX;

    private int lastMouseY;

    public PipBoyScreen() {
        super(Component.translatable("pipboy.atomicdawn.title"));
        if (!AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_REMEMBER_TAB) {
            PipBoyClientState.tab = 0;
        }
        PipBoyClientState.tab = Math.floorMod(PipBoyClientState.tab, TABS.size());
        var config = AtomicDawn.CONFIG.client.pipboy.behavior;
        if (config.PIPBOY_BOOT_SEQUENCE) {
            var now = millis();
            var last = PipBoyClientState.lastClosedMillis;
            var warm = last >= 0 && now - last < config.PIPBOY_BOOT_COOLDOWN_SECONDS * 1000L;
            boot = new PipBoyBoot(warm ? PipBoyBoot.Mode.QUICK : PipBoyBoot.Mode.FULL, now);
        }
    }

    private static long millis() {
        return System.nanoTime() / 1_000_000L;
    }

    public PipBoyTab currentTab() {
        return TABS.get(PipBoyClientState.tab);
    }

    public String currentSubtab() {
        var tab = currentTab();
        var subtab = PipBoyClientState.SUBTABS.get(tab.id());
        if (subtab == null || !tab.subtabs().contains(subtab)) {
            subtab = tab.subtabs().getFirst();
            PipBoyClientState.SUBTABS.put(tab.id(), subtab);
        }
        return subtab;
    }

    public Rect contentArea() {
        return content;
    }

    public int lastMouseX() {
        return lastMouseX;
    }

    public int lastMouseY() {
        return lastMouseY;
    }

    public int tabIndex(String id) {
        for (var i = 0; i < TABS.size(); i++) {
            if (TABS.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public void open(String tab, String subtab) {
        var index = tabIndex(tab);
        if (index < 0) {
            return;
        }
        PipBoyClientState.SUBTABS.put(tab, subtab);
        if (index == PipBoyClientState.tab) {
            playSound(ADSounds.PIPBOY_TAB_SWITCH.get());
            rebuildWidgets();
        } else {
            switchTab(index);
        }
    }

    public long ticks() {
        return ticks;
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> T widget(T widget) {
        if (widget instanceof AbstractWidget abstractWidget) {
            vanillaWidgets.add(abstractWidget);
        }
        return addRenderableWidget(widget);
    }

    public PipBoyButton button(Component label, int x, int y, int w, int h, boolean active, Runnable action) {
        return button(label, x, y, w, h, active, null, action);
    }

    public PipBoyButton button(
        Component label,
        int x,
        int y,
        int w,
        int h,
        boolean active,
        @Nullable Component tooltip,
        Runnable action
    ) {
        return add(
            new PipBoyButton(label, new Rect(x, y, w, h), active, false, PipBoyButton.Style.BUTTON, tooltip, action)
        );
    }

    public PipBoyButton row(Component label, int x, int y, int w, int h, boolean selected, Runnable action) {
        return row(label, x, y, w, h, selected, null, action);
    }

    public PipBoyButton row(
        Component label,
        int x,
        int y,
        int w,
        int h,
        boolean selected,
        @Nullable Component tooltip,
        Runnable action
    ) {
        return add(
            new PipBoyButton(label, new Rect(x, y, w, h), !selected, selected, PipBoyButton.Style.ROW, tooltip, action)
        );
    }

    public PipBoyButton option(Component label, int x, int y, int w, int h, boolean selected, Runnable action) {
        return option(label, x, y, w, h, selected, null, action);
    }

    public PipBoyButton option(
        Component label,
        int x,
        int y,
        int w,
        int h,
        boolean selected,
        @Nullable Component tooltip,
        Runnable action
    ) {
        return add(
            new PipBoyButton(
                label,
                new Rect(x, y, w, h),
                !selected,
                selected,
                PipBoyButton.Style.OPTION,
                tooltip,
                action
            )
        );
    }

    private PipBoyButton add(PipBoyButton button) {
        buttons.add(button);
        return button;
    }

    public void refresh() {
        rebuildWidgets();
    }

    public void playSound(SoundEvent sound) {
        if (minecraft != null && minecraft.player != null && AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_SOUNDS) {
            minecraft.player.playSound(sound, 1F, 1F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean booting() {
        return boot != null && boot.blocking(millis());
    }

    @Override
    protected void init() {
        buttons.clear();
        vanillaWidgets.clear();
        hiddenUnderTooltip.clear();
        tabHits.clear();
        subtabHits.clear();

        layout = PipBoyHousing.layout(width, height, AtomicDawn.CONFIG.client.pipboy.display.PIPBOY_FULLSCREEN);
        screenArea = layout.screen();
        inner = screenArea.inset(layout.fullscreen() ? 6 : 5);
        style = PipBoyModel.layout();
        verticalSubtabs = false;

        var tab = currentTab();
        var subtab = currentSubtab();
        switch (style) {
            case NEW_VEGAS, FALLOUT_3 -> initFallout3000(tab);
            case FALLOUT_1_2 -> initClassic(tab);
            default -> initModern(tab, subtab);
        }

        contentReady = !booting();
        if (contentReady) {
            tab.init(this, content, subtab);
        }
    }

    private void initModern(PipBoyTab tab, String subtab) {
        var slot = inner.width() / (float) TABS.size();
        for (var i = 0; i < TABS.size(); i++) {
            var x0 = inner.x() + Math.round(slot * i);
            var x1 = inner.x() + Math.round(slot * (i + 1));
            tabHits.add(new Rect(x0, inner.y(), x1 - x0, HEADER_H));
        }
        var subtabs = tab.subtabs();
        var subY = inner.y() + HEADER_H + 1;
        if (subtabs.size() > 1) {
            if (style == PipBoyLayout.FALLOUT_4) {
                layoutCarousel(tab, subtabs, subtab, subY);
            } else {
                layoutSubtabs(tab, subtabs, inner.x() + 2, inner.width() - 4, subY);
            }
        }
        var top = subY + (subtabs.size() > 1 ? SUBTAB_H + 5 : 3);
        content = new Rect(inner.x() + 2, top, inner.width() - 4, inner.bottom() - FOOTER_H - 3 - top);
    }

    private void initFallout3000(PipBoyTab tab) {
        if (layout.fullscreen()) {
            var x = inner.x() + 2;
            for (var candidate : TABS) {
                var w = font.width(candidate.label()) + 12;
                tabHits.add(new Rect(x, inner.y(), w, HEADER_H));
                x += w;
            }
        }
        var subtabs = tab.subtabs();
        var rowY = inner.bottom() - SUBTAB_H - 2;
        if (subtabs.size() > 1) {
            layoutSubtabs(tab, subtabs, inner.x() + 8, inner.width() - 16, rowY);
        } else {
            subtabHits.add(
                new SubtabHit(
                    subtabs.getFirst(),
                    new Rect(inner.x() + 8, rowY, inner.width() - 16, SUBTAB_H),
                    tab.label(),
                    inner.x() + inner.width() / 2
                )
            );
        }
        var top = inner.y() + HEADER_H + 3;
        content = new Rect(inner.x() + 6, top, inner.width() - 12, rowY - 6 - top);
    }

    private void initClassic(PipBoyTab tab) {
        if (!layout.fullscreen()) {
            initClassicHoused(tab);
            return;
        }
        var sideW = Math.clamp(inner.width() / 4, 72, 112);
        sidebar = new Rect(inner.x() + 2, inner.y() + 2, sideW, inner.height() - 4);
        var y = sidebar.y() + CLASSIC_CLOCK_H + 6;
        for (var i = 0; i < TABS.size(); i++) {
            tabHits.add(new Rect(sidebar.x() + 3, y, sidebar.width() - 6, CLASSIC_TAB_H));
            y += CLASSIC_TAB_H + 3;
        }
        var contentX = sidebar.right() + 8;
        var contentW = inner.right() - 5 - contentX;
        var contentTop = inner.y() + 5;
        var subtabs = tab.subtabs();
        if (subtabs.size() > 1) {
            var statsTop = sidebar.bottom() - CLASSIC_STATS_H;
            var rowH = Math.min(12, (statsTop - y - 4) / subtabs.size());
            if (rowH >= 10) {
                verticalSubtabs = true;
                y += 2;
                for (var subtab : subtabs) {
                    subtabHits.add(
                        new SubtabHit(
                            subtab,
                            new Rect(sidebar.x() + 6, y, sidebar.width() - 9, rowH),
                            tab.subtabLabel(subtab),
                            sidebar.x() + 14
                        )
                    );
                    y += rowH;
                }
            } else {
                layoutSubtabs(tab, subtabs, contentX, contentW, contentTop);
                contentTop += SUBTAB_H + 5;
            }
        }
        content = new Rect(contentX, contentTop, contentW, inner.bottom() - 5 - contentTop);
    }

    private void initClassicHoused(PipBoyTab tab) {
        sidebar = new Rect(0, 0, 0, 0);
        var titleW = font.width(tab.label()) + 14;
        var top = inner.y() + 3;
        var subtabs = tab.subtabs();
        if (subtabs.size() > 1) {
            layoutSubtabs(tab, subtabs, inner.x() + 2 + titleW, inner.width() - 4 - titleW, top);
        }
        top += SUBTAB_H + 6;
        content = new Rect(inner.x() + 7, top, inner.width() - 14, inner.bottom() - 7 - CLASSIC_STATUS_H - top);
    }

    private void layoutSubtabs(PipBoyTab tab, List<String> subtabs, int left, int width, int y) {
        var slot = width / (float) subtabs.size();
        for (var i = 0; i < subtabs.size(); i++) {
            var x0 = left + Math.round(slot * i);
            var x1 = left + Math.round(slot * (i + 1));
            var label = tab.subtabLabel(subtabs.get(i));
            subtabHits.add(
                new SubtabHit(
                    subtabs.get(i),
                    new Rect(x0, y, x1 - x0, SUBTAB_H),
                    label,
                    x0 + (x1 - x0) / 2
                )
            );
        }
    }

    private void layoutCarousel(PipBoyTab tab, List<String> subtabs, String selected, int y) {
        var index = Math.max(0, subtabs.indexOf(selected));
        var left = inner.x() + 2;
        var right = inner.right() - 2;
        var widths = new int[subtabs.size()];
        for (var i = 0; i < subtabs.size(); i++) {
            widths[i] = Math.min(font.width(tab.subtabLabel(subtabs.get(i))), right - left - 6);
        }
        var start = inner.x() + (inner.width() - widths[index]) / 2;
        addCard(tab, subtabs.get(index), start, widths[index], y);
        var cursor = start - CARD_GAP;
        for (var i = index - 1; i >= 0; i--) {
            var x0 = cursor - widths[i];
            if (x0 < left) {
                break;
            }
            addCard(tab, subtabs.get(i), x0, widths[i], y);
            cursor = x0 - CARD_GAP;
        }
        cursor = start + widths[index] + CARD_GAP;
        for (var i = index + 1; i < subtabs.size(); i++) {
            if (cursor + widths[i] > right) {
                break;
            }
            addCard(tab, subtabs.get(i), cursor, widths[i], y);
            cursor += widths[i] + CARD_GAP;
        }
    }

    private void addCard(PipBoyTab tab, String subtab, int x, int w, int y) {
        subtabHits.add(new SubtabHit(subtab, new Rect(x - 4, y, w + 8, SUBTAB_H), tab.subtabLabel(subtab), x + w / 2));
    }

    public void switchTab(int index) {
        var next = Math.floorMod(index, TABS.size());
        if (next != PipBoyClientState.tab) {
            PipBoyClientState.tab = next;
            playSound(ADSounds.PIPBOY_TAB_SWITCH.get());
            rebuildWidgets();
        }
    }

    public void switchSubtab(String subtab) {
        PipBoyClientState.SUBTABS.put(currentTab().id(), subtab);
        playSound(ADSounds.PIPBOY_TAB_SWITCH.get());
        rebuildWidgets();
    }

    private void pressSelector(int index) {
        var selectors = layout.selectorTabs();
        if (index < 0 || index >= selectors.length) {
            return;
        }
        var target = selectors[index];
        var cycles = selectors.length < TABS.size() && index == selectors.length - 1;
        if (cycles && PipBoyClientState.tab >= target) {
            switchTab(PipBoyClientState.tab + 1 >= TABS.size() ? target : PipBoyClientState.tab + 1);
        } else {
            switchTab(target);
        }
    }

    private int selectorLit() {
        var selectors = layout.selectorTabs();
        var tab = PipBoyClientState.tab;
        for (var i = selectors.length - 1; i >= 0; i--) {
            if (tab >= selectors[i]) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (minecraft == null || minecraft.player == null) {
            finishClose();
            return;
        }
        var now = millis();
        if (closingSince >= 0 && now - closingSince >= SHUTDOWN_MS) {
            finishClose();
            return;
        }
        tickBootTyping(now);
        if (boot != null) {
            if (!contentReady && !boot.blocking(now)) {
                rebuildWidgets();
            }
            if (boot.finished(now)) {
                boot = null;
            }
        }
        var player = minecraft.player;
        Object[] synced = {
            player.getData(ADAttachments.SPECIAL_PROGRESS.get()),
            player.getData(ADAttachments.PIPBOY.get()),
            player.getData(ADAttachments.LOCATIONS.get()),
            player.getData(ADAttachments.QUESTS.get()),
            player.getData(ADAttachments.HOLOTAPES.get()),
            player.getData(ADAttachments.BESTIARY.get())
        };
        if (lastSynced == null) {
            lastSynced = synced;
        } else if (changed(lastSynced, synced)) {
            lastSynced = synced;
            if (contentReady) {
                rebuildWidgets();
            }
        }
        if (contentReady) {
            currentTab().tick(this, currentSubtab());
        }
    }

    private static boolean changed(Object[] before, Object[] after) {
        for (var i = 0; i < before.length; i++) {
            if (before[i] != after[i]) {
                return true;
            }
        }
        return false;
    }

    private void tickBootTyping(long now) {
        var segment = boot == null ? -1 : boot.typingSegment(now);
        if (segment < 0) {
            typingSegment = -1;
            stopTypingSound();
            return;
        }
        if (segment != typingSegment || now - typingSoundAt >= TYPING_REPEAT_MS) {
            typingSegment = segment;
            typingSoundAt = now;
            if (minecraft != null && AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_SOUNDS) {
                stopTypingSound();
                var pitch = 0.95F + (float) Math.random() * 0.1F;
                typingSound = SimpleSoundInstance.forUI(ADSounds.PIPBOY_BOOT_TYPING.get(), pitch, 0.55F);
                minecraft.getSoundManager().play(typingSound);
            }
        }
    }

    private void stopTypingSound() {
        if (typingSound != null && minecraft != null) {
            minecraft.getSoundManager().stop(typingSound);
        }
        typingSound = null;
    }

    private void skipBoot() {
        if (boot != null) {
            boot.skip(millis());
            stopTypingSound();
            if (!contentReady) {
                rebuildWidgets();
            }
        }
    }

    @Override
    public boolean keyPressed(ADGuiInput.Key event) {
        if (closingSince >= 0) {
            finishClose();
            return true;
        }
        if (booting()) {
            if (event.key() == InputConstants.KEY_ESCAPE || ADGuiInput.matches(ADKeyMappings.PIPBOY, event)) {
                finishClose();
            } else {
                skipBoot();
            }
            return true;
        }
        if (getFocused() instanceof EditBox box && box.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
            return super.keyPressed(event);
        }
        if (ADGuiInput.matches(ADKeyMappings.PIPBOY, event)) {
            onClose();
            return true;
        }
        if (currentTab().keyPressed(this, currentSubtab(), event.key())) {
            return true;
        }
        switch (event.key()) {
            case InputConstants.KEY_LEFT -> {
                switchTab(PipBoyClientState.tab - 1);
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                switchTab(PipBoyClientState.tab + 1);
                return true;
            }
            case InputConstants.KEY_TAB -> {
                var subtabs = currentTab().subtabs();
                if (subtabs.size() > 1) {
                    switchSubtab(subtabs.get((subtabs.indexOf(currentSubtab()) + 1) % subtabs.size()));
                }
                return true;
            }
            case InputConstants.KEY_BACKSPACE -> {
                if (!currentTab().back(this, currentSubtab())) {
                    var subtabs = currentTab().subtabs();
                    var index = subtabs.indexOf(currentSubtab());
                    if (index > 0) {
                        switchSubtab(subtabs.get(index - 1));
                    }
                }
                playSound(ADSounds.PIPBOY_BACK.get());
                return true;
            }
            default -> {}
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(ADGuiInput.Mouse event, boolean doubleClick) {
        if (closingSince >= 0) {
            return true;
        }
        if (booting()) {
            skipBoot();
            return true;
        }
        var mx = event.x();
        var my = event.y();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (layout.power() != null && layout.power().inset(-2).contains(mx, my)) {
                poweredOff = true;
                onClose();
                return true;
            }
            for (var i = 0; i < layout.selectorLabels().length; i++) {
                if (layout.selectorLabels()[i].contains(mx, my) || layout.selectorLamps()[i].contains(mx, my)) {
                    pressSelector(i);
                    return true;
                }
            }
            for (var i = 0; i < tabHits.size(); i++) {
                if (tabHits.get(i).contains(mx, my)) {
                    if (i != PipBoyClientState.tab) {
                        switchTab(i);
                    }
                    return true;
                }
            }
            for (var hit : subtabHits) {
                if (hit.rect().contains(mx, my)) {
                    if (!hit.id().equals(currentSubtab())) {
                        switchSubtab(hit.id());
                    }
                    return true;
                }
            }
            for (var i = buttons.size() - 1; i >= 0; i--) {
                var button = buttons.get(i);
                if (button.hovered(mx, my)) {
                    if (button.active()) {
                        setFocused(null);
                        playSound(ADSounds.PIPBOY_SELECT.get());
                        button.press();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!contentReady) {
            return true;
        }
        if (
            content.contains(mouseX, mouseY) && scrollY != 0 && currentTab().scroll(
                this,
                currentSubtab(),
                scrollY > 0 ? -1 : 1
            )
        ) {
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (closingSince >= 0) {
            return;
        }
        stopTypingSound();
        playSound(ADSounds.PIPBOY_CLOSE.get());
        if (!AtomicDawn.CONFIG.client.pipboy.behavior.PIPBOY_BOOT_SEQUENCE || booting()) {
            finishClose();
            return;
        }
        closingSince = millis();
        setFocused(null);
    }

    private void finishClose() {
        if (closingSince == Long.MAX_VALUE) {
            return;
        }
        closingSince = Long.MAX_VALUE;
        super.onClose();
    }

    @Override
    public void removed() {
        stopTypingSound();
        PipBoyClientState.lastClosedMillis = poweredOff ? -1L : millis();
        super.removed();
    }

    @Override
    protected void drawBackground(ADGuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        var now = millis();
        var closing = closingSince >= 0 && closingSince != Long.MAX_VALUE
            ? Math.clamp((now - closingSince) / (float) SHUTDOWN_MS, 0F, 1F)
            : 0F;
        var power = boot == null ? 1F : boot.power(now);
        var tubes = boot == null ? 1F : boot.tubes(now);
        if (closing > 0) {
            tubes *= 1F - closing;
            power = closing > 0.6F ? 0F : power;
        }

        graphics.fill(0, 0, width, height, 0x60000000);
        PipBoyHousing.render(
            graphics,
            this,
            layout,
            new PipBoyHousing.State(power, tubes, selectorLit(), hoveredControl(mouseX, mouseY), now)
        );

        PipBoyDraw.fill(graphics, screenArea, PipBoyTheme.glass());
        PipBoyDraw.fill(graphics, screenArea.inset(screenArea.height() / 6), PipBoyTheme.phosphor(0.035F, 0x60));

        if (booting()) {
            boot.render(graphics, this, screenArea, now);
        } else {
            switch (style) {
                case NEW_VEGAS, FALLOUT_3 -> renderFallout3000(graphics, mouseX, mouseY);
                case FALLOUT_1_2 -> renderClassic(graphics, mouseX, mouseY);
                default -> renderHeader(graphics, mouseX, mouseY);
            }
            currentTab().render(graphics, this, content, currentSubtab(), mouseX, mouseY);
            for (var button : buttons) {
                button.render(graphics, this, mouseX, mouseY);
            }
            if (style == PipBoyLayout.FALLOUT_76) {
                renderFooter(graphics);
            } else if (style == PipBoyLayout.FALLOUT_4) {
                renderPanelFooter(graphics);
            }
            if (boot != null) {
                boot.renderReveal(graphics, screenArea, now);
            }
        }

        crtEffects(graphics, now);

        if (boot != null) {
            PipBoyDraw.crtMask(graphics, screenArea, boot.crtOpen(now));
        }
        if (closing > 0) {
            PipBoyDraw.crtMask(graphics, screenArea, 1F - closing);
        }
        if (!booting() && closing <= 0) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private int hoveredControl(int mouseX, int mouseY) {
        if (layout.power() != null && layout.power().inset(-2).contains(mouseX, mouseY)) {
            return HOVER_POWER;
        }
        for (var i = 0; i < layout.selectorLabels().length; i++) {
            if (
                layout.selectorLabels()[i].contains(mouseX, mouseY) || layout.selectorLamps()[i].contains(
                    mouseX,
                    mouseY
                )
            ) {
                return i;
            }
        }
        return HOVER_NONE;
    }

    private void renderHeader(ADGuiGraphics graphics, int mouseX, int mouseY) {
        var lineY = inner.y() + HEADER_H - 3;
        var color = PipBoyTheme.text();
        graphics.fill(inner.x(), lineY, inner.right(), lineY + 1, color);
        graphics.fill(inner.x(), lineY + 1, inner.right(), lineY + 2, PipBoyTheme.phosphor(0.3F, 0x60));
        for (var i = 0; i < TABS.size(); i++) {
            var hit = tabHits.get(i);
            var label = TABS.get(i).label();
            var w = font.width(label);
            var x = hit.x() + (hit.width() - w) / 2;
            var y = inner.y() + 3;
            var selected = i == PipBoyClientState.tab;
            if (selected) {

                var left = x - 5;
                var right = x + w + 4;
                var top = y - 3;
                graphics.fill(left, top, left + 1, lineY, color);
                graphics.fill(right, top, right + 1, lineY, color);
                graphics.fill(left, top, left + 4, top + 1, color);
                graphics.fill(right - 3, top, right + 1, top + 1, color);
            } else if (hit.contains(mouseX, mouseY)) {
                graphics.fill(x - 3, y - 2, x + w + 3, lineY - 1, PipBoyTheme.highlight());
            }
            glowText(graphics, label, x, y, color);
        }
        for (var hit : subtabHits) {
            var selected = hit.id().equals(currentSubtab());
            var hovered = hit.rect().contains(mouseX, mouseY);
            var c = selected || hovered ? PipBoyTheme.text() : PipBoyTheme.disabled();
            var available = Math.max(1, hit.rect().width() - 4);
            var labelWidth = font.width(hit.label());
            if (labelWidth <= available) {
                text(graphics, hit.label(), hit.labelX() - labelWidth / 2, hit.rect().y() + 2, c);
            } else {
                fitted(graphics, hit.label(), hit.rect().x() + 2, hit.rect().y() + 2, available, c);
            }
        }
    }

    private void glowText(ADGuiGraphics graphics, Component text, int x, int y, int color) {
        var halo = PipBoyTheme.phosphor(0.5F, 0x38);
        graphics.text(font, text, x - 1, y, halo, false);
        graphics.text(font, text, x + 1, y, halo, false);
        graphics.text(font, text, x, y + 1, halo, false);
        graphics.text(font, text, x, y, color, false);
    }

    private void renderFooter(ADGuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        var player = minecraft.player;
        var y = inner.bottom() - FOOTER_H + 3;
        graphics.fill(inner.x(), y - 3, inner.right(), y - 2, PipBoyTheme.border());

        var hp = Component.translatable(
            "pipboy.atomicdawn.footer.hp",
            Math.round(player.getHealth()),
            Math.round(player.getMaxHealth())
        );
        text(graphics, hp, inner.x() + 2, y + 1, PipBoyTheme.text());

        var progress = player.getData(ADAttachments.SPECIAL_PROGRESS.get());
        var level = Component.translatable("pipboy.atomicdawn.footer.level", progress.level());
        var levelW = font.width(level);
        var barW = Math.clamp(inner.width() / 7, 24, 70);
        var cx = inner.x() + inner.width() / 2;
        var lx = cx - (levelW + 6 + barW) / 2;
        text(graphics, level, lx, y + 1, PipBoyTheme.text());
        var bx = lx + levelW + 6;
        PipBoyDraw.outline(graphics, new Rect(bx, y + 2, barW, 6), PipBoyTheme.border());
        var fill = Math.round((barW - 2) * Math.clamp(SpecialXp.fraction(progress.xp(), progress.level()), 0F, 1F));
        graphics.fill(bx + 1, y + 3, bx + 1 + fill, y + 7, PipBoyTheme.text());

        var right = footerStatus(apLine());
        var maxW = inner.right() - (bx + barW + 8) - 2;
        var w = Math.min(font.width(right.text()), maxW);
        fitted(graphics, right.text(), inner.right() - w - 2, y + 1, maxW, right.color());
    }

    private Component apLine() {
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return Component.empty();
        }
        var ap = player.getData(ADAttachments.ACTION_POINTS.get());
        return Component.translatable(
            "pipboy.atomicdawn.footer.ap",
            Math.round(ap.points()),
            Math.round(VatsActions.maxActionPoints(player))
        );
    }

    private Component hpLine() {
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return Component.empty();
        }
        return Component.translatable(
            "pipboy.atomicdawn.footer.hp",
            Math.round(player.getHealth()),
            Math.round(player.getMaxHealth())
        );
    }

    private int playerLevel() {
        var player = minecraft == null ? null : minecraft.player;
        return player == null ? 1 : player.getData(ADAttachments.SPECIAL_PROGRESS.get()).level();
    }

    private float levelFraction() {
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return 0F;
        }
        var progress = player.getData(ADAttachments.SPECIAL_PROGRESS.get());
        return Math.clamp(SpecialXp.fraction(progress.xp(), progress.level()), 0F, 1F);
    }

    private StatusLine footerStatus(Component fallback) {
        var status = PipBoyClientState.status;
        if (
            status != null && minecraft != null && minecraft.level != null
                && minecraft.level.getGameTime() < PipBoyClientState.statusUntil
        ) {
            return new StatusLine(status, PipBoyClientState.statusColor);
        }
        return new StatusLine(fallback, PipBoyTheme.text());
    }

    private void renderPanelFooter(ADGuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        var y = inner.bottom() - FOOTER_H + 1;
        var h = FOOTER_H - 2;
        var gap = 3;
        var total = inner.width() - 4;
        var sideW = total * 27 / 100;
        var midW = total - sideW * 2 - gap * 2;
        var left = new Rect(inner.x() + 2, y, sideW, h);
        var middle = new Rect(left.right() + gap, y, midW, h);
        var right = new Rect(middle.right() + gap, y, sideW, h);
        var fill = PipBoyTheme.selection();
        PipBoyDraw.fill(graphics, left, fill);
        PipBoyDraw.fill(graphics, middle, fill);
        PipBoyDraw.fill(graphics, right, fill);
        var ty = y + (h - 8) / 2 + 1;
        fitted(graphics, hpLine(), left.x() + 4, ty, left.width() - 8, PipBoyTheme.text());

        var level = Component.translatable("pipboy.atomicdawn.footer.level", playerLevel());
        var levelW = Math.min(font.width(level), middle.width() / 2);
        fitted(graphics, level, middle.x() + 4, ty, levelW, PipBoyTheme.text());
        var barX = middle.x() + 4 + levelW + 6;
        var barW = middle.right() - 4 - barX;
        if (barW > 6) {
            var barY = y + h / 2 - 2;
            PipBoyDraw.outline(graphics, new Rect(barX, barY, barW, 5), PipBoyTheme.text());
            var filled = Math.round((barW - 2) * levelFraction());
            graphics.fill(barX + 1, barY + 1, barX + 1 + filled, barY + 4, PipBoyTheme.text());
        }

        var status = footerStatus(apLine());
        var maxW = right.width() - 8;
        var w = Math.min(font.width(status.text()), maxW);
        fitted(graphics, status.text(), right.right() - 4 - w, ty, maxW, status.color());
    }

    private void renderFallout3000(ADGuiGraphics graphics, int mouseX, int mouseY) {
        var color = PipBoyTheme.text();
        var frameTop = inner.y() + 5;
        var left = inner.x() + 2;
        var right = inner.right() - 3;
        var titleEnd = left + 4;
        if (layout.fullscreen()) {
            var lineY = inner.y() + HEADER_H - 3;
            graphics.fill(inner.x(), lineY, inner.right(), lineY + 1, color);
            for (var i = 0; i < TABS.size(); i++) {
                var hit = tabHits.get(i);
                titleEnd = Math.max(titleEnd, hit.right());
                var label = TABS.get(i).label();
                var w = font.width(label);
                var x = hit.x() + 6;
                var y = inner.y() + 3;
                if (i == PipBoyClientState.tab) {
                    bracketUp(graphics, x - 4, x + w + 3, y - 3, lineY, color);
                    glowText(graphics, label, x, y, color);
                } else {
                    if (hit.contains(mouseX, mouseY)) {
                        graphics.fill(x - 3, y - 2, x + w + 3, lineY - 1, PipBoyTheme.highlight());
                    }
                    text(graphics, label, x, y, PipBoyTheme.dim());
                }
            }
        } else {
            var title = currentTab().label();
            var titleX = left + 10;
            graphics.fill(left, frameTop, titleX - 3, frameTop + 1, color);
            glowText(graphics, title, titleX, inner.y() + 2, color);
            titleEnd = titleX + font.width(title) + 4;
        }
        if (style == PipBoyLayout.NEW_VEGAS) {
            renderVegasHeader(graphics, titleEnd, frameTop, color);
        } else {
            renderStatSegments(graphics, titleEnd, color);
        }

        if (subtabHits.isEmpty()) {
            return;
        }
        var rowY = subtabHits.getFirst().rect().y();
        var rowMid = rowY + SUBTAB_H / 2;
        if (!layout.fullscreen()) {
            graphics.fill(left, frameTop, left + 1, rowMid + 1, color);
            graphics.fill(right, frameTop + 8, right + 1, rowMid + 1, color);
        }
        graphics.fill(left, rowMid, right + 1, rowMid + 1, color);
        for (var hit : subtabHits) {
            var selected = hit.id().equals(currentSubtab());
            var hovered = hit.rect().contains(mouseX, mouseY);
            var available = Math.max(1, hit.rect().width() - 12);
            var labelWidth = Math.min(available, font.width(hit.label()));
            var x = hit.labelX() - labelWidth / 2;
            var box = new Rect(x - 5, rowY - 1, labelWidth + 10, SUBTAB_H + 1);
            PipBoyDraw.fill(graphics, box, PipBoyTheme.glass());
            if (selected) {
                PipBoyDraw.outline(graphics, box, color);
            } else if (hovered) {
                PipBoyDraw.fill(graphics, box.inset(1), PipBoyTheme.highlight());
            }
            fitted(graphics, hit.label(), x, rowY + 2, available, selected || hovered ? color : PipBoyTheme.dim());
        }
    }

    private void renderStatSegments(ADGuiGraphics graphics, int start, int color) {
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return;
        }
        var x0 = start + 6;
        var available = inner.right() - 3 - x0;
        if (available < 30) {
            return;
        }
        var status = PipBoyClientState.status;
        if (
            status != null && minecraft.level != null && minecraft.level.getGameTime() < PipBoyClientState.statusUntil
        ) {
            segment(graphics, x0, inner.right() - 3, Component.empty(), status, PipBoyClientState.statusColor, color);
            return;
        }
        var ap = player.getData(ADAttachments.ACTION_POINTS.get());
        Component[] labels = {
            Component.translatable("pipboy.atomicdawn.header.level"),
            Component.translatable("pipboy.atomicdawn.header.hp"),
            Component.translatable("pipboy.atomicdawn.header.ap") };
        Component[] values = {
            Component.literal(String.valueOf(playerLevel())),
            Component.literal(Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth())),
            Component.literal(Math.round(ap.points()) + "/" + Math.round(VatsActions.maxActionPoints(player))) };
        var segmentW = available / labels.length;
        for (var i = 0; i < labels.length; i++) {
            var sx = x0 + segmentW * i;
            var ex = i == labels.length - 1 ? inner.right() - 3 : sx + segmentW - 4;
            segment(graphics, sx, ex, labels[i], values[i], color, color);
        }
    }

    private void segment(
        ADGuiGraphics graphics,
        int x0,
        int x1,
        Component label,
        Component value,
        int valueColor,
        int color
    ) {
        var top = inner.y() + 1;
        graphics.fill(x0, top, x1, top + 1, color);
        graphics.fill(x0, top, x0 + 1, top + HEADER_H - 5, color);
        var y = inner.y() + 4;
        var labelW = font.width(label);
        if (labelW > 0) {
            text(graphics, label, x0 + 4, y, color);
        }
        var room = Math.max(1, x1 - x0 - 10 - labelW);
        var valueW = Math.min(room, font.width(value));
        fitted(graphics, value, x1 - 3 - valueW, y, room, valueColor);
    }

    private void renderVegasHeader(ADGuiGraphics graphics, int start, int frameTop, int color) {
        var dayNumber = minecraft == null || minecraft.level == null
            ? 1L
            : minecraft.level.getGameTime() / 24000L + 1L;
        var now = LocalTime.now();
        var date = Component.translatable(
            "pipboy.atomicdawn.header.date",
            String.format(Locale.ROOT, "%04d", Math.min(9999L, dayNumber)),
            String.format(Locale.ROOT, "%02d:%02d", now.getHour(), now.getMinute())
        );
        var dateW = font.width(date) + 12;
        var dateX = Math.max(start + 20, inner.right() - 3 - dateW);
        segment(graphics, dateX, inner.right() - 3, Component.empty(), date, color, color);
        var status = footerStatus(location());
        var locationEnd = dateX - 6;
        var room = locationEnd - (start + 8);
        graphics.fill(start, frameTop, dateX - 3, frameTop + 1, color);
        if (room > 10) {
            var w = Math.min(room, font.width(status.text()));
            fitted(graphics, status.text(), locationEnd - w, inner.y() + 7, room, status.color());
        }
    }

    private Component location() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return Component.empty();
        }
        return ADWorldgenUtil.biome(minecraft.level, minecraft.player.blockPosition())
            .unwrapKey()
            .<Component>map(
                key -> Component.translatable(
                    "biome." + key.identifier().getNamespace() + "." + key.identifier().getPath()
                )
            )
            .orElse(Component.empty());
    }

    private void bracketUp(ADGuiGraphics graphics, int left, int right, int top, int bottom, int color) {
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right, top, right + 1, bottom, color);
        graphics.fill(left, top, left + 4, top + 1, color);
        graphics.fill(right - 3, top, right + 1, top + 1, color);
    }

    private void renderClassicHoused(ADGuiGraphics graphics, int mouseX, int mouseY) {
        var bright = PipBoyTheme.text();
        var title = currentTab().label();
        glowText(graphics, title, inner.x() + 4, inner.y() + 5, bright);
        for (var hit : subtabHits) {
            var selected = hit.id().equals(currentSubtab());
            var hovered = hit.rect().contains(mouseX, mouseY);
            var available = Math.max(1, hit.rect().width() - 4);
            var labelWidth = Math.min(available, font.width(hit.label()));
            var x = hit.labelX() - labelWidth / 2;
            if (selected) {
                PipBoyDraw.outline(
                    graphics,
                    new Rect(x - 4, hit.rect().y(), labelWidth + 8, hit.rect().height()),
                    bright
                );
            } else if (hovered) {
                graphics.fill(
                    x - 3,
                    hit.rect().y() + 1,
                    x + labelWidth + 3,
                    hit.rect().bottom() - 1,
                    PipBoyTheme.highlight()
                );
            }
            fitted(
                graphics,
                hit.label(),
                x,
                hit.rect().y() + 2,
                available,
                selected || hovered ? bright : PipBoyTheme.disabled()
            );
        }
        var divider = inner.y() + 3 + SUBTAB_H + 2;
        graphics.fill(inner.x() + 2, divider, inner.right() - 2, divider + 1, PipBoyTheme.border());
        var frame = content.inset(-4);
        PipBoyDraw.outline(graphics, frame, PipBoyTheme.border());
        PipBoyDraw.outline(graphics, frame.inset(2), PipBoyTheme.faint());
        var status = footerStatus(
            Component.empty()
                .append(hpLine())
                .append("   ")
                .append(apLine())
                .append("   ")
                .append(Component.translatable("pipboy.atomicdawn.footer.level", playerLevel()))
        );
        var y = inner.bottom() - CLASSIC_STATUS_H;
        fitted(graphics, status.text(), inner.x() + 6, y, inner.width() - 12, status.color());
    }

    private void renderClassic(ADGuiGraphics graphics, int mouseX, int mouseY) {
        if (!layout.fullscreen()) {
            renderClassicHoused(graphics, mouseX, mouseY);
            return;
        }
        var bright = PipBoyTheme.text();
        PipBoyDraw.fill(graphics, sidebar, PipBoyTheme.panel());
        PipBoyDraw.outline(graphics, sidebar, PipBoyTheme.border());

        var clock = new Rect(sidebar.x() + 3, sidebar.y() + 3, sidebar.width() - 6, CLASSIC_CLOCK_H);
        PipBoyDraw.fill(graphics, clock, PipBoyTheme.ink());
        PipBoyDraw.outline(graphics, clock, PipBoyTheme.dim());
        var dayNumber = minecraft == null || minecraft.level == null
            ? 1L
            : minecraft.level.getGameTime() / 24000L + 1L;
        var day = Component.translatable(
            "pipboy.atomicdawn.lcd.day",
            String.format(Locale.ROOT, "%04d", Math.min(9999L, dayNumber))
        );
        var now = LocalTime.now();
        var time = Component.literal(String.format(Locale.ROOT, "%02d:%02d", now.getHour(), now.getMinute()));
        fitted(graphics, day, clock.x() + 3, clock.y() + 2, clock.width() - 6, bright);
        fitted(graphics, time, clock.x() + 3, clock.y() + 12, clock.width() - 6, bright);

        for (var i = 0; i < TABS.size(); i++) {
            var hit = tabHits.get(i);
            var selected = i == PipBoyClientState.tab;
            var hovered = hit.contains(mouseX, mouseY);
            PipBoyDraw.fill(
                graphics,
                hit,
                selected ? PipBoyTheme.selection() : hovered ? PipBoyTheme.highlight() : PipBoyTheme.ink()
            );
            PipBoyDraw.outline(graphics, hit, selected ? bright : PipBoyTheme.border());
            graphics.fill(hit.x() + 1, hit.bottom() - 1, hit.right() - 1, hit.bottom(), PipBoyTheme.faint());
            var lamp = new Rect(hit.x() + 3, hit.y() + (hit.height() - 5) / 2, 5, 5);
            if (selected) {
                PipBoyDraw.fill(graphics, lamp, bright);
            } else {
                PipBoyDraw.outline(graphics, lamp, PipBoyTheme.dim());
            }
            fitted(
                graphics,
                TABS.get(i).label(),
                hit.x() + 11,
                hit.y() + (hit.height() - 8) / 2 + 1,
                hit.width() - 14,
                selected || hovered ? bright : PipBoyTheme.dim()
            );
        }

        for (var hit : subtabHits) {
            var selected = hit.id().equals(currentSubtab());
            var hovered = hit.rect().contains(mouseX, mouseY);
            var c = selected || hovered ? bright : PipBoyTheme.disabled();
            if (verticalSubtabs) {
                var y = hit.rect().y() + (hit.rect().height() - 8) / 2 + 1;
                if (selected) {
                    text(graphics, Component.literal(">"), hit.rect().x(), y, bright);
                }
                fitted(graphics, hit.label(), hit.rect().x() + 8, y, hit.rect().width() - 8, c);
            } else {
                var available = Math.max(1, hit.rect().width() - 4);
                var labelWidth = Math.min(available, font.width(hit.label()));
                var x = hit.labelX() - labelWidth / 2;
                if (selected) {
                    graphics.fill(x - 2, hit.rect().bottom() - 1, x + labelWidth + 2, hit.rect().bottom(), bright);
                }
                fitted(graphics, hit.label(), x, hit.rect().y() + 2, available, c);
            }
        }

        var statsY = sidebar.bottom() - CLASSIC_STATS_H + 2;
        graphics.fill(sidebar.x() + 3, statsY - 2, sidebar.right() - 3, statsY - 1, PipBoyTheme.border());
        var statsW = sidebar.width() - 8;
        fitted(graphics, hpLine(), sidebar.x() + 4, statsY, statsW, bright);
        var status = footerStatus(apLine());
        fitted(graphics, status.text(), sidebar.x() + 4, statsY + 10, statsW, status.color());
        fitted(
            graphics,
            Component.translatable("pipboy.atomicdawn.footer.level", playerLevel()),
            sidebar.x() + 4,
            statsY + 20,
            statsW,
            bright
        );

        var frame = content.inset(-4);
        PipBoyDraw.outline(graphics, frame, PipBoyTheme.border());
        PipBoyDraw.outline(graphics, frame.inset(2), PipBoyTheme.faint());
    }

    private void crtEffects(ADGuiGraphics graphics, long now) {
        var scan = PipBoyTheme.scanline();
        if ((scan >>> 24) > 0) {
            var offset = (int) (ticks % 3);
            for (var y = screenArea.y() + offset; y < screenArea.bottom(); y += 3) {
                graphics.fill(screenArea.x(), y, screenArea.right(), y + 1, scan);
            }

            var period = 7000L;
            var p = (now % period) / (float) period;
            var bandH = Math.max(8, screenArea.height() / 12);
            var by = screenArea.y() - bandH + Math.round((screenArea.height() + bandH) * p);
            var alpha = Math.round(0x10 * (float) AtomicDawn.CONFIG.client.pipboy.display.PIPBOY_SCANLINES / 0.35F);
            for (var i = 0; i < bandH; i++) {
                var y = by + i;
                if (y < screenArea.y() || y >= screenArea.bottom()) {
                    continue;
                }
                var a = Math.round(alpha * (float) Math.sin(Math.PI * i / bandH));
                graphics.fill(screenArea.x(), y, screenArea.right(), y + 1, PipBoyTheme.phosphor(1F, a));
            }
        }
        PipBoyDraw.vignette(graphics, screenArea, Math.max(6, screenArea.height() / 14), 0x90);

        graphics.fill(screenArea.x() + 2, screenArea.y() + 1, screenArea.right() - 2, screenArea.y() + 2, 0x14FFFFFF);
    }

    private void renderTooltip(ADGuiGraphics graphics, int mouseX, int mouseY) {
        for (var widget : hiddenUnderTooltip) {
            widget.visible = true;
        }
        hiddenUnderTooltip.clear();
        PipBoyButton hovered = null;
        for (var i = buttons.size() - 1; i >= 0; i--) {
            if (buttons.get(i).hovered(mouseX, mouseY)) {
                hovered = buttons.get(i);
                break;
            }
        }
        if (hovered == null || hovered.tooltip() == null) {
            return;
        }
        var maxW = Math.min(200, width / 2);
        var lines = font.split(hovered.tooltip(), maxW);
        var w = 0;
        for (var line : lines) {
            w = Math.max(w, font.width(line));
        }
        var h = lines.size() * 10;
        var x = Math.min(mouseX + 10, width - w - 8);
        var y = Math.min(mouseY - 4, height - h - 8);
        var box = new Rect(x - 4, y - 4, w + 8, h + 6);

        for (var widget : vanillaWidgets) {
            if (
                widget.visible && widget.getX() < box.right() && widget.getX() + widget.getWidth() > box.x()
                    && widget.getY() < box.bottom() && widget.getY() + widget.getHeight() > box.y()
            ) {
                widget.visible = false;
                hiddenUnderTooltip.add(widget);
            }
        }
        PipBoyDraw.fill(graphics, box, PipBoyTheme.withAlpha(PipBoyTheme.glass(), 0xF2));
        PipBoyDraw.outline(graphics, box, PipBoyTheme.text());
        for (var line : lines) {
            graphics.text(font, line, x, y, PipBoyTheme.text(), false);
            y += 10;
        }
    }

    public void text(ADGuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, false);
    }

    public void fitted(ADGuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        var w = font.width(text);
        if (w <= maxWidth || w == 0) {
            graphics.text(font, text, x, y, color, false);
            return;
        }
        var scale = Math.max(0.01F, (float) maxWidth / w);
        graphics.push();
        graphics.translate(x, y + (1 - scale) * 4);
        graphics.scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pop();
    }

    public int wrapped(
        ADGuiGraphics graphics,
        Component text,
        int x,
        int y,
        int maxWidth,
        int bottom,
        int color
    ) {
        for (FormattedCharSequence line : font.split(text, maxWidth)) {
            if (y + 9 > bottom) {
                break;
            }
            graphics.text(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    public void bar(ADGuiGraphics graphics, int x, int y, int w, int h, float fraction, int color) {
        graphics.fill(x, y, x + w, y + h, PipBoyTheme.faint());
        graphics.fill(x, y, x + Math.round(w * Math.clamp(fraction, 0F, 1F)), y + h, color);
    }

    public Font font() {
        return font;
    }
}

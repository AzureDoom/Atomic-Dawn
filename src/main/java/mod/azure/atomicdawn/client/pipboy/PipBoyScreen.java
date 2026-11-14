package mod.azure.atomicdawn.client.pipboy;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.client.pipboy.tab.DataTab;
import mod.azure.atomicdawn.client.pipboy.tab.InventoryTab;
import mod.azure.atomicdawn.client.pipboy.tab.MapTab;
import mod.azure.atomicdawn.client.pipboy.tab.PipBoyTab;
import mod.azure.atomicdawn.client.pipboy.tab.RadioTab;
import mod.azure.atomicdawn.client.pipboy.tab.StatTab;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.special.SpecialXp;
import mod.azure.atomicdawn.weapon.action.VatsActions;

public class PipBoyScreen extends Screen {

    public static final List<PipBoyTab> TABS = List.of(
        new StatTab(),
        new InventoryTab(),
        new DataTab(),
        new MapTab(),
        new RadioTab()
    );

    public static final int[] SELECTOR_TABS = { 0, 1, 2 };

    public static final int HOVER_NONE = -1;

    public static final int HOVER_POWER = 3;

    private static final int HEADER_H = 17;

    private static final int SUBTAB_H = 12;

    private static final int FOOTER_H = 14;

    private static final int SHUTDOWN_MS = 260;

    private record SubtabHit(
        String id,
        Rect rect,
        Component label,
        int labelX
    ) {}

    private PipBoyHousing.Layout layout = PipBoyHousing.layout(0, 0, true);

    private Rect screenArea = new Rect(0, 0, 0, 0);

    private Rect inner = new Rect(0, 0, 0, 0);

    private Rect content = new Rect(0, 0, 0, 0);

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
        if (!AtomicDawn.CONFIG.clientPipboy.PIPBOY_REMEMBER_TAB) {
            PipBoyClientState.tab = 0;
        }
        PipBoyClientState.tab = Math.floorMod(PipBoyClientState.tab, TABS.size());
        var config = AtomicDawn.CONFIG.clientPipboy;
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
        if (minecraft != null && minecraft.player != null && AtomicDawn.CONFIG.clientPipboy.PIPBOY_SOUNDS) {
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

        layout = PipBoyHousing.layout(width, height, AtomicDawn.CONFIG.clientPipboy.PIPBOY_FULLSCREEN);
        screenArea = layout.screen();
        inner = screenArea.inset(layout.fullscreen() ? 6 : 5);

        var slot = inner.width() / (float) TABS.size();
        for (var i = 0; i < TABS.size(); i++) {
            var x0 = inner.x() + Math.round(slot * i);
            var x1 = inner.x() + Math.round(slot * (i + 1));
            tabHits.add(new Rect(x0, inner.y(), x1 - x0, HEADER_H));
        }

        var tab = currentTab();
        var subtab = currentSubtab();
        var subtabs = tab.subtabs();
        var subY = inner.y() + HEADER_H + 1;
        if (subtabs.size() > 1) {
            layoutSubtabs(tab, subtabs, subtab, subY);
        }
        var top = subY + (subtabs.size() > 1 ? SUBTAB_H + 5 : 3);
        content = new Rect(inner.x() + 2, top, inner.width() - 4, inner.bottom() - FOOTER_H - 3 - top);

        contentReady = !booting();
        if (contentReady) {
            tab.init(this, content, subtab);
        }
    }

    private void layoutSubtabs(PipBoyTab tab, List<String> subtabs, String selected, int y) {
        var slot = (inner.width() - 4) / (float) subtabs.size();
        for (var i = 0; i < subtabs.size(); i++) {
            var x0 = inner.x() + 2 + Math.round(slot * i);
            var x1 = inner.x() + 2 + Math.round(slot * (i + 1));
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
        var target = SELECTOR_TABS[index];
        if (index == SELECTOR_TABS.length - 1 && PipBoyClientState.tab >= target) {
            switchTab(PipBoyClientState.tab + 1 >= TABS.size() ? target : PipBoyClientState.tab + 1);
        } else {
            switchTab(target);
        }
    }

    private int selectorLit() {
        var tab = PipBoyClientState.tab;
        for (var i = SELECTOR_TABS.length - 1; i >= 0; i--) {
            if (tab >= SELECTOR_TABS[i]) {
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
            if (minecraft != null && AtomicDawn.CONFIG.clientPipboy.PIPBOY_SOUNDS) {
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
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (closingSince >= 0) {
            finishClose();
            return true;
        }
        if (booting()) {
            if (event.key() == InputConstants.KEY_ESCAPE || ADKeyMappings.PIPBOY.matches(event)) {
                finishClose();
            } else {
                skipBoot();
            }
            return true;
        }
        if (getFocused() instanceof EditBox box && box.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
            return super.keyPressed(event);
        }
        if (ADKeyMappings.PIPBOY.matches(event)) {
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
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
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
        if (!AtomicDawn.CONFIG.clientPipboy.PIPBOY_BOOT_SEQUENCE || booting()) {
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
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
            renderHeader(graphics, mouseX, mouseY);
            currentTab().render(graphics, this, content, currentSubtab(), mouseX, mouseY);
            for (var button : buttons) {
                button.render(graphics, this, mouseX, mouseY);
            }
            renderFooter(graphics);
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

    private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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

    private void glowText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        var halo = PipBoyTheme.phosphor(0.5F, 0x38);
        graphics.text(font, text, x - 1, y, halo, false);
        graphics.text(font, text, x + 1, y, halo, false);
        graphics.text(font, text, x, y + 1, halo, false);
        graphics.text(font, text, x, y, color, false);
    }

    private void renderFooter(GuiGraphicsExtractor graphics) {
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

        var status = PipBoyClientState.status;
        Component right;
        int rightColor;
        if (
            status != null && minecraft.level != null && minecraft.level.getGameTime() < PipBoyClientState.statusUntil
        ) {
            right = status;
            rightColor = PipBoyClientState.statusColor;
        } else {
            var ap = player.getData(ADAttachments.ACTION_POINTS.get());
            right = Component.translatable(
                "pipboy.atomicdawn.footer.ap",
                Math.round(ap.points()),
                Math.round(VatsActions.maxActionPoints(player))
            );
            rightColor = PipBoyTheme.text();
        }
        var maxW = inner.right() - (bx + barW + 8) - 2;
        var w = Math.min(font.width(right), maxW);
        fitted(graphics, right, inner.right() - w - 2, y + 1, maxW, rightColor);
    }

    private void crtEffects(GuiGraphicsExtractor graphics, long now) {
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
            var alpha = Math.round(0x10 * (float) AtomicDawn.CONFIG.clientPipboy.PIPBOY_SCANLINES / 0.35F);
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

    private void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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

    public void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, false);
    }

    public void fitted(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth, int color) {
        var w = font.width(text);
        if (w <= maxWidth || w == 0) {
            graphics.text(font, text, x, y, color, false);
            return;
        }
        var scale = Math.max(0.01F, (float) maxWidth / w);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + (1 - scale) * 4);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    public int wrapped(
        GuiGraphicsExtractor graphics,
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

    public void bar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, float fraction, int color) {
        graphics.fill(x, y, x + w, y + h, PipBoyTheme.faint());
        graphics.fill(x, y, x + Math.round(w * Math.clamp(fraction, 0F, 1F)), y + h, color);
    }

    public Font font() {
        return font;
    }
}

package mod.azure.atomicdawn.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.client.party.PartyClient;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;

public final class PartyHud implements GuiLayer {

    private static final int WIDTH = 92;

    private static final int ROW = 16;

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        if (!AtomicDawn.CONFIG.clientParty.PARTY_HUD || !PartyClient.inParty()) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        if (player == null || minecraft.level == null || hudHidden || minecraft.gui.screen() instanceof PipBoyScreen) {
            return;
        }
        var font = minecraft.font;
        var x = 4;
        var y = AtomicDawn.CONFIG.clientParty.PARTY_HUD_Y;
        var members = PartyClient.members();
        var height = 12 + members.size() * ROW;
        if (!HudLayout.push(graphics, HudElement.PARTY, x - 2, y - 2, WIDTH + 4, height + 2)) {
            return;
        }
        graphics.fill(x - 2, y - 2, x + WIDTH + 2, y + height, PipBoyTheme.withAlpha(PipBoyTheme.background(), 0x90));
        graphics.fill(x - 2, y - 2, x - 1, y + height, PipBoyTheme.border());
        graphics.text(
            font,
            Component.translatable("hud.atomicdawn.party", members.size(), PartyClient.maxSize()),
            x,
            y,
            PipBoyTheme.text(),
            false
        );
        y += 11;
        var dimension = minecraft.level.dimension().identifier();
        for (var member : members) {
            var self = member.id().equals(player.getUUID());
            var name = (member.leader() ? "★ " : "") + member.name();
            var color = !member.online() ? PipBoyTheme.faint() : self ? PipBoyTheme.text() : PipBoyTheme.dim();
            var label = font.plainSubstrByWidth(name, WIDTH - 30);
            graphics.text(font, Component.literal(label), x, y, color, false);

            Component right;
            if (!member.online()) {
                right = Component.translatable("hud.atomicdawn.party.offline");
            } else if (self) {
                right = Component.empty();
            } else if (!member.dimension().equals(dimension)) {
                right = Component.literal("—");
            } else {
                var distance = Math.sqrt(member.pos().distToCenterSqr(player.position()));
                right = Component.literal(Math.round(distance) + "m");
            }
            graphics.text(font, right, x + WIDTH - font.width(right), y, PipBoyTheme.dim(), false);

            if (member.online()) {
                var fraction = member.maxHealth() <= 0F ? 0F : Math.clamp(member.health() / member.maxHealth(), 0F, 1F);
                var barColor = fraction > 0.5F
                    ? PipBoyTheme.text()
                    : fraction > 0.25F ? PipBoyTheme.warn() : PipBoyTheme.bad();
                graphics.fill(x, y + 10, x + WIDTH, y + 12, PipBoyTheme.faint());
                graphics.fill(x, y + 10, x + Math.round(WIDTH * fraction), y + 12, barColor);
            }
            y += ROW;
        }
        HudLayout.pop(graphics);
    }
}

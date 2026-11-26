package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.mutation.Mutation;
import mod.azure.atomicdawn.mutation.MutationService;

final class EffectsPage {

    private static final String SCROLL = "effects";

    private static final int ROW = 22;

    private EffectsPage() {}

    static void init(PipBoyScreen screen, Rect area) {}

    static boolean scroll(int direction) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        var max = Math.max(0, player.getActiveEffects().size() + mutations(player).size() - 1);
        PipBoyClientState.setScroll(SCROLL, Math.min(max, PipBoyClientState.scroll(SCROLL) + direction));
        return true;
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var effects = new ArrayList<>(player.getActiveEffects());
        var mutations = mutations(player);
        if (effects.isEmpty() && mutations.isEmpty()) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.effects.none"),
                area.x() + 4,
                area.y() + 4,
                PipBoyTheme.dim()
            );
            return;
        }
        effects.sort(
            Comparator.comparing((MobEffectInstance e) -> !e.getEffect().value().isBeneficial())
                .thenComparing(e -> e.getEffect().value().getDisplayName().getString())
        );
        var total = mutations.size() + effects.size();
        var first = Math.min(PipBoyClientState.scroll(SCROLL), total - 1);
        var y = area.y() + 2;
        var now = player.level().getGameTime();
        var data = MutationService.get(player);
        for (var i = first; i < total && y + ROW <= area.bottom(); i++) {
            if (i < mutations.size()) {
                var mutation = mutations.get(i);
                screen.fitted(
                    graphics,
                    Component.literal("[M] ").append(Component.translatable(mutation.translationKey())),
                    area.x() + 4,
                    y,
                    area.width() - 80,
                    PipBoyTheme.text()
                );
                if (data.suppressed(mutation, now)) {
                    var left = Component.translatable(
                        "pipboy.atomicdawn.effects.serum",
                        duration((int) (data.suppressedUntil().get(mutation) - now))
                    );
                    screen.text(graphics, left, area.right() - 4 - screen.font().width(left), y, PipBoyTheme.dim());
                }
                screen.fitted(
                    graphics,
                    Component.translatable(mutation.translationKey() + ".desc"),
                    area.x() + 16,
                    y + 10,
                    area.width() - 20,
                    PipBoyTheme.dim()
                );
                y += ROW;
                continue;
            }
            var effect = effects.get(i - mutations.size());
            var beneficial = effect.getEffect().value().isBeneficial();
            var name = Component.literal(beneficial ? "[+] " : "[-] ")
                .append(effect.getEffect().value().getDisplayName());
            if (effect.getAmplifier() > 0) {
                name.append(" " + roman(effect.getAmplifier() + 1));
            }
            screen.fitted(
                graphics,
                name,
                area.x() + 4,
                y,
                area.width() - 80,
                beneficial ? PipBoyTheme.text() : PipBoyTheme.warn()
            );
            var duration = effect.isInfiniteDuration()
                ? Component.literal("∞")
                : Component.literal(duration(effect.getDuration()));
            screen.text(graphics, duration, area.right() - 4 - screen.font().width(duration), y, PipBoyTheme.dim());
            screen.text(
                graphics,
                Component.translatable(
                    beneficial
                        ? "pipboy.atomicdawn.effects.positive"
                        : "pipboy.atomicdawn.effects.negative"
                ),
                area.x() + 16,
                y + 10,
                PipBoyTheme.dim()
            );
            y += ROW;
        }
    }

    private static List<Mutation> mutations(Player player) {
        return MutationService.enabled()
            ? MutationService.get(player).all().stream().sorted().toList()
            : List.of();
    }

    private static String duration(int ticks) {
        var seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static String roman(int value) {
        return switch (value) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(value);
        };
    }
}

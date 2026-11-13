package mod.azure.atomicdawn.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.Consumer;

import mod.azure.atomicdawn.ghoul.GhoulTreatmentService;

public class GhoulTreatmentItem extends Item {

    private final GhoulTreatmentService.Kind kind;

    public GhoulTreatmentItem(Properties properties, GhoulTreatmentService.Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public static Properties properties(Properties properties) {
        return properties.stacksTo(4).rarity(Rarity.RARE);
    }

    public GhoulTreatmentService.Kind kind() {
        return kind;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            GhoulTreatmentService.prompt(serverPlayer, hand, kind);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull TooltipDisplay display,
        @NotNull Consumer<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.accept(
            Component.translatable("item.atomicdawn.ghoul_treatment." + kind.name().toLowerCase(Locale.ROOT) + ".desc")
                .withStyle(ChatFormatting.GRAY)
        );
    }
}

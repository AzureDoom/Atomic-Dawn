package mod.azure.atomicdawn.client.powerarmor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class PowerArmorFirstPerson {

    private static boolean handPass;

    @Nullable
    private static PowerArmorEntity armor;

    private static final ModelPart[] SUPPRESSED = new ModelPart[4];

    private PowerArmorFirstPerson() {}

    public static boolean active() {
        return handPass && armor != null;
    }

    @Nullable
    public static PowerArmorEntity armor() {
        return handPass ? armor : null;
    }

    public static boolean suppresses(ModelPart part) {
        if (!handPass || armor == null) {
            return false;
        }
        for (var suppressed : SUPPRESSED) {
            if (suppressed == part) {
                return true;
            }
        }
        return false;
    }

    public static PowerArmorSlot slot(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? PowerArmorSlot.LEFT_ARM : PowerArmorSlot.RIGHT_ARM;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (handPass) {
            return;
        }
        handPass = true;
        armor = PowerArmorClient.current();
        Arrays.fill(SUPPRESSED, null);
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (armor == null || player == null) {
            return;
        }
        if (
            minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof LivingEntityRenderer<?, ?, ?> renderer
                && renderer.getModel() instanceof PlayerModel model
        ) {
            SUPPRESSED[0] = model.rightArm;
            SUPPRESSED[1] = model.leftArm;
            SUPPRESSED[2] = model.rightSleeve;
            SUPPRESSED[3] = model.leftSleeve;
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        end();
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre event) {
        end();
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Render.Pre event) {
        end();
    }

    private static void end() {
        handPass = false;
        armor = null;
        Arrays.fill(SUPPRESSED, null);
    }
}

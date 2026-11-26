package mod.azure.atomicdawn.client.ghoul;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADPlayerRenderUtil;
import mod.azure.atomicdawn.ghoul.GhoulAppearance;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.ghoul.GhoulState;
import mod.azure.atomicdawn.network.GhoulActionPayload;
import mod.azure.atomicdawn.network.GhoulPromptPayload;
import mod.azure.atomicdawn.registry.ADAttachments;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class GhoulClient {

    public static final Identifier GLOW_TEXTURE = AtomicDawn.id("textures/entity/ghoul/ghoul_glow.png");

    private static final Identifier[] SKINS = new Identifier[GhoulState.APPEARANCE_VARIANTS];

    private static final DustParticleOptions GLOW_DUST = new DustParticleOptions(0x7CFF4A, 0.9F);

    private static final double PARTICLE_RANGE_SQR = 48.0 * 48.0;

    static {
        for (var i = 0; i < SKINS.length; i++) {
            SKINS[i] = AtomicDawn.id("textures/entity/ghoul/ghoul_skin_" + i + ".png");
        }
    }

    private GhoulClient() {}

    public static Identifier skin(int variant) {
        return SKINS[GhoulState.clampVariant(variant)];
    }

    public static void onPrompt(GhoulPromptPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.gui.setScreen(new GhoulConfirmScreen(payload.kind()));
    }

    public static @Nullable GhoulAppearance appearance(int entityId) {
        var minecraft = Minecraft.getInstance();
        if (
            minecraft.level == null || !GhoulService.enabled()
                || !AtomicDawn.CONFIG.client.effects.ghoul.GHOUL_COSMETICS
        ) {
            return null;
        }
        if (!(minecraft.level.getEntity(entityId) instanceof Player player)) {
            return null;
        }
        return appearance(player);
    }

    private static @Nullable GhoulAppearance appearance(Player player) {
        var appearance = player.getData(ADAttachments.GHOUL_APPEARANCE.get());
        if (player == Minecraft.getInstance().player) {
            var state = GhoulService.state(player);
            var ghoul = GhoulService.isGhoul(player);
            appearance = new GhoulAppearance(
                ghoul,
                state.appearanceVariant(),
                state.cosmeticVisible(),
                ghoul && GhoulService.glowFraction(player) >= 0.995F
            );
        }
        return appearance.rendersCosmetics() ? appearance : null;
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        ADPlayerRenderUtil.addPlayerLayers(event, GhoulPlayerRenderLayer::new);
    }

    public static void send(GhoulActionPayload.Action action, int value) {
        ClientPacketDistributor.sendToServer(new GhoulActionPayload(action, value));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var camera = minecraft.player;
        if (level == null || camera == null || minecraft.isPaused() || !GhoulService.enabled()) {
            return;
        }
        if (!AtomicDawn.CONFIG.client.effects.ghoul.GHOUL_COSMETICS) {
            return;
        }
        for (var player : level.players()) {
            if (player.isInvisible() || player.distanceToSqr(camera) > PARTICLE_RANGE_SQR) {
                continue;
            }
            var appearance = appearance(player);
            if (appearance == null || !appearance.fullGlow()) {
                continue;
            }
            var random = player.getRandom();
            if (random.nextFloat() > 0.35F) {
                continue;
            }
            if (player == camera && minecraft.options.getCameraType().isFirstPerson()) {
                continue;
            }
            level.addParticle(
                GLOW_DUST,
                player.getRandomX(0.6),
                player.getRandomY(),
                player.getRandomZ(0.6),
                0.0,
                0.02,
                0.0
            );
        }
    }
}

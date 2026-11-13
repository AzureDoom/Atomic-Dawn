package mod.azure.atomicdawn.client.pipboy.radio;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.pipboy.holotape.Holotape;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADSounds;

@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class HolotapePlayer {

    private static final int START_GRACE_TICKS = 100;

    private static final int START_CUE_FALLBACK_TICKS = 20;

    private static final int START_CUE_TIMEOUT_TICKS = 100;

    @Nullable
    private static Identifier current;

    @Nullable
    private static RadioSoundInstance sound;

    @Nullable
    private static RadioSoundInstance startCue;

    @Nullable
    private static SoundEvent pendingTapeSound;

    private static boolean startCueWasActive;

    private static boolean started;

    private static int cueTicks;

    private static int ticksSinceStart;

    private static int playedTicks;

    private HolotapePlayer() {}

    @Nullable
    public static Identifier current() {
        return current;
    }

    public static boolean isPlaying() {
        return current != null && (pendingTapeSound != null || started);
    }

    public static int playedTicks() {
        return playedTicks;
    }

    public static void play(Identifier id) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        var holotape = Holotape.get(minecraft.level.registryAccess(), id).orElse(null);
        if (holotape == null || !minecraft.player.getData(ADAttachments.HOLOTAPES.get()).has(id)) {
            return;
        }
        RadioPlayer.stop();
        clearPlayback();
        current = id;
        pendingTapeSound = SoundEvent.createVariableRangeEvent(holotape.sound());
        if (AtomicDawn.CONFIG.clientPipboy.PIPBOY_RADIO_PAUSES_MUSIC) {
            minecraft.getMusicManager().stopPlaying();
        }
        startCue = new RadioSoundInstance(ADSounds.HOLOTAPE_START.get());
        minecraft.getSoundManager().play(startCue);
    }

    public static void stop() {
        finish(true);
    }

    private static void finish(boolean playEjectCue) {
        boolean hadTape = current != null;
        clearPlayback();
        if (playEjectCue && hadTape && Minecraft.getInstance().level != null) {
            Minecraft.getInstance()
                .getSoundManager()
                .play(
                    new RadioSoundInstance(ADSounds.HOLOTAPE_STOP.get())
                );
        }
    }

    private static void clearPlayback() {
        var manager = Minecraft.getInstance().getSoundManager();
        if (sound != null) {
            manager.stop(sound);
            sound = null;
        }
        if (startCue != null) {
            manager.stop(startCue);
            startCue = null;
        }
        current = null;
        pendingTapeSound = null;
        started = false;
        startCueWasActive = false;
        cueTicks = 0;
        ticksSinceStart = 0;
        playedTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (current == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            finish(false);
            return;
        }
        var manager = minecraft.getSoundManager();
        if (pendingTapeSound != null) {
            cueTicks++;
            boolean active = startCue != null && manager.isActive(startCue);
            if (active) {
                startCueWasActive = true;
            }
            boolean cueFinished = startCueWasActive && !active;
            boolean cueUnavailable = !startCueWasActive && cueTicks >= START_CUE_FALLBACK_TICKS;
            if (!cueFinished && !cueUnavailable && cueTicks < START_CUE_TIMEOUT_TICKS) {
                return;
            }
            if (startCue != null) {
                manager.stop(startCue);
                startCue = null;
            }
            sound = new RadioSoundInstance(pendingTapeSound);
            pendingTapeSound = null;
            ticksSinceStart = 0;
            manager.play(sound);
            return;
        }
        if (sound == null) {
            finish(false);
            return;
        }
        ticksSinceStart++;
        if (manager.isActive(sound)) {
            started = true;
            playedTicks++;
            return;
        }
        if (!started && ticksSinceStart < START_GRACE_TICKS) {
            return;
        }
        if (!started) {
            AtomicDawn.LOGGER.warn("Holotape '{}' did not start; check sounds.json and the .ogg file", current);
            finish(false);
            return;
        }
        finish(true);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        finish(false);
    }
}

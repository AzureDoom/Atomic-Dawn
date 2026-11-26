package mod.azure.atomicdawn.client.pipboy.radio;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.radio.RadioStation;

@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class RadioPlayer {

    private static final int START_GRACE_TICKS = 100;

    private static final RandomSource RANDOM = RandomSource.create();

    private static final Set<Identifier> WARNED = new HashSet<>();

    @Nullable
    private static Identifier station;

    private static final List<Identifier> PLAYLIST = new ArrayList<>();

    private static int index;

    @Nullable
    private static RadioSoundInstance current;

    private static boolean started;

    private static int ticksSinceStart;

    private static int playedTicks;

    private RadioPlayer() {}

    @Nullable
    public static Identifier station() {
        return station;
    }

    @Nullable
    public static Identifier track() {
        return station == null || PLAYLIST.isEmpty() ? null : PLAYLIST.get(index);
    }

    public static boolean isPlaying() {
        return station != null && current != null && started;
    }

    public static int playedTicks() {
        return playedTicks;
    }

    public static void tune(Identifier id) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !AtomicDawn.CONFIG.common.pipboy.features.PIPBOY_ENABLE_RADIO) {
            return;
        }
        var definition = RadioStation.get(minecraft.level.registryAccess(), id).orElse(null);
        if (definition == null) {
            stop();
            return;
        }
        HolotapePlayer.stop();
        stopSound();
        station = id;
        PLAYLIST.clear();
        PLAYLIST.addAll(definition.tracks());
        if (definition.shuffle()) {
            shuffle();
        }
        index = 0;
        play();
    }

    public static void next() {
        if (station == null || PLAYLIST.isEmpty()) {
            return;
        }
        stopSound();
        index++;
        if (index >= PLAYLIST.size()) {
            index = 0;
            var minecraft = Minecraft.getInstance();
            var definition = minecraft.level == null
                ? null
                : RadioStation.get(minecraft.level.registryAccess(), station).orElse(null);
            if (definition != null && definition.shuffle() && PLAYLIST.size() > 1) {
                var last = PLAYLIST.getLast();
                do {
                    shuffle();
                } while (PLAYLIST.getFirst().equals(last));
            }
        }
        play();
    }

    public static void stop() {
        stopSound();
        station = null;
        PLAYLIST.clear();
        index = 0;
    }

    private static void shuffle() {
        for (var i = PLAYLIST.size() - 1; i > 0; i--) {
            var j = RANDOM.nextInt(i + 1);
            var tmp = PLAYLIST.get(i);
            PLAYLIST.set(i, PLAYLIST.get(j));
            PLAYLIST.set(j, tmp);
        }
    }

    private static void play() {
        var track = track();
        if (track == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (AtomicDawn.CONFIG.client.pipboy.radio.PIPBOY_RADIO_PAUSES_MUSIC) {
            minecraft.getMusicManager().stopPlaying();
        }
        current = new RadioSoundInstance(SoundEvent.createVariableRangeEvent(track));
        started = false;
        ticksSinceStart = 0;
        playedTicks = 0;
        minecraft.getSoundManager().play(current);
    }

    private static void stopSound() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
        started = false;
        ticksSinceStart = 0;
        playedTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (station == null) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (
            minecraft.level == null || minecraft.player == null
                || !AtomicDawn.CONFIG.common.pipboy.features.PIPBOY_ENABLE_RADIO
        ) {
            stop();
            return;
        }
        if (RadioStation.get(minecraft.level.registryAccess(), station).isEmpty()) {
            stop();
            return;
        }
        if (
            !AtomicDawn.CONFIG.client.pipboy.radio.PIPBOY_RADIO_WHEN_CLOSED && !(minecraft.gui
                .screen() instanceof PipBoyScreen)
        ) {
            stop();
            return;
        }
        if (current == null) {
            return;
        }
        ticksSinceStart++;
        if (minecraft.getSoundManager().isActive(current)) {
            started = true;
            playedTicks++;
            if (AtomicDawn.CONFIG.client.pipboy.radio.PIPBOY_RADIO_PAUSES_MUSIC && playedTicks % 100 == 0) {
                minecraft.getMusicManager().stopPlaying();
            }
            return;
        }
        if (!started && ticksSinceStart < START_GRACE_TICKS) {
            return;
        }
        if (!started) {
            var track = track();
            if (track != null && WARNED.add(track)) {
                AtomicDawn.LOGGER.warn("Radio track '{}' did not start; check sounds.json and the .ogg file", track);
            }
        }
        next();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }
}

package mod.azure.atomicdawn.client.pipboy.radio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import mod.azure.atomicdawn.AtomicDawn;

public final class RadioSoundInstance extends AbstractTickableSoundInstance {

    public RadioSoundInstance(SoundEvent event) {
        super(event, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.volume = targetVolume();
    }

    private static float targetVolume() {
        return (float) AtomicDawn.CONFIG.client.pipboy.radio.PIPBOY_RADIO_VOLUME;
    }

    @Override
    public void tick() {
        volume = targetVolume();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}

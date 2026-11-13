package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record NpcInteraction(
    Optional<Identifier> dialogue,
    String greeting
) {

    public static final int MAX_GREETING = 512;

    public static final NpcInteraction DEFAULT = new NpcInteraction(Optional.empty(), "");

    public static final Codec<NpcInteraction> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("dialogue").forGetter(NpcInteraction::dialogue),
            Codec.STRING.optionalFieldOf("greeting", "").forGetter(NpcInteraction::greeting)
        ).apply(instance, NpcInteraction::new)
    );

    public NpcInteraction sanitized() {
        var text = greeting == null ? "" : greeting.strip();
        return new NpcInteraction(
            dialogue == null ? Optional.empty() : dialogue,
            text.length() > MAX_GREETING ? text.substring(0, MAX_GREETING) : text
        );
    }
}

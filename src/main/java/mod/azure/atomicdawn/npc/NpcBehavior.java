package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record NpcBehavior(
    Movement movement,
    int wanderRadius,
    boolean lookAtPlayers,
    float speed,
    NpcPose pose,
    boolean immovable
) {

    public enum Movement implements StringRepresentable {

        STATIONARY,
        WANDER;

        public static final Codec<Movement> CODEC = StringRepresentable.fromEnum(Movement::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Movement next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum NpcPose implements StringRepresentable {

        STANDING,
        CROUCHING,
        SITTING;

        public static final Codec<NpcPose> CODEC = StringRepresentable.fromEnum(NpcPose::values);

        private static final NpcPose[] VALUES = values();

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public NpcPose next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public static NpcPose byIndex(int index) {
            return index >= 0 && index < VALUES.length ? VALUES[index] : STANDING;
        }
    }

    public static final int MAX_WANDER_RADIUS = 64;

    public static final NpcBehavior DEFAULT = new NpcBehavior(
        Movement.STATIONARY,
        8,
        true,
        0.5F,
        NpcPose.STANDING,
        true
    );

    public static final Codec<NpcBehavior> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Movement.CODEC.optionalFieldOf("movement", Movement.STATIONARY).forGetter(NpcBehavior::movement),
            Codec.INT.optionalFieldOf("wander_radius", 8).forGetter(NpcBehavior::wanderRadius),
            Codec.BOOL.optionalFieldOf("look_at_players", true).forGetter(NpcBehavior::lookAtPlayers),
            Codec.FLOAT.optionalFieldOf("speed", 0.5F).forGetter(NpcBehavior::speed),
            NpcPose.CODEC.optionalFieldOf("pose", NpcPose.STANDING).forGetter(NpcBehavior::pose),
            Codec.BOOL.optionalFieldOf("immovable", true).forGetter(NpcBehavior::immovable)
        ).apply(instance, NpcBehavior::new)
    );

    public NpcBehavior sanitized() {
        return new NpcBehavior(
            movement == null ? Movement.STATIONARY : movement,
            Math.clamp(wanderRadius, 1, MAX_WANDER_RADIUS),
            lookAtPlayers,
            Float.isFinite(speed) ? Math.clamp(speed, 0.05F, 2F) : 0.5F,
            pose == null ? NpcPose.STANDING : pose,
            immovable
        );
    }
}

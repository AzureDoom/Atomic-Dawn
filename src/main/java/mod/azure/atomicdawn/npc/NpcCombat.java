package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record NpcCombat(
    Hostility hostility,
    float meleeDamage,
    float armor,
    float aggroRange,
    float rangedRange,
    float aimSpread,
    float damageMultiplier,
    boolean respawn,
    int respawnSeconds
) {

    public enum Hostility implements StringRepresentable {

        PASSIVE,
        DEFENSIVE,
        GUARD,
        HOSTILE;

        public static final Codec<Hostility> CODEC = StringRepresentable.fromEnum(Hostility::values);

        private static final Hostility[] VALUES = values();

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Hostility next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public boolean fights() {
            return this != PASSIVE;
        }
    }

    public static final NpcCombat DEFAULT = new NpcCombat(
        Hostility.DEFENSIVE,
        2F,
        0F,
        16F,
        24F,
        4F,
        1F,
        false,
        60
    );

    public static final Codec<NpcCombat> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Hostility.CODEC.optionalFieldOf("hostility", Hostility.DEFENSIVE).forGetter(NpcCombat::hostility),
            Codec.FLOAT.optionalFieldOf("melee_damage", 2F).forGetter(NpcCombat::meleeDamage),
            Codec.FLOAT.optionalFieldOf("armor", 0F).forGetter(NpcCombat::armor),
            Codec.FLOAT.optionalFieldOf("aggro_range", 16F).forGetter(NpcCombat::aggroRange),
            Codec.FLOAT.optionalFieldOf("ranged_range", 24F).forGetter(NpcCombat::rangedRange),
            Codec.FLOAT.optionalFieldOf("aim_spread", 4F).forGetter(NpcCombat::aimSpread),
            Codec.FLOAT.optionalFieldOf("damage_multiplier", 1F).forGetter(NpcCombat::damageMultiplier),
            Codec.BOOL.optionalFieldOf("respawn", false).forGetter(NpcCombat::respawn),
            Codec.INT.optionalFieldOf("respawn_seconds", 60).forGetter(NpcCombat::respawnSeconds)
        ).apply(instance, NpcCombat::new)
    );

    public NpcCombat sanitized() {
        return new NpcCombat(
            hostility == null ? Hostility.DEFENSIVE : hostility,
            clamp(meleeDamage, 0F, 1000F, 2F),
            clamp(armor, 0F, 30F, 0F),
            clamp(aggroRange, 2F, 64F, 16F),
            clamp(rangedRange, 2F, 96F, 24F),
            clamp(aimSpread, 0F, 45F, 4F),
            clamp(damageMultiplier, 0F, 20F, 1F),
            respawn,
            Math.clamp(respawnSeconds, 1, 3600)
        );
    }

    private static float clamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }

    public NpcCombat withHostility(Hostility value) {
        return new NpcCombat(
            value,
            meleeDamage,
            armor,
            aggroRange,
            rangedRange,
            aimSpread,
            damageMultiplier,
            respawn,
            respawnSeconds
        );
    }

    public NpcCombat withMelee(float value) {
        return new NpcCombat(
            hostility,
            value,
            armor,
            aggroRange,
            rangedRange,
            aimSpread,
            damageMultiplier,
            respawn,
            respawnSeconds
        );
    }

    public NpcCombat withArmor(float value) {
        return new NpcCombat(
            hostility,
            meleeDamage,
            value,
            aggroRange,
            rangedRange,
            aimSpread,
            damageMultiplier,
            respawn,
            respawnSeconds
        );
    }

    public NpcCombat withAggroRange(float value) {
        return new NpcCombat(
            hostility,
            meleeDamage,
            armor,
            value,
            rangedRange,
            aimSpread,
            damageMultiplier,
            respawn,
            respawnSeconds
        );
    }

    public NpcCombat withRangedRange(float value) {
        return new NpcCombat(
            hostility,
            meleeDamage,
            armor,
            aggroRange,
            value,
            aimSpread,
            damageMultiplier,
            respawn,
            respawnSeconds
        );
    }

    public NpcCombat withAimSpread(float value) {
        return new NpcCombat(
            hostility,
            meleeDamage,
            armor,
            aggroRange,
            rangedRange,
            value,
            damageMultiplier,
            respawn,
            respawnSeconds
        );
    }

    public NpcCombat withDamageMultiplier(float value) {
        return new NpcCombat(
            hostility,
            meleeDamage,
            armor,
            aggroRange,
            rangedRange,
            aimSpread,
            value,
            respawn,
            respawnSeconds
        );
    }

    public NpcCombat withRespawn(boolean value, int seconds) {
        return new NpcCombat(
            hostility,
            meleeDamage,
            armor,
            aggroRange,
            rangedRange,
            aimSpread,
            damageMultiplier,
            value,
            seconds
        );
    }
}

package mod.azure.atomicdawn.registry;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampRegistry;
import mod.azure.atomicdawn.ghoul.GhoulAppearance;
import mod.azure.atomicdawn.ghoul.GhoulState;
import mod.azure.atomicdawn.limb.LimbState;
import mod.azure.atomicdawn.lock.LockMap;
import mod.azure.atomicdawn.mutation.Mutations;
import mod.azure.atomicdawn.pipboy.PipBoyData;
import mod.azure.atomicdawn.pipboy.PipBoyStats;
import mod.azure.atomicdawn.pipboy.bestiary.BestiaryLog;
import mod.azure.atomicdawn.pipboy.holotape.HolotapeLog;
import mod.azure.atomicdawn.pipboy.location.LocationLog;
import mod.azure.atomicdawn.progression.KnownMods;
import mod.azure.atomicdawn.quest.QuestLog;
import mod.azure.atomicdawn.radiation.BlastZones;
import mod.azure.atomicdawn.radiation.RadStorm;
import mod.azure.atomicdawn.radiation.Radiation;
import mod.azure.atomicdawn.special.SpecialProgress;
import mod.azure.atomicdawn.thirst.Thirst;
import mod.azure.atomicdawn.vats.ActionPoints;
import mod.azure.atomicdawn.weapon.action.ShooterState;

@SuppressWarnings("unused")
public final class ADAttachments {

    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(
        NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ShooterState>> SHOOTER_STATE = REGISTER
        .register("shooter_state", () -> AttachmentType.builder(ShooterState::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<KnownMods>> KNOWN_MODS = REGISTER.register(
        "known_mods",
        () -> AttachmentType.builder(() -> KnownMods.EMPTY)
            .serialize(KnownMods.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, KnownMods.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> STOWED_STABILIZED = REGISTER
        .register(
            "stowed_stabilized",
            () -> AttachmentType.builder(() -> 0F)
                .serialize(Codec.FLOAT.fieldOf("value"))
                .sync((holder, to) -> holder == to, ByteBufCodecs.FLOAT)
                .build()
        );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ActionPoints>> ACTION_POINTS = REGISTER
        .register(
            "action_points",
            () -> AttachmentType.builder(() -> ActionPoints.FULL_DEFAULT)
                .serialize(ActionPoints.MAP_CODEC)
                .sync((holder, to) -> holder == to, ActionPoints.STREAM_CODEC)
                .build()
        );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Radiation>> RADIATION = REGISTER.register(
        "radiation",
        () -> AttachmentType.builder(() -> Radiation.NONE)
            .serialize(Radiation.MAP_CODEC)
            .sync((holder, to) -> holder == to, Radiation.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpecialProgress>> SPECIAL_PROGRESS = REGISTER
        .register(
            "special_progress",
            () -> AttachmentType.builder(() -> SpecialProgress.EMPTY)
                .serialize(SpecialProgress.MAP_CODEC)
                .copyOnDeath()
                .sync((holder, to) -> holder == to, SpecialProgress.STREAM_CODEC)
                .build()
        );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PipBoyData>> PIPBOY = REGISTER.register(
        "pipboy",
        () -> AttachmentType.builder(() -> PipBoyData.EMPTY)
            .serialize(PipBoyData.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, PipBoyData.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PipBoyStats>> PIPBOY_STATS = REGISTER
        .register(
            "pipboy_stats",
            () -> AttachmentType.builder(() -> PipBoyStats.EMPTY).serialize(PipBoyStats.MAP_CODEC).copyOnDeath().build()
        );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<QuestLog>> QUESTS = REGISTER.register(
        "quests",
        () -> AttachmentType.builder(() -> QuestLog.EMPTY)
            .serialize(QuestLog.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, QuestLog.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LocationLog>> LOCATIONS = REGISTER.register(
        "locations",
        () -> AttachmentType.builder(() -> LocationLog.EMPTY)
            .serialize(LocationLog.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, LocationLog.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BestiaryLog>> BESTIARY = REGISTER.register(
        "bestiary",
        () -> AttachmentType.builder(() -> BestiaryLog.EMPTY)
            .serialize(BestiaryLog.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, BestiaryLog.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HolotapeLog>> HOLOTAPES = REGISTER.register(
        "holotapes",
        () -> AttachmentType.builder(() -> HolotapeLog.EMPTY)
            .serialize(HolotapeLog.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, HolotapeLog.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Mutations>> MUTATIONS = REGISTER.register(
        "mutations",
        () -> AttachmentType.builder(() -> Mutations.NONE)
            .serialize(Mutations.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, Mutations.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GhoulState>> GHOUL = REGISTER.register(
        "ghoul",
        () -> AttachmentType.builder(() -> GhoulState.DEFAULT)
            .serialize(GhoulState.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, GhoulState.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GhoulAppearance>> GHOUL_APPEARANCE = REGISTER
        .register(
            "ghoul_appearance",
            () -> AttachmentType.builder(() -> GhoulAppearance.NONE)
                .serialize(GhoulAppearance.MAP_CODEC)
                .copyOnDeath()
                .sync((holder, to) -> true, GhoulAppearance.STREAM_CODEC)
                .build()
        );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LockMap>> LOCKS = REGISTER.register(
        "locks",
        () -> AttachmentType.builder(() -> LockMap.EMPTY).serialize(LockMap.MAP_CODEC).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CampRegistry>> CAMPS = REGISTER.register(
        "camps",
        () -> AttachmentType.builder(() -> CampRegistry.EMPTY).serialize(CampRegistry.MAP_CODEC).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LimbState>> LIMBS = REGISTER.register(
        "limbs",
        () -> AttachmentType.builder(() -> LimbState.HEALTHY)
            .serialize(LimbState.MAP_CODEC)
            .sync((holder, to) -> true, LimbState.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Thirst>> THIRST = REGISTER.register(
        "thirst",
        () -> AttachmentType.builder(() -> Thirst.FULL)
            .serialize(Thirst.MAP_CODEC)
            .sync((holder, to) -> holder == to, Thirst.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> RAD_STORM = REGISTER.register(
        "rad_storm",
        () -> AttachmentType.builder(() -> false).sync((holder, to) -> holder == to, ByteBufCodecs.BOOL).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RadStorm>> LEVEL_RAD_STORM = REGISTER
        .register(
            "level_rad_storm",
            () -> AttachmentType.builder(() -> RadStorm.NONE).serialize(RadStorm.MAP_CODEC).build()
        );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BlastZones>> BLAST_ZONES = REGISTER.register(
        "blast_zones",
        () -> AttachmentType.builder(() -> BlastZones.EMPTY).serialize(BlastZones.MAP_CODEC).build()
    );

    private ADAttachments() {}
}

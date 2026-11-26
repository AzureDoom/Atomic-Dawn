package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;

public record NpcConfig(
    String npcId,
    String name,
    String title,
    boolean showName,
    NpcAppearance appearance,
    NpcBehavior behavior,
    NpcStats stats,
    NpcInteraction interaction,
    NpcCombat combat,
    NpcCompanion companion,
    NpcShop shop
) {

    public static final int MAX_NAME = 64;

    public static final int MAX_DATA_CHARS = 32_768;

    public static final NpcConfig DEFAULT = new NpcConfig(
        "",
        "",
        "",
        true,
        NpcAppearance.DEFAULT,
        NpcBehavior.DEFAULT,
        NpcStats.DEFAULT,
        NpcInteraction.DEFAULT,
        NpcCombat.DEFAULT,
        NpcCompanion.DEFAULT,
        NpcShop.EMPTY
    );

    public static final Codec<NpcConfig> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("npc_id", "").forGetter(NpcConfig::npcId),
            Codec.STRING.optionalFieldOf("name", "").forGetter(NpcConfig::name),
            Codec.STRING.optionalFieldOf("title", "").forGetter(NpcConfig::title),
            Codec.BOOL.optionalFieldOf("show_name", true).forGetter(NpcConfig::showName),
            NpcAppearance.CODEC.optionalFieldOf("appearance", NpcAppearance.DEFAULT).forGetter(NpcConfig::appearance),
            NpcBehavior.CODEC.optionalFieldOf("behavior", NpcBehavior.DEFAULT).forGetter(NpcConfig::behavior),
            NpcStats.CODEC.optionalFieldOf("stats", NpcStats.DEFAULT).forGetter(NpcConfig::stats),
            NpcInteraction.CODEC.optionalFieldOf("interaction", NpcInteraction.DEFAULT)
                .forGetter(NpcConfig::interaction),
            NpcCombat.CODEC.optionalFieldOf("combat", NpcCombat.DEFAULT).forGetter(NpcConfig::combat),
            NpcCompanion.CODEC.optionalFieldOf("companion", NpcCompanion.DEFAULT).forGetter(NpcConfig::companion),
            NpcShop.CODEC.optionalFieldOf("shop", NpcShop.EMPTY).forGetter(NpcConfig::shop)
        ).apply(instance, NpcConfig::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcConfig> STREAM_CODEC = ByteBufCodecs
        .fromCodecWithRegistries(CODEC);

    public Optional<Identifier> identifier() {
        return npcId.isBlank() ? Optional.empty() : Optional.ofNullable(ADRegistryUtil.tryParse(npcId));
    }

    public NpcConfig sanitized() {
        var cleanAppearance = appearance == null ? NpcAppearance.DEFAULT : appearance;
        if (cleanAppearance.data().toString().length() > MAX_DATA_CHARS) {
            cleanAppearance = cleanAppearance.withData(new CompoundTag());
        }
        var data = cleanAppearance.data().copy();
        for (var key : new String[] { "UUID", "Pos", "Motion", "Passengers", "Leash" }) {
            data.remove(key);
        }
        cleanAppearance = cleanAppearance.withData(data);
        if (cleanAppearance.kind() == NpcAppearance.Kind.ENTITY && cleanAppearance.model().equals(NpcIds.NPC_TYPE)) {
            cleanAppearance = NpcAppearance.DEFAULT;
        }
        var cleanId = npcId == null ? "" : npcId.strip();
        if (!cleanId.isEmpty() && ADRegistryUtil.tryParse(cleanId) == null) {
            cleanId = "";
        }
        return new NpcConfig(
            cleanId,
            trim(name),
            trim(title),
            showName,
            cleanAppearance,
            behavior == null ? NpcBehavior.DEFAULT : behavior.sanitized(),
            stats == null ? NpcStats.DEFAULT : stats.sanitized(),
            interaction == null ? NpcInteraction.DEFAULT : interaction.sanitized(),
            combat == null ? NpcCombat.DEFAULT : combat.sanitized(),
            companion == null ? NpcCompanion.DEFAULT : companion.sanitized(),
            shop == null ? NpcShop.EMPTY : shop.sanitized()
        );
    }

    private static String trim(String value) {
        if (value == null) {
            return "";
        }
        var clean = value.strip();
        return clean.length() > MAX_NAME ? clean.substring(0, MAX_NAME) : clean;
    }

    public NpcConfig withAppearance(NpcAppearance value) {
        return new NpcConfig(
            npcId,
            name,
            title,
            showName,
            value,
            behavior,
            stats,
            interaction,
            combat,
            companion,
            shop
        );
    }

    public NpcConfig withGeneral(String newId, String newName, String newTitle, boolean newShowName) {
        return new NpcConfig(
            newId,
            newName,
            newTitle,
            newShowName,
            appearance,
            behavior,
            stats,
            interaction,
            combat,
            companion,
            shop
        );
    }

    public NpcConfig withBehavior(NpcBehavior value) {
        return new NpcConfig(
            npcId,
            name,
            title,
            showName,
            appearance,
            value,
            stats,
            interaction,
            combat,
            companion,
            shop
        );
    }

    public NpcConfig withStats(NpcStats value) {
        return new NpcConfig(
            npcId,
            name,
            title,
            showName,
            appearance,
            behavior,
            value,
            interaction,
            combat,
            companion,
            shop
        );
    }

    public NpcConfig withInteraction(NpcInteraction value) {
        return new NpcConfig(npcId, name, title, showName, appearance, behavior, stats, value, combat, companion, shop);
    }

    public NpcConfig withCombat(NpcCombat value) {
        return new NpcConfig(
            npcId,
            name,
            title,
            showName,
            appearance,
            behavior,
            stats,
            interaction,
            value,
            companion,
            shop
        );
    }

    public NpcConfig withCompanion(NpcCompanion value) {
        return new NpcConfig(
            npcId,
            name,
            title,
            showName,
            appearance,
            behavior,
            stats,
            interaction,
            combat,
            value,
            shop
        );
    }

    public NpcConfig withShop(NpcShop value) {
        return new NpcConfig(
            npcId,
            name,
            title,
            showName,
            appearance,
            behavior,
            stats,
            interaction,
            combat,
            companion,
            value
        );
    }
}

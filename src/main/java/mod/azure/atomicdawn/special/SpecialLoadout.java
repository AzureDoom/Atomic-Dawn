package mod.azure.atomicdawn.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SpecialLoadout(
    String name,
    List<Integer> special,
    Map<Identifier, Integer> equipped
) {

    public static final int MAX_NAME_LENGTH = 24;

    public static final int MAX_EQUIPPED = 64;

    public static final Codec<SpecialLoadout> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(SpecialLoadout::name),
            Codec.INT.listOf()
                .fieldOf("special")
                .forGetter(SpecialLoadout::special),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("equipped", Map.of())
                .forGetter(SpecialLoadout::equipped)
        ).apply(instance, SpecialLoadout::new)
    );

    public static final StreamCodec<ByteBuf, SpecialLoadout> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH),
        SpecialLoadout::name,
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(SpecialAttribute.COUNT)),
        SpecialLoadout::special,
        ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT, MAX_EQUIPPED),
        SpecialLoadout::equipped,
        SpecialLoadout::new
    );

    public SpecialLoadout {
        name = sanitizeName(name);
        var copy = new ArrayList<Integer>(SpecialAttribute.COUNT);
        for (var i = 0; i < SpecialAttribute.COUNT; i++) {
            copy.add(special != null && i < special.size() ? special.get(i) : 1);
        }
        special = List.copyOf(copy);
        equipped = Map.copyOf(equipped);
    }

    public static SpecialLoadout fresh(int startingPoints) {
        var list = new ArrayList<Integer>(SpecialAttribute.COUNT);
        for (var i = 0; i < SpecialAttribute.COUNT; i++) {
            list.add(startingPoints);
        }
        return new SpecialLoadout("", list, Map.of());
    }

    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        var builder = new StringBuilder();
        for (var i = 0; i < raw.length() && builder.length() < MAX_NAME_LENGTH; i++) {
            var c = raw.charAt(i);
            if (c == 0xA7 || Character.isISOControl(c)) {
                continue;
            }
            builder.append(c);
        }
        return builder.toString().strip();
    }

    public int base(SpecialAttribute attribute) {
        return special.get(attribute.ordinal());
    }

    public int allocated() {
        var total = 0;
        for (var value : special) {
            total += value;
        }
        return total;
    }

    public SpecialLoadout withSpecial(List<Integer> values) {
        return new SpecialLoadout(name, values, equipped);
    }

    public SpecialLoadout withName(String value) {
        return new SpecialLoadout(value, special, equipped);
    }

    public SpecialLoadout withEquipped(Map<Identifier, Integer> value) {
        return new SpecialLoadout(name, special, value);
    }

    public SpecialLoadout equip(Identifier perk, int rank) {
        var copy = new HashMap<>(equipped);
        copy.put(perk, rank);
        return withEquipped(copy);
    }

    public SpecialLoadout unequip(Identifier perk) {
        if (!equipped.containsKey(perk)) {
            return this;
        }
        var copy = new HashMap<>(equipped);
        copy.remove(perk);
        return withEquipped(copy);
    }
}

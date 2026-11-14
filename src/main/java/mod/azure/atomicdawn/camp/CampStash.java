package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record CampStash(List<ItemStack> items) {

    public static final int ROWS = 6;

    public static final int SIZE = ROWS * 9;

    public static final CampStash EMPTY = new CampStash(List.of());

    private record Entry(
        int slot,
        ItemStack stack
    ) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.intRange(0, SIZE - 1).fieldOf("slot").forGetter(Entry::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::stack)
            ).apply(instance, Entry::new)
        );
    }

    public static final MapCodec<CampStash> MAP_CODEC = Entry.CODEC.listOf()
        .optionalFieldOf("items", List.of())
        .xmap(CampStash::fromEntries, CampStash::toEntries);

    public CampStash {
        var copy = new ArrayList<ItemStack>(SIZE);
        for (var i = 0; i < SIZE; i++) {
            copy.add(i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        }
        items = List.copyOf(copy);
    }

    private static CampStash fromEntries(List<Entry> entries) {
        var list = new ArrayList<ItemStack>(SIZE);
        for (var i = 0; i < SIZE; i++) {
            list.add(ItemStack.EMPTY);
        }
        for (var entry : entries) {
            list.set(entry.slot(), entry.stack());
        }
        return new CampStash(list);
    }

    private List<Entry> toEntries() {
        var entries = new ArrayList<Entry>();
        for (var i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                entries.add(new Entry(i, items.get(i)));
            }
        }
        return entries;
    }

    public static CampStash of(Player player) {
        return player.getData(CampAttachments.STASH.get());
    }

    public static SimpleContainer open(Player player) {
        var container = new SimpleContainer(SIZE) {

            private boolean loaded = false;

            @Override
            public void setChanged() {
                super.setChanged();

                if (loaded) {
                    save(player, this);
                }
            }

            public SimpleContainer finishLoading() {
                loaded = true;
                return this;
            }
        };

        var stash = of(player);

        for (var i = 0; i < SIZE; i++) {
            container.setItem(i, stash.items().get(i).copy());
        }

        return container.finishLoading();
    }

    private static void save(Player player, SimpleContainer container) {
        var list = new ArrayList<ItemStack>(SIZE);
        for (var i = 0; i < SIZE; i++) {
            list.add(container.getItem(i).copy());
        }
        player.setData(CampAttachments.STASH.get(), new CampStash(list));
    }
}

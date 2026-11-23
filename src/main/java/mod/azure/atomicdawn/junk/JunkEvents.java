package mod.azure.atomicdawn.junk;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class JunkEvents {

    private JunkEvents() {}

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        ScrapRecipes.invalidate();
    }
}

package mod.azure.atomicdawn.camp;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import mod.azure.atomicdawn.AtomicDawn;

public final class CampAttachments {

    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(
        NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CampPlans>> CAMP_PLANS = REGISTER.register(
        "camp_plans",
        () -> AttachmentType.builder(() -> CampPlans.EMPTY)
            .serialize(CampPlans.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, CampPlans.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CampStash>> STASH = REGISTER.register(
        "camp_stash",
        () -> AttachmentType.builder(() -> CampStash.EMPTY).serialize(CampStash.MAP_CODEC).copyOnDeath().build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CampScrapbox>> SCRAPBOX = REGISTER.register(
        "camp_scrapbox",
        () -> AttachmentType.builder(() -> CampScrapbox.EMPTY)
            .serialize(CampScrapbox.MAP_CODEC)
            .copyOnDeath()
            .sync((holder, to) -> holder == to, CampScrapbox.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CampBlueprintLibrary>> BLUEPRINTS = REGISTER
        .register(
            "camp_blueprints",
            () -> AttachmentType.builder(() -> CampBlueprintLibrary.EMPTY)
                .serialize(CampBlueprintLibrary.MAP_CODEC)
                .copyOnDeath()
                .sync((holder, to) -> holder == to, CampBlueprintLibrary.STREAM_CODEC)
                .build()
        );

    private CampAttachments() {}
}

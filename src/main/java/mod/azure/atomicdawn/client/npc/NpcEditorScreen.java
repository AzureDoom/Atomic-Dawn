package mod.azure.atomicdawn.client.npc;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.network.NpcEditPayload;
import mod.azure.atomicdawn.npc.NpcAppearance;
import mod.azure.atomicdawn.npc.NpcBehavior;
import mod.azure.atomicdawn.npc.NpcCompanion;
import mod.azure.atomicdawn.npc.NpcConfig;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.npc.NpcInteraction;
import mod.azure.atomicdawn.npc.NpcModels;
import mod.azure.atomicdawn.npc.NpcShop;
import mod.azure.atomicdawn.npc.NpcStats;
import mod.azure.atomicdawn.npc.dialogue.NpcDialogue;

@SuppressWarnings("unused")
public final class NpcEditorScreen extends Screen {

    private enum Tab {

        GENERAL,
        APPEARANCE,
        BEHAVIOR,
        INTERACTION,
        COMBAT,
        SERVICES;

        String key() {
            return "npc.atomicdawn.editor.tab." + name().toLowerCase(Locale.ROOT);
        }
    }

    private record Label(
        Component text,
        int x,
        int y,
        int color
    ) {}

    private static final int ROW = 22;

    private static final int FIELD_H = 18;

    private static final int LIST_ROW = 11;

    private static final int LABEL_W = 78;

    private static final String SKIN_KEY = "profile";

    private final int entityId;

    private NpcConfig config;

    private Tab tab = Tab.GENERAL;

    private final List<Label> labels = new ArrayList<>();

    private String filter = "";

    private int scroll;

    private int listX;

    private int listY;

    private int listW;

    private int listRows;

    private Component dataError = Component.empty();

    private int left;

    private int top;

    private int panelW;

    private int panelBottom;

    public NpcEditorScreen(int entityId, NpcConfig config) {
        super(Component.translatable("npc.atomicdawn.editor.title"));
        this.entityId = entityId;
        this.config = config;
    }

    @Override
    protected void init() {
        labels.clear();
        panelW = Math.min(300, width / 2 - 8);
        left = 8;
        top = 8;
        panelBottom = height - 8;
        var tabW = (panelW - 8) / Tab.values().length;
        var x = left + 4;
        for (var value : Tab.values()) {
            var button = Button.builder(Component.translatable(value.key()), _ -> {
                tab = value;
                scroll = 0;
                rebuildWidgets();
            }).pos(x, top + 4).size(tabW - 2, 18).build();
            button.active = value != tab;
            addRenderableWidget(button);
            x += tabW;
        }
        var y = top + 30;
        switch (tab) {
            case GENERAL -> initGeneral(y);
            case APPEARANCE -> initAppearance(y);
            case BEHAVIOR -> initBehavior(y);
            case INTERACTION -> initInteraction(y);
            case COMBAT -> initCombat(y);
            case SERVICES -> initServices(y);
        }
        initFooter();
    }

    private int contentLeft() {
        return left + 6;
    }

    private int fieldX() {
        return contentLeft() + LABEL_W;
    }

    private int fieldW() {
        return panelW - LABEL_W - 12;
    }

    private EditBox field(String key, int y, String value, int maxLength, Consumer<String> responder) {
        labels.add(new Label(Component.translatable(key), contentLeft(), y + 5, PipBoyTheme.dim()));
        var box = new EditBox(font, fieldX(), y, fieldW(), FIELD_H, Component.translatable(key));
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setResponder(responder);
        addRenderableWidget(box);
        return box;
    }

    private EditBox half(String key, int y, boolean right, String value, int maxLength, Consumer<String> responder) {
        var columnW = (panelW - 12) / 2;
        var x = contentLeft() + (right ? columnW + 4 : 0);
        var labelW = Math.min(LABEL_W - 16, font.width(Component.translatable(key)) + 4);
        labels.add(new Label(Component.translatable(key), x, y + 5, PipBoyTheme.dim()));
        var box = new EditBox(font, x + labelW, y, columnW - labelW - 4, FIELD_H, Component.translatable(key));
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setResponder(responder);
        addRenderableWidget(box);
        return box;
    }

    private static String number(float value) {
        return value == Math.round(value)
            ? Integer.toString(Math.round(value))
            : String.format(Locale.ROOT, "%.2f", value);
    }

    private void button(Component label, int y, Runnable action) {
        addRenderableWidget(
            Button.builder(label, _ -> action.run()).pos(contentLeft(), y).size(panelW - 12, FIELD_H).build()
        );
    }

    private static Component onOff(String key, boolean value) {
        return Component.translatable(
            key,
            Component.translatable(value ? "npc.atomicdawn.editor.on" : "npc.atomicdawn.editor.off")
        );
    }

    private void initGeneral(int y) {
        field(
            "npc.atomicdawn.editor.name",
            y,
            config.name(),
            NpcConfig.MAX_NAME,
            value -> config = config.withGeneral(
                config.npcId(),
                value,
                config.title(),
                config.showName()
            )
        );
        y += ROW;
        field(
            "npc.atomicdawn.editor.npc_title",
            y,
            config.title(),
            NpcConfig.MAX_NAME,
            value -> config = config
                .withGeneral(config.npcId(), config.name(), value, config.showName())
        );
        y += ROW;
        var idBox = field(
            "npc.atomicdawn.editor.npc_id",
            y,
            config.npcId(),
            128,
            value -> config = config.withGeneral(
                value,
                config.name(),
                config.title(),
                config.showName()
            )
        );
        idBox.setHint(Component.literal("atomicdawn:overseer"));
        y += ROW;
        button(onOff("npc.atomicdawn.editor.show_name", config.showName()), y, () -> {
            config = config.withGeneral(config.npcId(), config.name(), config.title(), !config.showName());
            rebuildWidgets();
        });
        y += ROW + 4;
        labels.add(
            new Label(
                Component.translatable("npc.atomicdawn.editor.npc_id_hint"),
                contentLeft(),
                y,
                PipBoyTheme.faint()
            )
        );
    }

    private void initAppearance(int y) {
        var appearance = config.appearance();
        var azure = appearance.kind() == NpcAppearance.Kind.AZURE;
        button(
            Component.translatable(
                "npc.atomicdawn.editor.kind",
                Component.translatable("npc.atomicdawn.editor.kind." + appearance.kind().getSerializedName())
            ),
            y,
            () -> {
                var next = azure ? NpcAppearance.Kind.ENTITY : NpcAppearance.Kind.AZURE;
                var models = next == NpcAppearance.Kind.AZURE ? azureModels() : List.of(NpcAppearance.DEFAULT_MODEL);
                var model = models.isEmpty() ? NpcAppearance.DEFAULT_MODEL : models.getFirst();
                setAppearance(config.appearance().withKind(next, model));
                scroll = 0;
                rebuildWidgets();
            }
        );
        y += ROW;
        var search = field("npc.atomicdawn.editor.search", y, filter, 64, value -> {
            filter = value.toLowerCase(Locale.ROOT);
            scroll = 0;
        });
        search.setHint(Component.translatable("npc.atomicdawn.editor.search_hint"));
        y += ROW;
        var footer = 2 * ROW + 8;
        var after = azure ? 2 * ROW : 3 * ROW + 12;
        listRows = Math.clamp((panelBottom - footer - after - y - 4) / LIST_ROW, 3, 14);
        listX = contentLeft();
        listY = y;
        listW = panelW - 12;
        y += listRows * LIST_ROW + 6;
        field(
            "npc.atomicdawn.editor.scale",
            y,
            String.format(Locale.ROOT, "%.2f", appearance.scale()),
            8,
            value -> parseFloat(value).ifPresent(scale -> setAppearance(config.appearance().withScale(scale)))
        );
        y += ROW;
        if (azure) {
            var texture = field(
                "npc.atomicdawn.editor.texture",
                y,
                appearance.texture().map(Identifier::toString).orElse(""),
                256,
                value -> setAppearance(
                    config.appearance()
                        .withTexture(
                            value.isBlank() ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(value.strip()))
                        )
                )
            );
            texture.setHint(Component.translatable("npc.atomicdawn.editor.texture_hint"));
            return;
        }
        var skin = field("npc.atomicdawn.editor.skin", y, appearance.data().getStringOr(SKIN_KEY, ""), 64, value -> {
            var data = config.appearance().data().copy();
            if (value.isBlank()) {
                data.remove(SKIN_KEY);
            } else {
                data.putString(SKIN_KEY, value.strip());
                data.putBoolean("hide_description", true);
            }
            var updated = config.appearance().withData(data);
            if (!value.isBlank()) {
                updated = new NpcAppearance(
                    NpcAppearance.Kind.ENTITY,
                    NpcAppearance.DEFAULT_MODEL,
                    data,
                    updated.scale(),
                    Optional.empty()
                );
            }
            setAppearance(updated);
        });
        skin.setHint(Component.translatable("npc.atomicdawn.editor.skin_hint"));
        y += ROW;
        field("npc.atomicdawn.editor.data", y, appearance.data().toString(), NpcConfig.MAX_DATA_CHARS, value -> {
            if (value.isBlank()) {
                dataError = Component.empty();
                setAppearance(config.appearance().withData(new CompoundTag()));
                return;
            }
            try {
                var parsed = TagParser.parseCompoundFully(value);
                dataError = Component.empty();
                setAppearance(config.appearance().withData(parsed));
            } catch (CommandSyntaxException exception) {
                dataError = Component.literal(exception.getMessage());
            }
        });
    }

    private void initBehavior(int y) {
        var behavior = config.behavior();
        button(
            Component.translatable(
                "npc.atomicdawn.editor.movement",
                Component.translatable("npc.atomicdawn.editor.movement." + behavior.movement().getSerializedName())
            ),
            y,
            () -> {
                setBehavior(withMovement(config.behavior(), config.behavior().movement().next()));
                rebuildWidgets();
            }
        );
        y += ROW;
        field(
            "npc.atomicdawn.editor.radius",
            y,
            Integer.toString(behavior.wanderRadius()),
            3,
            value -> parseInt(value).ifPresent(radius -> {
                var current = config.behavior();
                setBehavior(
                    new NpcBehavior(
                        current.movement(),
                        radius,
                        current.lookAtPlayers(),
                        current.speed(),
                        current.pose(),
                        current.immovable()
                    )
                );
            })
        );
        y += ROW;
        field(
            "npc.atomicdawn.editor.speed",
            y,
            String.format(Locale.ROOT, "%.2f", behavior.speed()),
            6,
            value -> parseFloat(value).ifPresent(speed -> {
                var current = config.behavior();
                setBehavior(
                    new NpcBehavior(
                        current.movement(),
                        current.wanderRadius(),
                        current.lookAtPlayers(),
                        speed,
                        current.pose(),
                        current.immovable()
                    )
                );
            })
        );
        y += ROW;
        button(onOff("npc.atomicdawn.editor.look", behavior.lookAtPlayers()), y, () -> {
            var current = config.behavior();
            setBehavior(
                new NpcBehavior(
                    current.movement(),
                    current.wanderRadius(),
                    !current.lookAtPlayers(),
                    current.speed(),
                    current.pose(),
                    current.immovable()
                )
            );
            rebuildWidgets();
        });
        y += ROW;
        button(
            Component.translatable(
                "npc.atomicdawn.editor.pose",
                Component.translatable("npc.atomicdawn.editor.pose." + behavior.pose().getSerializedName())
            ),
            y,
            () -> {
                var current = config.behavior();
                setBehavior(
                    new NpcBehavior(
                        current.movement(),
                        current.wanderRadius(),
                        current.lookAtPlayers(),
                        current.speed(),
                        current.pose().next(),
                        current.immovable()
                    )
                );
                rebuildWidgets();
            }
        );
        y += ROW;
        button(onOff("npc.atomicdawn.editor.immovable", behavior.immovable()), y, () -> {
            var current = config.behavior();
            setBehavior(
                new NpcBehavior(
                    current.movement(),
                    current.wanderRadius(),
                    current.lookAtPlayers(),
                    current.speed(),
                    current.pose(),
                    !current.immovable()
                )
            );
            rebuildWidgets();
        });
        y += ROW;
        var half = (panelW - 14) / 2;
        addRenderableWidget(
            Button.builder(
                Component.translatable("npc.atomicdawn.editor.move_here"),
                _ -> send(NpcEditPayload.Action.MOVE_HERE)
            )
                .pos(contentLeft(), y)
                .size(half, FIELD_H)
                .build()
        );
        addRenderableWidget(
            Button.builder(
                Component.translatable("npc.atomicdawn.editor.set_home"),
                _ -> send(NpcEditPayload.Action.SET_HOME)
            )
                .pos(contentLeft() + half + 2, y)
                .size(half, FIELD_H)
                .build()
        );
        y += ROW;
        addRenderableWidget(
            Button.builder(
                Component.translatable("npc.atomicdawn.editor.edit_gear"),
                _ -> send(NpcEditPayload.Action.EDIT_GEAR)
            ).pos(contentLeft(), y).size(half, FIELD_H).build()
        );
        addRenderableWidget(
            Button.builder(
                Component.translatable("npc.atomicdawn.editor.clear_equipment"),
                _ -> send(NpcEditPayload.Action.CLEAR_EQUIPMENT)
            ).pos(contentLeft() + half + 2, y).size(half, FIELD_H).build()
        );
    }

    private static NpcBehavior withMovement(NpcBehavior current, NpcBehavior.Movement movement) {
        return new NpcBehavior(
            movement,
            current.wanderRadius(),
            current.lookAtPlayers(),
            current.speed(),
            current.pose(),
            current.immovable()
        );
    }

    private void initInteraction(int y) {
        var interaction = config.interaction();
        var dialogues = dialogueIds();
        int current = interaction.dialogue().map(dialogues::indexOf).orElse(-1);
        var label = interaction.dialogue()
            .map(id -> Component.literal(id.toString()))
            .orElse(Component.translatable("npc.atomicdawn.editor.none"));
        labels.add(
            new Label(Component.translatable("npc.atomicdawn.editor.dialogue"), contentLeft(), y + 5, PipBoyTheme.dim())
        );
        addRenderableWidget(
            Button.builder(Component.literal("<"), _ -> cycleDialogue(dialogues, current - 1))
                .pos(fieldX(), y)
                .size(16, FIELD_H)
                .build()
        );
        var middle = Button.builder(label, _ -> {}).pos(fieldX() + 18, y).size(fieldW() - 36, FIELD_H).build();
        middle.active = false;
        addRenderableWidget(middle);
        addRenderableWidget(
            Button.builder(Component.literal(">"), _ -> cycleDialogue(dialogues, current + 1))
                .pos(fieldX() + fieldW() - 16, y)
                .size(16, FIELD_H)
                .build()
        );
        y += ROW;
        field(
            "npc.atomicdawn.editor.greeting",
            y,
            interaction.greeting(),
            NpcInteraction.MAX_GREETING,
            value -> config = config.withInteraction(new NpcInteraction(config.interaction().dialogue(), value))
        );
        y += ROW;
        var stats = config.stats();
        field(
            "npc.atomicdawn.editor.health",
            y,
            String.format(Locale.ROOT, "%.0f", stats.maxHealth()),
            6,
            value -> parseFloat(value).ifPresent(
                health -> config = config.withStats(new NpcStats(health, config.stats().invulnerable()))
            )
        );
        y += ROW;
        button(onOff("npc.atomicdawn.editor.invulnerable", stats.invulnerable()), y, () -> {
            config = config.withStats(new NpcStats(config.stats().maxHealth(), !config.stats().invulnerable()));
            rebuildWidgets();
        });
        y += ROW + 4;
        labels.add(
            new Label(
                Component.translatable("npc.atomicdawn.editor.dialogue_hint"),
                contentLeft(),
                y,
                PipBoyTheme.faint()
            )
        );
    }

    private void initCombat(int y) {
        var combat = config.combat();
        button(
            Component.translatable(
                "npc.atomicdawn.editor.hostility",
                Component.translatable("npc.atomicdawn.editor.hostility." + combat.hostility().getSerializedName())
            ),
            y,
            () -> {
                config = config.withCombat(config.combat().withHostility(config.combat().hostility().next()));
                rebuildWidgets();
            }
        );
        y += ROW;
        half(
            "npc.atomicdawn.editor.melee",
            y,
            false,
            number(combat.meleeDamage()),
            6,
            value -> parseFloat(value)
                .ifPresent(v -> config = config.withCombat(config.combat().withMelee(v)))
        );
        half(
            "npc.atomicdawn.editor.armor",
            y,
            true,
            number(combat.armor()),
            6,
            value -> parseFloat(value)
                .ifPresent(v -> config = config.withCombat(config.combat().withArmor(v)))
        );
        y += ROW;
        half(
            "npc.atomicdawn.editor.aggro",
            y,
            false,
            number(combat.aggroRange()),
            6,
            value -> parseFloat(value)
                .ifPresent(v -> config = config.withCombat(config.combat().withAggroRange(v)))
        );
        half(
            "npc.atomicdawn.editor.ranged",
            y,
            true,
            number(combat.rangedRange()),
            6,
            value -> parseFloat(value)
                .ifPresent(v -> config = config.withCombat(config.combat().withRangedRange(v)))
        );
        y += ROW;
        half(
            "npc.atomicdawn.editor.spread",
            y,
            false,
            number(combat.aimSpread()),
            6,
            value -> parseFloat(value)
                .ifPresent(v -> config = config.withCombat(config.combat().withAimSpread(v)))
        );
        half(
            "npc.atomicdawn.editor.damage",
            y,
            true,
            number(combat.damageMultiplier()),
            6,
            value -> parseFloat(value)
                .ifPresent(v -> config = config.withCombat(config.combat().withDamageMultiplier(v)))
        );
        y += ROW;
        var half = (panelW - 14) / 2;
        addRenderableWidget(
            Button.builder(onOff("npc.atomicdawn.editor.respawn", combat.respawn()), _ -> {
                var current = config.combat();
                config = config.withCombat(current.withRespawn(!current.respawn(), current.respawnSeconds()));
                rebuildWidgets();
            }).pos(contentLeft(), y).size(half, FIELD_H).build()
        );
        half(
            "npc.atomicdawn.editor.respawn_seconds",
            y,
            true,
            Integer.toString(combat.respawnSeconds()),
            5,
            value -> parseInt(
                value
            ).ifPresent(v -> config = config.withCombat(config.combat().withRespawn(config.combat().respawn(), v)))
        );
        if (isDowned()) {
            y += ROW;
            addRenderableWidget(
                Button.builder(Component.translatable("npc.atomicdawn.editor.respawn_now"), _ -> {
                    send(NpcEditPayload.Action.RESPAWN_NOW);
                    onClose();
                }).pos(contentLeft(), y).size(panelW - 12, FIELD_H).build()
            );
        }
        y += ROW + 4;
        for (var key : new String[] { "npc.atomicdawn.editor.combat_hint", "npc.atomicdawn.editor.combat_hint2" }) {
            for (var line : font.split(Component.translatable(key), panelW - 12)) {
                labels.add(new Label(Component.literal(toPlain(line)), contentLeft(), y, PipBoyTheme.faint()));
                y += 10;
            }
        }
    }

    private static String toPlain(FormattedCharSequence sequence) {
        var builder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    private void initServices(int y) {
        var companion = config.companion();
        button(onOff("npc.atomicdawn.editor.hireable", companion.hireable()), y, () -> {
            var current = config.companion();
            config = config.withCompanion(
                new NpcCompanion(
                    !current.hireable(),
                    current.costItem(),
                    current.costCount(),
                    current.durationMinutes()
                )
            );
            rebuildWidgets();
        });
        y += ROW;
        var cost = field(
            "npc.atomicdawn.editor.hire_item",
            y,
            companion.costItem().map(Identifier::toString).orElse(""),
            128,
            value -> {
                var current = config.companion();
                var item = value.isBlank()
                    ? Optional.<Identifier>empty()
                    : Optional.ofNullable(Identifier.tryParse(value.strip()));
                config = config.withCompanion(
                    new NpcCompanion(current.hireable(), item, current.costCount(), current.durationMinutes())
                );
            }
        );
        cost.setHint(Component.literal("minecraft:emerald"));
        y += ROW;
        half(
            "npc.atomicdawn.editor.hire_count",
            y,
            false,
            Integer.toString(companion.costCount()),
            5,
            value -> parseInt(value)
                .ifPresent(v -> {
                    var current = config.companion();
                    config = config.withCompanion(
                        new NpcCompanion(current.hireable(), current.costItem(), v, current.durationMinutes())
                    );
                })
        );
        half(
            "npc.atomicdawn.editor.hire_minutes",
            y,
            true,
            Integer.toString(companion.durationMinutes()),
            6,
            value -> parseInt(
                value
            ).ifPresent(v -> {
                var current = config.companion();
                config = config.withCompanion(
                    new NpcCompanion(current.hireable(), current.costItem(), current.costCount(), v)
                );
            })
        );
        y += ROW;
        button(
            Component.translatable("npc.atomicdawn.editor.release"),
            y,
            () -> send(NpcEditPayload.Action.RELEASE_COMPANION)
        );
        y += ROW + 6;
        var shop = config.shop();
        labels.add(
            new Label(
                Component.translatable("npc.atomicdawn.editor.shop_trades", shop.trades().size(), NpcShop.MAX_TRADES),
                contentLeft(),
                y,
                PipBoyTheme.text()
            )
        );
        y += 12;
        half(
            "npc.atomicdawn.editor.stock",
            y,
            false,
            Integer.toString(shop.stock()),
            5,
            value -> parseInt(value).ifPresent(
                v -> config = config.withShop(config.shop().withLimits(v, config.shop().restockMinutes()))
            )
        );
        half(
            "npc.atomicdawn.editor.restock",
            y,
            true,
            Integer.toString(shop.restockMinutes()),
            6,
            value -> parseInt(value)
                .ifPresent(v -> config = config.withShop(config.shop().withLimits(config.shop().stock(), v)))
        );
        y += ROW;
        button(
            Component.translatable("npc.atomicdawn.editor.edit_shop"),
            y,
            () -> send(NpcEditPayload.Action.EDIT_SHOP)
        );
        y += ROW + 4;
        for (var line : font.split(Component.translatable("npc.atomicdawn.editor.shop_hint"), panelW - 12)) {
            labels.add(new Label(Component.literal(toPlain(line)), contentLeft(), y, PipBoyTheme.faint()));
            y += 10;
        }
    }

    private void cycleDialogue(List<Identifier> dialogues, int index) {
        Optional<Identifier> next;
        if (dialogues.isEmpty()) {
            next = Optional.empty();
        } else {
            var wrapped = Math.floorMod(index + 1, dialogues.size() + 1) - 1;
            next = wrapped < 0 ? Optional.empty() : Optional.of(dialogues.get(wrapped));
        }
        config = config.withInteraction(new NpcInteraction(next, config.interaction().greeting()));
        rebuildWidgets();
    }

    private void initFooter() {
        var y = panelBottom - ROW - 2;
        var third = (panelW - 16) / 3;
        addRenderableWidget(
            Button.builder(Component.translatable("npc.atomicdawn.editor.save"), _ -> {
                send(NpcEditPayload.Action.SAVE);
                onClose();
            }).pos(contentLeft(), y).size(third, FIELD_H + 2).build()
        );
        addRenderableWidget(
            Button.builder(Component.translatable("npc.atomicdawn.editor.cancel"), _ -> onClose())
                .pos(contentLeft() + third + 2, y)
                .size(third, FIELD_H + 2)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.translatable("npc.atomicdawn.editor.delete"), _ -> {
                send(NpcEditPayload.Action.DELETE);
                onClose();
            }).pos(contentLeft() + 2 * (third + 2), y).size(third, FIELD_H + 2).build()
        );
    }

    private boolean isDowned() {
        var level = minecraft == null ? null : minecraft.level;
        return level != null && level.getEntity(entityId) instanceof NpcEntity npc && npc.isDowned();
    }

    private void send(NpcEditPayload.Action action) {
        ClientPacketDistributor.sendToServer(new NpcEditPayload(entityId, action, config));
    }

    private void setAppearance(NpcAppearance appearance) {
        config = config.withAppearance(appearance);
        NpcClient.preview(entityId, appearance);
    }

    private void setBehavior(NpcBehavior behavior) {
        config = config.withBehavior(behavior);
        NpcClient.previewPose(entityId, behavior.pose());
    }

    private List<Identifier> azureModels() {
        var level = minecraft == null ? null : minecraft.level;
        return level == null ? List.of() : NpcModels.ids(level.registryAccess());
    }

    private List<Identifier> dialogueIds() {
        var level = minecraft == null ? null : minecraft.level;
        return level == null ? List.of() : NpcDialogue.ids(level.registryAccess());
    }

    private List<Identifier> entries() {
        var source = config.appearance().kind() == NpcAppearance.Kind.AZURE ? azureModels() : NpcClient.livingTypes();
        if (filter.isEmpty()) {
            return source;
        }
        return source.stream().filter(id -> id.toString().contains(filter)).toList();
    }

    private static Optional<Float> parseFloat(String value) {
        try {
            var parsed = Float.parseFloat(value.strip());
            return Float.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value.strip()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (tab == Tab.APPEARANCE && inList(event.x(), event.y())) {
            var row = (int) ((event.y() - listY) / LIST_ROW) + scroll;
            var list = entries();
            if (row >= 0 && row < list.size()) {
                setAppearance(config.appearance().withModel(list.get(row)));
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.APPEARANCE && inList(mouseX, mouseY)) {
            var max = Math.max(0, entries().size() - listRows);
            scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * 3L, 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean inList(double x, double y) {
        return x >= listX && x < listX + listW && y >= listY && y < listY + listRows * LIST_ROW;
    }

    @Override
    public void removed() {
        NpcClient.clearPreview(entityId);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left - 1, top - 1, left + panelW + 1, panelBottom + 1, PipBoyTheme.border());
        graphics.fill(left, top, left + panelW, panelBottom, PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xE8));
        for (var label : labels) {
            graphics.text(font, label.text(), label.x(), label.y(), label.color(), false);
        }
        if (tab == Tab.APPEARANCE) {
            renderList(graphics, mouseX, mouseY);
            if (!dataError.getString().isEmpty()) {
                var y = panelBottom - 2 * ROW - 12;
                var message = Component.literal(font.plainSubstrByWidth(dataError.getString(), panelW - 12));
                graphics.text(font, message, contentLeft(), y, PipBoyTheme.bad(), false);
            }
        }
    }

    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(
            listX,
            listY,
            listX + listW,
            listY + listRows * LIST_ROW,
            PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xFF)
        );
        var list = entries();
        var selected = config.appearance().model();
        for (var i = 0; i < listRows; i++) {
            var index = scroll + i;
            if (index >= list.size()) {
                break;
            }
            var id = list.get(index);
            var y = listY + i * LIST_ROW;
            var hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + LIST_ROW;
            if (id.equals(selected)) {
                graphics.fill(listX, y, listX + listW, y + LIST_ROW, PipBoyTheme.selection());
            } else if (hovered) {
                graphics.fill(listX, y, listX + listW, y + LIST_ROW, PipBoyTheme.highlight());
            }
            var text = Component.literal(font.plainSubstrByWidth(id.toString(), listW - 6));
            var color = id.equals(selected) ? PipBoyTheme.ink() : PipBoyTheme.text();
            graphics.text(font, text, listX + 3, y + 2, color, false);
        }
        if (list.isEmpty()) {
            graphics.text(
                font,
                Component.translatable("npc.atomicdawn.editor.empty"),
                listX + 3,
                listY + 2,
                PipBoyTheme.faint(),
                false
            );
        }
    }
}

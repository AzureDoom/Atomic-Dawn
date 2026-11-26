package mod.azure.atomicdawn.camp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;

public final class CampBlueprintFiles {

    public static final String PACK = "atomicdawn_blueprints";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final int MIN_FORMAT = 88;

    private static final int MAX_FORMAT = 9999;

    private CampBlueprintFiles() {}

    public static Path root(MinecraftServer server) {
        return AtomicDawn.CONFIG.common.settlement.camp.blueprints.CAMP_BLUEPRINT_EXPORT_TO_DATAPACKS
            ? server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK)
            : server.getWorldPath(LevelResource.ROOT).resolve(PACK);
    }

    public static Identifier exportId(String owner, String key) {
        var folder = CampBlueprints.slug(owner);
        return AtomicDawn.id((folder.isEmpty() ? "shared" : folder) + "/" + key);
    }

    public static Optional<Path> write(MinecraftServer server, Identifier id, CampBlueprint blueprint) {
        var root = root(server);
        var file = root.resolve("data")
            .resolve(id.getNamespace())
            .resolve(AtomicDawn.MOD_ID)
            .resolve("camp_blueprint")
            .resolve(id.getPath() + ".json");
        try {
            var json = CampBlueprint.CODEC.encodeStart(JsonOps.INSTANCE, blueprint).getOrThrow();
            writeMeta(root);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
            var world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            return Optional.of(world.relativize(file.toAbsolutePath().normalize()));
        } catch (IOException | RuntimeException exception) {
            AtomicDawn.LOGGER.error("Could not write C.A.M.P. blueprint {}", id, exception);
            return Optional.empty();
        }
    }

    private static void writeMeta(Path root) throws IOException {
        var meta = root.resolve("pack.mcmeta");
        if (Files.exists(meta)) {
            return;
        }
        var pack = new JsonObject();
        pack.addProperty("description", "Atomic Dawn C.A.M.P. blueprints");
        pack.addProperty("min_format", MIN_FORMAT);
        pack.addProperty("max_format", MAX_FORMAT);
        var json = new JsonObject();
        json.add("pack", pack);
        Files.createDirectories(root);
        Files.writeString(meta, GSON.toJson(json), StandardCharsets.UTF_8);
    }
}

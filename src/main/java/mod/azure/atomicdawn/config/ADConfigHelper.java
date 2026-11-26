package mod.azure.atomicdawn.config;

import mod.azure.azurelib.config.io.ConfigIO;
import mod.azure.azurelib.config.value.BooleanValue;
import mod.azure.azurelib.config.value.ConfigValue;
import mod.azure.azurelib.config.value.DoubleValue;
import mod.azure.azurelib.config.value.ObjectValue;

import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADConfigHelper {

    private static final String CLIENT_ROOT = "client";

    private ADConfigHelper() {}

    private static ConfigValue<?> clientValue(String path) {
        ConfigValue<?> current = AtomicDawn.CONFIG_HOLDER.getValueMap().get(CLIENT_ROOT);
        if (!(current instanceof ObjectValue)) {
            throw new IllegalStateException("AzureLib config group '" + CLIENT_ROOT + "' is missing");
        }
        for (String key : path.split("\\.")) {
            if (!(current instanceof ObjectValue objectValue)) {
                throw new IllegalArgumentException("Client config path does not name a group: " + path);
            }
            Map<String, ConfigValue<?>> values = objectValue.get();
            current = values.get(key);
            if (current == null) {
                throw new IllegalArgumentException("Unknown client config option: " + path);
            }
        }
        return current;
    }

    public static void setClientDouble(String path, double value) {
        if (!(clientValue(path) instanceof DoubleValue configValue)) {
            throw new IllegalArgumentException("Client config option is not a double: " + path);
        }
        configValue.set(value);
        saveClient();
    }

    public static void setClientBoolean(String path, boolean value) {
        if (!(clientValue(path) instanceof BooleanValue configValue)) {
            throw new IllegalArgumentException("Client config option is not a boolean: " + path);
        }
        configValue.set(value);
        saveClient();
    }

    private static void saveClient() {
        ConfigIO.saveClientValues(AtomicDawn.CONFIG_HOLDER);
    }
}

package mod.azure.atomicdawn.config;

import mod.azure.azurelib.config.io.ConfigIO;
import mod.azure.azurelib.config.value.BooleanValue;
import mod.azure.azurelib.config.value.ConfigValue;
import mod.azure.azurelib.config.value.DoubleValue;
import mod.azure.azurelib.config.value.ObjectValue;

import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADConfigHelper {

    private ADConfigHelper() {}

    private static ConfigValue<?> clientValue(String key) {
        ConfigValue<?> group = AtomicDawn.CONFIG_HOLDER.getValueMap().get("clientPipboy");
        if (!(group instanceof ObjectValue objectValue)) {
            throw new IllegalStateException("AzureLib config group 'clientPipboy' is missing");
        }
        Map<String, ConfigValue<?>> values = objectValue.get();
        ConfigValue<?> value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown Pip-Boy config option: " + key);
        }
        return value;
    }

    public static void setClientDouble(String key, double value) {
        if (!(clientValue(key) instanceof DoubleValue configValue)) {
            throw new IllegalArgumentException("Pip-Boy config option is not a double: " + key);
        }
        configValue.set(value);
        saveClient();
    }

    public static void setClientBoolean(String key, boolean value) {
        if (!(clientValue(key) instanceof BooleanValue configValue)) {
            throw new IllegalArgumentException("Pip-Boy config option is not a boolean: " + key);
        }
        configValue.set(value);
        saveClient();
    }

    private static void saveClient() {
        ConfigIO.saveClientValues(AtomicDawn.CONFIG_HOLDER);
    }
}

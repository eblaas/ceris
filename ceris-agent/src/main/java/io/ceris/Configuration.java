package io.ceris;

import com.google.common.collect.Maps;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.ceris.init.ResourcesInitializer.getDemoInitResources;
import static io.ceris.plugin.EnvConfigProvider.CERIS_SECRET_PREFIX;

public class Configuration {

    static final String CONNECT_ENV_PREFIX = "CONNECT_";
    static final String SCHEMA_REGISTRY_ENV_PREFIX = "SCHEMA_REGISTRY_";
    static final String KAFKA_ENV_PREFIX = "KAFKA_";

    private static final Predicate<Map.Entry<String, ?>> CONNECT_ENV_FILTER = e -> e.getKey()
            .startsWith(CONNECT_ENV_PREFIX);
    private static final Predicate<Map.Entry<String, ?>> SCHEMA_REGISTRY_ENV_FILTER = e -> e.getKey()
            .startsWith(SCHEMA_REGISTRY_ENV_PREFIX);
    private static final Predicate<Map.Entry<String, ?>> KAFKA_ENV_FILTER = e -> e.getKey()
            .startsWith(KAFKA_ENV_PREFIX);

    private final Map<String, String> envVariables = new HashMap<>();

    public Configuration() {
        this(Collections.emptyMap());
    }

    public Configuration(Map<String, String> overwrite) {
        envVariables.putAll(overwrite);

        envVariables.putAll(System.getenv());
        for (Map.Entry<String, String> entry : Maps.fromProperties(System.getProperties()).entrySet()) {
            envVariables.put(entry.getKey().toUpperCase().replace(".", "_"), entry.getValue());
        }

        if (envVariables.containsKey("CERIS_DEMO")) {
            envVariables.putIfAbsent("CERIS_INIT_RESOURCES", getDemoInitResources());
        }

        //ceris config
        envVariables.putIfAbsent("CERIS_EMBEDDED_ENABLED", "true");
        envVariables.putIfAbsent("CERIS_EMBEDDED_KAFKA_PORT", "9092");
        envVariables.putIfAbsent("CERIS_EMBEDDED_DATA_PATH", "data");
        //envVariables.putIfAbsent("CERIS_EMBEDDED_PLUGINS_INSTALL", "confluentinc/kafka-connect-datagen:0.6.0");

        envVariables.putIfAbsent("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        envVariables.putIfAbsent("KAFKA_SCHEMA_REGISTRY_URL", "http://localhost:8081");

        envVariables.putIfAbsent("CERIS_CONNECT_URL", "http://localhost:8083");
        envVariables.putIfAbsent("CERIS_API_PORT", "4567");
        envVariables.putIfAbsent("CERIS_ROOT_LOGLEVEL", "WARN");
        envVariables.putIfAbsent("CERIS_JMX_EXPORT_CONFIG", "classpath:kafka-connect-jmx.yml");

        // authentication
        envVariables.putIfAbsent("CERIS_AUTH_ENABLED", "true");
        envVariables.putIfAbsent("CERIS_AUTH_JWT_SECRET_KEY", "ceris_auth_secret_key");
        envVariables.putIfAbsent("CERIS_AUTH_JWT_EXPIRATION", "PT24H");
        envVariables.putIfAbsent("CERIS_AUTH_USERS", "admin:admin:ADMIN,user:user:USER");

        // default secrets
        System.setProperty(CERIS_SECRET_PREFIX + "KAFKA_BOOTSTRAP_SERVERS",
                           envVariables.get("KAFKA_BOOTSTRAP_SERVERS"));
        System.setProperty(CERIS_SECRET_PREFIX + "SCHEMA_REGISTRY_URL",
                           envVariables.get("KAFKA_SCHEMA_REGISTRY_URL"));

        envVariables.putIfAbsent("CONNECT_BOOTSTRAP_SERVERS", "localhost:9092");
        envVariables.putIfAbsent("CONNECT_LISTENERS", "http://0.0.0.0:8083");
        envVariables.putIfAbsent("CONNECT_GROUP_ID", "kafka-connect");
        envVariables.putIfAbsent("CONNECT_CONFIG_STORAGE_TOPIC", "_connect-configs");
        envVariables.putIfAbsent("CONNECT_OFFSET_STORAGE_TOPIC", "_connect-offsets");
        envVariables.putIfAbsent("CONNECT_STATUS_STORAGE_TOPIC", "_connect-status");
        envVariables.putIfAbsent("CONNECT_OFFSET_STORAGE_PARTITIONS", "3");
        envVariables.putIfAbsent("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1");
        envVariables.putIfAbsent("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1");
        envVariables.putIfAbsent("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1");
        envVariables.putIfAbsent("CONNECT_KEY_CONVERTER", "io.confluent.connect.avro.AvroConverter");
        envVariables.putIfAbsent("CONNECT_VALUE_CONVERTER", "io.confluent.connect.avro.AvroConverter");
        envVariables.putIfAbsent("CONNECT_KEY_CONVERTER_SCHEMA_REGISTRY_URL", "http://localhost:8081");
        envVariables.putIfAbsent("CONNECT_VALUE_CONVERTER_SCHEMA_REGISTRY_URL", "http://localhost:8081");
        envVariables.putIfAbsent("CONNECT_PLUGIN_PATH",
                                 Path.of(envVariables.get("CERIS_EMBEDDED_DATA_PATH"), "connect/plugins").toString());

        envVariables.put("CONNECT_CONFIG_PROVIDERS", "env");
        envVariables.put("CONNECT_CONFIG_PROVIDERS_ENV_CLASS", "io.ceris.plugin.EnvConfigProvider");
        envVariables.putIfAbsent("CONNECT_LOG4J_ROOT_LOGLEVEL", "INFO");

        //schema registry
        envVariables.putIfAbsent("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "localhost:9092");
        envVariables.putIfAbsent("SCHEMA_REGISTRY_GROUP_ID", "schema-registry");
        envVariables.putIfAbsent("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
        envVariables.putIfAbsent("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081");
        envVariables.putIfAbsent("SCHEMA_REGISTRY_LOG4J_ROOT_LOGLEVEL", "INFO");
    }

    public String get(String key) {
        return envVariables.get(key);
    }

    public String get(String key, String defaultValue) {
        return envVariables.getOrDefault(key, defaultValue);
    }

    public boolean isEmbeddedEnv() {
        return Boolean.parseBoolean(get("CERIS_EMBEDDED_ENABLED"));
    }

    public boolean isAuthEnabled() {
        return Boolean.parseBoolean(get("CERIS_AUTH_ENABLED"));
    }

    private static String envVarToProp(String k, String prefix) {
        if (k == null || k.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        if (k.length() < prefix.length() || k.equals(prefix)) {
            throw new IllegalArgumentException(
                    "Input does not start with '" + prefix + "' or does not define a property");
        }
        return k.toLowerCase().substring(prefix.length()).replace('_', '.');
    }


    public Map<String, String> createConnectProperties() {

        return envVariables.entrySet()
                .stream()
                .filter(CONNECT_ENV_FILTER)
                .map(e -> new AbstractMap.SimpleEntry<>(envVarToProp(e.getKey(), CONNECT_ENV_PREFIX), e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    public Map<String, String> createSchemaRegistryProperties() {

        return envVariables.entrySet()
                .stream()
                .filter(SCHEMA_REGISTRY_ENV_FILTER)
                .map(e -> new AbstractMap.SimpleEntry<>(envVarToProp(e.getKey(), SCHEMA_REGISTRY_ENV_PREFIX),
                                                        e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    public Map<String, Object> createKafkaProperties() {

        return envVariables.entrySet()
                .stream()
                .filter(KAFKA_ENV_FILTER)
                .map(e -> new AbstractMap.SimpleEntry<>(envVarToProp(e.getKey(), KAFKA_ENV_PREFIX), e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

}

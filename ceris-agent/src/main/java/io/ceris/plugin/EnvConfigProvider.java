package io.ceris.plugin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.provider.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class EnvConfigProvider implements ConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(EnvConfigProvider.class);
    public static final String CERIS_SECRET_PREFIX = "CERIS_SECRET_";

    private final Map<String, String> secrets;

    public EnvConfigProvider() {
        ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
        // add env vars
        mapBuilder.putAll(Maps.filterKeys(System.getenv(), key -> key.startsWith(CERIS_SECRET_PREFIX)));
        // add system properties
        System.getProperties().stringPropertyNames().stream()
                .filter(key -> key.startsWith(CERIS_SECRET_PREFIX))
                .forEach(key -> mapBuilder.put(key, System.getProperties().getProperty(key)));
        secrets = mapBuilder.build();
    }

    public Collection<String> getKeys() {
        return secrets.keySet();
    }

    @Override
    public ConfigData get(String path) {
        return new ConfigData(secrets);
    }

    @Override
    public ConfigData get(String path, Set<String> keys) {
        return new ConfigData(Maps.filterKeys(secrets, keys::contains));
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
        log.debug("received config parameters: {}", configs);
    }

}
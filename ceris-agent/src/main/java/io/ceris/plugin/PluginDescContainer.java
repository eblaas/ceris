package io.ceris.plugin;

import com.google.common.base.Preconditions;
import io.confluent.pluginregistry.rest.entities.PluginManifest;
import org.apache.kafka.connect.runtime.isolation.PluginDesc;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class PluginDescContainer {

    private final UUID id;
    private final PluginDesc<?> desc;
    private final Optional<PluginManifest> manifest;

    public PluginDescContainer(PluginDesc<?> desc, Optional<PluginManifest> manifest) {
        this.desc = Preconditions.checkNotNull(desc);
        this.manifest = Preconditions.checkNotNull(manifest);
        this.id = manifest.isPresent()
                ? UUID.nameUUIDFromBytes(desc.location().getBytes(StandardCharsets.UTF_8))
                : null;
    }

    public UUID getId() {
        return id;
    }

    public String getClazz() {
        return desc.className();
    }

    public PluginDesc<?> getDesc() {
        return desc;
    }

    public Optional<PluginManifest> getManifest() {
        return manifest;
    }
}

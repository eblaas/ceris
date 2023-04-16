package io.ceris.apicall.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import io.ceris.plugin.PluginDescContainer;
import io.confluent.pluginregistry.rest.entities.PluginManifest;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPlugin;

import java.util.UUID;

public class ConnectPluginDto {

    private UUID id;
    @JsonProperty("class")
    private String className;
    private final String type;
    private final String version;
    private final String location;
    private final PluginManifestDto manifest;


    public ConnectPluginDto(PluginDescContainer pluginDescContainer) {
        this.id = pluginDescContainer.getId();
        this.className = pluginDescContainer.getClazz();
        this.type = pluginDescContainer.getDesc().typeName();
        this.version = MoreObjects.firstNonNull(
                Strings.emptyToNull(pluginDescContainer.getDesc().version().replace("null", "")),
                pluginDescContainer.getManifest().map(PluginManifest::getVersion).orElse(null));
        this.location = pluginDescContainer.getDesc().location();
        this.manifest = pluginDescContainer.getManifest().map(PluginManifestDto::new).orElse(null);
    }

    public ConnectPluginDto(ConnectorPlugin connectorPlugin) {
        this.className = connectorPlugin.getClassName();
        this.type = connectorPlugin.getType();
        this.version = connectorPlugin.getVersion();
        this.location = null;
        this.manifest = null;
        this.id = null;
    }

    public UUID getId() {
        return id;
    }

    public String getClassName() {
        return this.className;
    }

    public String getType() {
        return this.type;
    }

    public String getVersion() {
        return this.version;
    }

    public String getLocation() {
        return location;
    }

    public PluginManifestDto getManifest() {
        return manifest;
    }
}

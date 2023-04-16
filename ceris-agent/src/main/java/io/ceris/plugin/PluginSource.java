package io.ceris.plugin;

import io.confluent.connect.hub.cli.ExitCode;
import io.confluent.pluginregistry.PluginId;
import io.confluent.pluginregistry.rest.entities.PluginManifest;
import org.apache.kafka.connect.runtime.isolation.PluginDesc;

import java.util.Collection;
import java.util.List;


public interface PluginSource {

    boolean canHandle(PluginId pluginId);

    Collection<PluginManifest> availablePlugins();

    List<PluginDescContainer> getPluginManifest(List<PluginDesc<?>> pluginDescriptions);

    ExitCode install(PluginId pluginId);

    void installJars(PluginDesc<?> desc, List<String> jars);

}

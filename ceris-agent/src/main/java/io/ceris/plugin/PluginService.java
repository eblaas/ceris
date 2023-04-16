package io.ceris.plugin;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Streams;
import io.ceris.Configuration;
import io.ceris.apicall.ApiCallError;
import io.ceris.apicall.ConnectClients;
import io.ceris.apicall.dto.PluginInstall;
import io.ceris.embedded.EmbeddedKafkaConnect;
import io.confluent.connect.hub.cli.ExitCode;
import io.confluent.connect.hub.utils.NamingUtils;
import io.confluent.pluginregistry.PluginId;
import io.confluent.pluginregistry.rest.entities.PluginManifest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.connect.runtime.isolation.PluginDesc;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);

    private static final String MOCK_PACKAGE = "org.apache.kafka.connect.tools.";

    private enum Key {INSTANCE}

    private final LoadingCache<Key, List<PluginDescContainer>> pluginCache;

    private final EmbeddedKafkaConnect embeddedKafkaConnect;
    private final Configuration configuration;
    private final ConnectClients clients;
    private final List<PluginSource> pluginSources;
    private final String pluginPath;


    public PluginService(Configuration configuration, List<PluginSource> pluginSources, ConnectClients clients) {
        this(configuration, pluginSources, clients, null);
    }

    public PluginService(Configuration configuration, List<PluginSource> pluginSources, ConnectClients clients,
                         EmbeddedKafkaConnect embeddedKafkaConnect) {

        this.configuration = configuration;
        this.pluginSources = pluginSources;
        this.clients = clients;
        this.embeddedKafkaConnect = embeddedKafkaConnect;

        this.pluginPath = configuration.get("CONNECT_PLUGIN_PATH");
        this.pluginCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
            @Override
            public List<PluginDescContainer> load(@NotNull Key key) {
                Map<Boolean, List<PluginDesc<?>>> pluginsInPath = getInstalledPluginDesc().stream()
                        .collect(Collectors.partitioningBy(pluginDesc -> pluginDesc.location().contains(pluginPath)));

                List<PluginDesc<?>> installed = pluginsInPath.get(true);
                List<PluginDesc<?>> inClassPath = pluginsInPath.get(false);

                return Stream.concat(
                                pluginSources.stream().flatMap(source -> source.getPluginManifest(installed).stream()),
                                inClassPath.stream().map(desc -> new PluginDescContainer(desc, Optional.empty())))
                        .toList();
            }
        });
    }

    private PluginSource sourceHandlerFor(PluginId pluginId) {
        return pluginSources.stream()
                .filter(pluginSource -> pluginSource.canHandle(pluginId))
                .findFirst()
                .orElseThrow(() -> new ApiCallError(500, "Unsupported pluginId=" + pluginId));
    }

    private List<PluginDesc<?>> getInstalledPluginDesc() {
        Plugins plugins = new Plugins(configuration.createConnectProperties());
        return Streams.concat(
                        plugins.sinkConnectors().stream().filter(c -> !c.className().contains(MOCK_PACKAGE)),
                        plugins.sourceConnectors().stream().filter(c -> !c.className().contains(MOCK_PACKAGE)),
                        plugins.transformations().stream(),
                        plugins.converters().stream(),
                        plugins.predicates().stream())
                .toList();
    }

    public Collection<PluginDescContainer> getInstalledPlugins() {
        return pluginCache.getUnchecked(Key.INSTANCE);
    }

    public List<PluginId> getInstalledPluginIds() {
        return pluginCache.getUnchecked(Key.INSTANCE).stream()
                .filter(d -> d.getManifest().isPresent())
                .map(d -> d.getManifest().get().getPluginId())
                .toList();
    }

    public void installJars(PluginId pluginId, List<String> jars) {
        if (CollectionUtils.isNotEmpty(jars)) {
            getInstalledPlugins().stream()
                    .filter(d -> d.getManifest().isPresent())
                    .filter(d -> Objects.equals(d.getManifest().get().getPluginId(), pluginId))
                    .findFirst()
                    .ifPresent(c -> sourceHandlerFor(pluginId).installJars(c.getDesc(), jars));
        }
    }

    public ExitCode install(PluginInstall pluginInstall, boolean restart, boolean force) {

        PluginId pluginId = NamingUtils.parsePluginId(pluginInstall.pluginId());

        log.info("Check if plugin is installed ... pluginId={}", pluginId);

        if (force || !getInstalledPluginIds().contains(pluginId)) {
            log.info("Installing plugin ... pluginId={}", pluginId);
            ExitCode exitCode = sourceHandlerFor(pluginId).install(pluginId);
            log.info("Installation finished. code={}, pluginId={}", exitCode.getCode(), pluginId);
            if (exitCode.equals(ExitCode.SUCCESSFUL_COMPLETION)) {
                installJars(pluginId, pluginInstall.jars());
                if (restart) {
                    restartKafkaConnect();
                }
                pluginCache.invalidateAll();
                return exitCode;
            }
        }

        return ExitCode.ALREADY_INSTALLED;
    }

    public void uninstall(String id) {

        List<PluginDescContainer> matchingPlugins = pluginCache.getUnchecked(Key.INSTANCE).stream()
                .filter(container -> Objects.equals(container.getId(), UUID.fromString(id)))
                .toList();

        Set<String> classesToDelete = matchingPlugins.stream()
                .map(PluginDescContainer::getClazz)
                .collect(Collectors.toSet());

        Set<String> existingConnectors = clients.getJavaClient().getConnectorsWithAllExpandedMetadata()
                .getAllDefinitions()
                .stream()
                .filter(definition -> classesToDelete.contains(definition.getConfig().get("connector.class")))
                .map(ConnectorDefinition::getName)
                .collect(Collectors.toSet());

        if (!existingConnectors.isEmpty()) {
            throw new ApiCallError(400,
                                   "Plugin can't be uninstalled because of exising connectors=" + existingConnectors);
        }

        matchingPlugins.stream()
                .findFirst()
                .ifPresent(container -> {
                    log.info("Uninstall plugin={}", container.getManifest().get().getPluginId());
                    try {
                        Path pathToDelete = Paths.get(new URI(container.getDesc().location()).getPath());
                        Files.walk(pathToDelete).sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        restartKafkaConnect();
                    } catch (Exception e) {
                        log.error("Failed to delete plugin={}", container.getManifest().get().getPluginId());
                        throw new ApiCallError(500, "Unable to delete plugin");
                    }
                    pluginCache.invalidateAll();
                });
    }

    public Optional<PluginDescContainer> getPluginDescription(String className) {
        return pluginCache.getUnchecked(Key.INSTANCE).stream()
                .filter(pluginDescContainer -> pluginDescContainer.getClazz().equals(className))
                .findFirst();
    }

    public Collection<PluginManifest> getAvailablePlugins() {
        return pluginSources.stream()
                .flatMap(pluginSource -> pluginSource.availablePlugins().stream())
                .toList();
    }

    private void restartKafkaConnect() {
        if (configuration.isEmbeddedEnv() && Objects.nonNull(embeddedKafkaConnect)) {
            embeddedKafkaConnect.restart();
        }
    }
}

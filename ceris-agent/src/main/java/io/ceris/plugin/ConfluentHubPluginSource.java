package io.ceris.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.ceris.Configuration;
import io.ceris.apicall.ApiCallError;
import io.confluent.connect.hub.actions.ConfluentHubController;
import io.confluent.connect.hub.cli.ExitCode;
import io.confluent.connect.hub.io.Storage;
import io.confluent.connect.hub.io.StorageAdapter;
import io.confluent.connect.hub.rest.PluginRegistryRepository;
import io.confluent.connect.hub.utils.NamingUtils;
import io.confluent.pluginregistry.PluginId;
import io.confluent.pluginregistry.rest.entities.PluginManifest;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.connect.runtime.isolation.PluginDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.resource.ClassPathResource;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ConfluentHubPluginSource extends ConfluentHubController implements PluginSource {

    private static final Logger log = LoggerFactory.getLogger(ConfluentHubPluginSource.class);
    private static final String HUB_CONFLUENT_IO = "https://api.hub.confluent.io";
    public static final String PLUGINS_CONFLUENT_HUB_FILE = "plugins-confluent-hub.json";
    public static final String MAVEN_ORG = "https://repo.maven.apache.org/maven2";

    private enum Key {INSTANCE}

    private final Storage storage;
    private final String pluginPath;
    private final ObjectMapper mapper;
    private final Cache<Key, Collection<PluginManifest>> cache;

    public ConfluentHubPluginSource(Configuration configuration, ObjectMapper mapper) {

        super(new StorageAdapter(false), new PluginRegistryRepository(HUB_CONFLUENT_IO));
        this.mapper = mapper;
        this.storage = new StorageAdapter(false);
        this.pluginPath = configuration.get("CONNECT_PLUGIN_PATH");
        this.cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
    }

    @Override
    public boolean canHandle(PluginId pluginId) {
        return !"lensesio".equalsIgnoreCase(pluginId.owner());
    }

    @Override
    public Collection<PluginManifest> availablePlugins() {
        try {
            return cache.get(Key.INSTANCE, this::availablePluginsFromHub);
        } catch (Exception e) {
            log.warn("Failed to load confluent hub plugin list", e);
            try {
                ClassPathResource classPathResource = new ClassPathResource(PLUGINS_CONFLUENT_HUB_FILE);
                return mapper.readValue(classPathResource.getInputStream(), new TypeReference<>() {});
            } catch (IOException ex) {
                log.error("Failed to load confluent plugin list from class path", e);
                throw new ApiCallError(500, "Failed to load confluent plugin list");
            }
        }
    }

    private Collection<PluginManifest> availablePluginsFromHub() throws Exception {
        try (Response response = ClientBuilder.newBuilder().build()
                .target(HUB_CONFLUENT_IO)
                .path("api/plugins")
                .queryParam("per_page", 1000)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()) {
            Preconditions.checkState(response.getStatus() == Response.Status.OK.getStatusCode());
            return mapper.readValue(response.readEntity(InputStream.class), new TypeReference<>() {});
        }
    }

    @Override
    public List<PluginDescContainer> getPluginManifest(List<PluginDesc<?>> pluginDescriptions) {
        return pluginDescriptions.stream()
                .map(pluginDesc -> {
                    try {
                        Path path = Paths.get(new URI(pluginDesc.location()));
                        PluginManifest pluginManifest =
                                storage.loadManifest(NamingUtils.getManifestPath(path.toString()));
                        return new PluginDescContainer(pluginDesc, Optional.of(pluginManifest));
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }


    public ExitCode install(PluginId pluginId) {

        return doInstallFromHub(pluginId, pluginPath, (s) -> true, (s) -> true, (l) -> true);
    }

    public void installJars(PluginDesc<?> desc, List<String> jars) {

        for (String jar : jars) {

            String[] parts = jar.split(":");
            Preconditions.checkArgument(parts.length == 3);
            String groupPart = parts[0].replace('.', '/');
            String namePart = parts[1];
            String versionPart = parts[2];
            String jarUrl = String.format("%s/%s/%s/%s/%s-%s.jar", MAVEN_ORG,
                                          groupPart, namePart, versionPart,
                                          namePart, versionPart);

            try {
                String fileName = String.format("%s-%s.jar", namePart, versionPart);
                Path pluginPath = Paths.get(new URI(desc.location()));
                try (Stream<Path> stream = Files.walk(pluginPath.resolve("lib"), 1)) {
                    if (stream.map(p -> p.getFileName().toString()).noneMatch(file -> file.startsWith(namePart))) {
                        Path outputPath = pluginPath.resolve("lib").resolve(fileName);
                        Files.createDirectories(outputPath.getParent());
                        log.info("Download jar. source={}, destination={}", jarUrl, outputPath);
                        FileOutputStream outputStream = new FileOutputStream(outputPath.toFile());
                        IOUtils.copyLarge(new URL(jarUrl).openStream(), outputStream);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to download jar", e);
            }
        }
    }
}

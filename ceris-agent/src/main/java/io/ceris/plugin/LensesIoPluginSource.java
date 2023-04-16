package io.ceris.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ceris.Configuration;
import io.ceris.apicall.ApiCallError;
import io.confluent.connect.hub.cli.ExitCode;
import io.confluent.connect.hub.io.StorageAdapter;
import io.confluent.connect.hub.utils.NamingUtils;
import io.confluent.pluginregistry.PluginId;
import io.confluent.pluginregistry.rest.entities.PluginManifest;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.kafka.connect.runtime.isolation.PluginDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.resource.ClassPathResource;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LensesIoPluginSource implements PluginSource {

    private static final Logger log = LoggerFactory.getLogger(LensesIoPluginSource.class);
    public static final String PLUGINS_LENSESIO_PATH = "plugins-lensesio.json";
    public static final String LENSESIO_OWNER = "lensesio";

    private final ObjectMapper mapper;
    private final StorageAdapter storageAdapter;
    private final String pluginPath;

    public LensesIoPluginSource(Configuration configuration, ObjectMapper mapper) {
        this.mapper = mapper;
        this.storageAdapter = new StorageAdapter(false);
        this.pluginPath = configuration.get("CONNECT_PLUGIN_PATH");
    }

    @Override
    public boolean canHandle(PluginId pluginId) {
        return LENSESIO_OWNER.equalsIgnoreCase(pluginId.owner());
    }

    @Override
    public Collection<PluginManifest> availablePlugins() {
        try {
            ClassPathResource json = new ClassPathResource(PLUGINS_LENSESIO_PATH);
            return mapper.readValue(json.getInputStream(), new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Failed to load lensesio plugin list", e);
            throw new ApiCallError(500, "Failed to load lensesio plugin list");
        }
    }

    @Override
    public List<PluginDescContainer> getPluginManifest(List<PluginDesc<?>> pluginDescriptions) {
        Collection<PluginManifest> pluginManifests = availablePlugins();
        return pluginDescriptions.stream()
                .flatMap(pluginDesc -> pluginManifests.stream()
                        .filter(manifest -> manifest.getRequirements().contains(pluginDesc.className()))
                        .findFirst()
                        .map(manifest -> new PluginDescContainer(pluginDesc, Optional.of(manifest)))
                        .stream())
                .toList();
    }

    @Override
    public ExitCode install(PluginId pluginId) {
        return availablePlugins().stream()
                .filter(manifest -> Objects.equals(pluginId, manifest.getPluginId()))
                .findFirst()
                .map(manifest -> {
                    File extractToDir = storageAdapter.createTmpDir("lensesio-tmp");
                    String zipFilePath = NamingUtils.getArchivePath(extractToDir.getPath(), manifest);
                    URI archiveUri = NamingUtils.getArchiveUri(manifest.getArchive());
                    Client httpClient = ClientBuilder.newBuilder().build();
                    try {
                        Response response = httpClient.target(archiveUri).request().get();
                        if (response.getStatus() == HttpStatus.SC_OK) {
                            FileOutputStream zipFile = new FileOutputStream(zipFilePath);
                            IOUtils.copyLarge(response.readEntity(InputStream.class), zipFile);
                            storageAdapter.unzip(new File(zipFilePath), new File(pluginPath));
                            return ExitCode.SUCCESSFUL_COMPLETION;
                        } else {
                            return ExitCode.IS_NOT_AVAILABLE_FOR_INSTALLATION;
                        }
                    } catch (Exception e) {
                        log.error("Failed to install plugin={}", pluginId, e);
                        return ExitCode.UNKNOWN_ERROR;
                    } finally {
                        storageAdapter.delete(extractToDir.getPath());
                        httpClient.close();
                    }
                })
                .orElse(ExitCode.COMPONENT_NOT_FOUND);
    }

    @Override
    public void installJars(PluginDesc<?> desc, List<String> jars) {
        throw new ApiCallError(400, "Jars installation ot supported");
    }
}

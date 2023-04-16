package io.ceris.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.ceris.Configuration;
import io.ceris.apicall.ConnectClients;
import org.apache.logging.log4j.util.Strings;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition;
import spark.resource.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

public class ResourcesInitializer implements Startable {

    private static final Logger log = LoggerFactory.getLogger(ResourcesInitializer.class);

    private final Configuration configuration;
    private final ObjectMapper objectMapper;
    private final ConnectClients clients;

    public ResourcesInitializer(Configuration configuration, ObjectMapper objectMapper, ConnectClients clients) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.clients = clients;
    }

    public static String getDemoInitResources() {
        try {
            InputStream initResources = new ClassPathResource("demo-init-resources.json").getInputStream();
            return new String(initResources.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {

        log.info("Starting resource initialization ...");

        String initResources = configuration.get("CERIS_INIT_RESOURCES");

        if (Strings.isNotEmpty(initResources)) {
            try {
                InitResources resources = objectMapper.readValue(initResources, InitResources.class);

                KafkaConnectClient javaClient = clients.getJavaClient();
                Collection<String> connectors = javaClient.getConnectors();

                for (Map<String, String> config : resources.connectors()) {

                    String name = Preconditions.checkNotNull(config.get("name"), "missing name property in config");

                    if (!connectors.contains(name)) {
                        log.info("Create new connector name={}", name);
                        javaClient.addConnector(NewConnectorDefinition.newBuilder()
                                                        .withName(name)
                                                        .withConfig(config)
                                                        .build());
                    } else {
                        Map<String, String> currentConfig = javaClient.getConnectorConfig(name);
                        if (!currentConfig.entrySet().containsAll(config.entrySet())) {
                            log.info("Update connector name={}", name);
                            javaClient.updateConnectorConfig(name, config);
                        }
                    }

                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse CERIS_INIT_RESOURCES", e);
            } catch (Exception e) {
                log.warn("Failed to init resources", e);
            }
        }

        log.info("Resource initialization done");
    }

    @Override
    public void stop() {

    }
}

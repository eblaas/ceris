package io.ceris.embedded;

import io.ceris.Configuration;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import org.apache.commons.collections4.MapUtils;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmbeddedSchemaRegistry {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSchemaRegistry.class);

    private final Configuration configuration;
    private Server server;

    public EmbeddedSchemaRegistry(Configuration configuration) {
        this.configuration = configuration;
    }

    public void start() {
        log.info("Starting schema registry ...");

        try {
            Properties properties = MapUtils.toProperties(configuration.createSchemaRegistryProperties());
            SchemaRegistryConfig config = new SchemaRegistryConfig(properties);
            SchemaRegistryRestApplication app = new SchemaRegistryRestApplication(config);
            server = app.createServer();
            server.start();

            log.info("Starting schema registry done");
        } catch (Exception e) {
            log.error("Failed to start schema registry", e);
            throw new StartupException(e);
        }
    }

    public void stop() {
        try {
            if (server != null) {
                log.info("Stopping schema registry ...");
                server.stop();
                log.info("Stopping schema registry done");
            }
        } catch (Exception e) {
            log.error("Failed to stop schema registry", e);
        }
    }
}

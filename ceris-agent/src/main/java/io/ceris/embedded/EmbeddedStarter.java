package io.ceris.embedded;

import org.picocontainer.Startable;

public class EmbeddedStarter implements Startable {

    private final EmbeddedKafka embeddedKafka;
    private final EmbeddedPluginInstaller embeddedPluginInstaller;
    private final EmbeddedSchemaRegistry embeddedSchemaRegistry;
    private final EmbeddedKafkaConnect embeddedKafkaConnect;

    public EmbeddedStarter(EmbeddedKafka embeddedKafka,
                           EmbeddedPluginInstaller embeddedPluginInstaller,
                           EmbeddedSchemaRegistry embeddedSchemaRegistry,
                           EmbeddedKafkaConnect embeddedKafkaConnect) {
        this.embeddedKafka = embeddedKafka;
        this.embeddedPluginInstaller = embeddedPluginInstaller;
        this.embeddedSchemaRegistry = embeddedSchemaRegistry;
        this.embeddedKafkaConnect = embeddedKafkaConnect;
    }

    @Override
    public void start() {
        embeddedKafka.start();
        embeddedPluginInstaller.start();
        embeddedSchemaRegistry.start();
        embeddedKafkaConnect.start();
    }

    @Override
    public void stop() {
        embeddedKafkaConnect.stop();
        embeddedSchemaRegistry.stop();
        embeddedKafka.stop();
    }
}

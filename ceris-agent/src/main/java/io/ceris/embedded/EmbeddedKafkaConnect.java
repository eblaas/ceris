package io.ceris.embedded;

import io.ceris.Configuration;
import org.apache.kafka.connect.cli.ConnectDistributed;
import org.apache.kafka.connect.runtime.Connect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedKafkaConnect {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedKafkaConnect.class);

    private final ConnectDistributed connectDistributed;
    private final Configuration configuration;
    private Connect connect;

    public EmbeddedKafkaConnect(Configuration configuration) {
        this.configuration = configuration;
        connectDistributed = new ConnectDistributed();
    }

    public void start() {
        log.info("Starting kafka connect ...");
        connect = connectDistributed.startConnect(configuration.createConnectProperties());
        log.info("Starting kafka connect done");
    }

    public void stop() {
        if (connect != null) {
            try {
                log.info("Stopping kafka connect ...");
                connect.stop();
                connect.awaitStop();
                log.info("Stopping kafka connect done");
            } catch (Exception e) {
                log.error("Failed to stop kafka connect", e);
            }
        }
    }

    public void restart() {
        stop();
        start();
    }

}

package io.ceris.embedded;

import io.ceris.Configuration;
import kafka.server.KafkaConfig;
import kafka.server.KafkaRaftServer;
import kafka.server.MetaProperties;
import kafka.tools.StorageTool;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.server.common.MetadataVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.immutable.Seq;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Properties;


public class EmbeddedKafka {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedKafka.class);
    private static final String CLUSTER_ID = "5Yr1SIgYQz-b-dgRabWx4g";

    private final int port;
    private final String dataPath;
    private KafkaRaftServer kafkaServer;

    public EmbeddedKafka(Configuration configuration) {
        this.port = Integer.parseInt(configuration.get("CERIS_EMBEDDED_KAFKA_PORT"));
        this.dataPath = configuration.get("CERIS_EMBEDDED_DATA_PATH");
    }

    public void start() {
        log.info("Starting kafka ...");
        try {

            KafkaConfig config = getConfig();
            Seq<String> logDirectories = StorageTool.configToLogDirectories(config);
            MetaProperties meta = StorageTool.buildMetadataProperties(CLUSTER_ID, config);
            StorageTool.formatCommand(new PrintStream(System.out), logDirectories, meta,
                                      MetadataVersion.MINIMUM_BOOTSTRAP_VERSION, true);

            kafkaServer = new KafkaRaftServer(config, Time.SYSTEM, scala.Option.apply(null));
            kafkaServer.startup();

            log.info("Starting kafka done");
        } catch (Exception e) {
            log.error("Failed to start kafka", e);
            kafkaServer.awaitShutdown();
            throw new StartupException(e);
        }
    }

    public void stop() {
        log.info("Stopping kafka ...");
        try {
            if (kafkaServer != null) {
                kafkaServer.shutdown();
                kafkaServer.awaitShutdown();
                log.info("Stopping kafka done");
            }
        } catch (Exception e) {
            log.error("Failed to stop kafka", e);
        }
    }

    public KafkaConfig getConfig() {

        Properties p = new Properties();

        p.put("process.roles", "broker,controller");
        p.put("node.id", "1");
        p.put("controller.quorum.voters", String.format("1@localhost:%d", port + 1));
        p.put("listeners", String.format("PLAINTEXT://:%d,CONTROLLER://:%d", port, port + 1));
        p.put("inter.broker.listener.name", "PLAINTEXT");
        p.put("advertised.listeners", String.format("PLAINTEXT://localhost:%d", port));
        p.put("controller.listener.names", "CONTROLLER");
        p.put("listener.security.protocol.map",
              "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL");
        p.put("num.network.threads", "3");
        p.put("num.io.threads", "8");
        p.put("socket.send.buffer.bytes", "102400");
        p.put("replica.socket.timeout.ms", "1000");
        p.put("controller.socket.timeout.ms", "1000");
        p.put("socket.receive.buffer.bytes", "102400");
        p.put("socket.request.max.bytes", "104857600");
        p.put("log.dirs", Path.of(dataPath, "kafka", "log").toString());
        p.put("num.recovery.threads.per.data.dir", "1");
        p.put("offsets.topic.replication.factor", "1");
        p.put("transaction.state.log.replication.factor", "1");
        p.put("transaction.state.log.min.isr", "1");
        p.put("log.retention.check.interval.ms", "300000");
        p.put("replica.high.watermark.checkpoint.interval.ms", String.valueOf(Long.MAX_VALUE));
        p.put("offsets.topic.num.partitions", "3");

        return new KafkaConfig(p);
    }

}

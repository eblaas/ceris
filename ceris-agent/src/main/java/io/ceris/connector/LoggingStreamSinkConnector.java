package io.ceris.connector;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LoggingStreamSinkConnector extends SinkConnector {
    private static final String VERSION = "1.0.0";
    private static final ConfigDef CONFIG_DEF = new ConfigDef();

    public LoggingStreamSinkConnector() {
    }

    public String version() {
        return VERSION;
    }

    public void start(Map<String, String> props) {}

    public Class<? extends Task> taskClass() {
        return LoggingStreamSinkConnectorTask.class;
    }

    public List<Map<String, String>> taskConfigs(int maxTasks) {
        return Collections.singletonList(Collections.emptyMap());
    }

    public void stop() {}

    public ConfigDef config() {
        return CONFIG_DEF;
    }

    public static class LoggingStreamSinkConnectorTask extends SinkTask {
        private static final Logger log = LoggerFactory.getLogger(LoggingStreamSinkConnectorTask.class);

        private record Record(String topic, Object key, Object value) {}

        public String version() {
            return LoggingStreamSinkConnector.VERSION;
        }

        public void start(Map<String, String> props) {}

        public void put(Collection<SinkRecord> sinkRecords) {
            for (SinkRecord sinkRecord : sinkRecords) {
                log.info("{}", new Record(sinkRecord.topic(), sinkRecord.key(), sinkRecord.value()));
            }
        }

        public void stop() {}
    }
}

package io.ceris.connector;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.apache.kafka.common.config.ConfigDef.Importance.HIGH;

public class CsvFileStreamSinkConnector extends SinkConnector {
    private static final String VERSION = "1.0.0";
    private static final String PATH_CONFIG = "path";
    private static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(PATH_CONFIG, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, HIGH, "Destination path.");

    private String path;

    public CsvFileStreamSinkConnector() {
    }

    public String version() {
        return VERSION;
    }

    public void start(Map<String, String> props) {
        path = new AbstractConfig(CONFIG_DEF, props).getString(PATH_CONFIG);
    }

    public Class<? extends Task> taskClass() {
        return CsvFileStreamSinkConnectorTask.class;
    }

    public List<Map<String, String>> taskConfigs(int maxTasks) {
        return Collections.singletonList(Map.of(PATH_CONFIG, path));
    }

    public void stop() {
    }

    public ConfigDef config() {
        return CONFIG_DEF;
    }

    public static class CsvFileStreamSinkConnectorTask extends SinkTask {
        private static final Logger log = LoggerFactory.getLogger(CsvFileStreamSinkConnectorTask.class);

        private record TopicDate(String topic, LocalDate date) {}

        private Path path;
        private Cache<TopicDate, PrintStream> cache;

        public CsvFileStreamSinkConnectorTask() {
        }

        public String version() {
            return CsvFileStreamSinkConnector.VERSION;
        }

        public void start(Map<String, String> props) {
            try {
                path = Paths.get(props.get(PATH_CONFIG)).toAbsolutePath();
                path.toFile().mkdirs();
            } catch (Exception e) {
                throw new ConnectException("Couldn't create path '" + path + "'", e);
            }
            cache = CacheBuilder.newBuilder()
                    .expireAfterWrite(1, TimeUnit.DAYS)
                    .removalListener((RemovalListener<TopicDate, PrintStream>) notification -> {
                        log.info("Close stream {}", notification.getKey());
                        notification.getValue().flush();
                        notification.getValue().close();
                    })
                    .build();

        }

        public void put(Collection<SinkRecord> sinkRecords) {
            for (SinkRecord sinkRecord : sinkRecords) {
                TopicDate key = new TopicDate(sinkRecord.topic(), LocalDate.now());
                sinkRecordToRow(sinkRecord).ifPresent(row -> {
                    try {
                        cache.get(key, () -> {

                            String file = key.topic.replace(".", "-") + "-" + key.date.toString() + ".csv";
                            Path fullFilePath = path.resolve(file);
                            OutputStream fileOutput = Files.newOutputStream(fullFilePath, CREATE, APPEND);
                            PrintStream printStream = new PrintStream(fileOutput, false, StandardCharsets.UTF_8.name());

                            if (!fullFilePath.toFile().exists())
                                printStream.println(sinkRecordToHeader(sinkRecord));

                            return printStream;
                        }).println(row);
                    } catch (ExecutionException e) {
                        throw new ConnectException("Failed to write record.", e);
                    }
                });
            }
        }

        private String sinkRecordToHeader(SinkRecord sinkRecord) {
            return sinkRecord.valueSchema().fields().stream().map(Field::name).collect(Collectors.joining(","));
        }

        private Optional<String> sinkRecordToRow(SinkRecord sinkRecord) {
            if (sinkRecord.value() instanceof Struct value) {
                return Optional.of(value.schema().fields()
                                           .stream()
                                           .map(Field::name)
                                           .map(value::get)
                                           .map(Objects::toString)
                                           .map(StringEscapeUtils::escapeCsv)
                                           .collect(Collectors.joining(",")));
            }
            return Optional.empty();
        }

        public void flush(Map<TopicPartition, OffsetAndMetadata> offsets) {
            if (cache != null)
                cache.asMap().values().forEach(PrintStream::flush);
        }

        public void stop() {
            if (cache != null)
                cache.asMap().values().forEach(PrintStream::close);
        }
    }
}

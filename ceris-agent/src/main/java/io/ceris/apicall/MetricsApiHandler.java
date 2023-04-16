package io.ceris.apicall;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ceris.Configuration;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.VersionInfoExports;
import io.prometheus.jmx.JmxCollector;
import io.prometheus.jmx.KafkaConnectMetric;
import io.prometheus.jmx.KafkaConnectMetricsCollector;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.resource.ClassPathResource;
import spark.resource.ExternalResource;
import spark.resource.Resource;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class MetricsApiHandler {

    private static final Logger log = LoggerFactory.getLogger(MetricsApiHandler.class);

    private static final String CLASSPATH_PREFIX = "classpath:";

    private final KafkaConnectMetricsCollector kafkaConnectMetricsCollector;
    private final ObjectMapper mapper;
    private final CollectorRegistry collectorRegistry;

    public MetricsApiHandler(Configuration configuration, ObjectMapper mapper,
                             KafkaConnectMetricsCollector kafkaConnectMetricsCollector,
                             CollectorRegistry collectorRegistry) {
        this.mapper = mapper;
        this.kafkaConnectMetricsCollector = kafkaConnectMetricsCollector;
        this.collectorRegistry = collectorRegistry;

        (new StandardExports()).register(collectorRegistry);
        (new MemoryPoolsExports()).register(collectorRegistry);
        (new VersionInfoExports()).register(collectorRegistry);

        String configPath = configuration.get("CERIS_JMX_EXPORT_CONFIG");
        try {
            Resource resource = configPath.startsWith(CLASSPATH_PREFIX)
                    ? new ClassPathResource(configPath.replace(CLASSPATH_PREFIX, ""))
                    : new ExternalResource(configPath);

            new JmxCollector(resource.getInputStream()).register(collectorRegistry);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load jmx export configuration", e);
        }
    }

    public Object getPrometheus(Request req, Response resp) throws Exception {
        resp.status(200);
        resp.type(TextFormat.CONTENT_TYPE_004);
        StringWriter writer = new StringWriter();
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples());
        return writer.toString();
    }

    public Object getKafkaConnectMetrics(Request req, Response resp) throws Exception {
        Optional<String> connectorFilter = Optional.ofNullable(req.queryParams("connector"));
        Collection<KafkaConnectMetric> kafkaConnectMetrics = kafkaConnectMetricsCollector.collectMetrics().stream()
                .filter(m -> connectorFilter.map(c -> Objects.equals(c, m.getConnector())).orElse(true))
                .toList();
        resp.status(200);
        resp.type(ContentType.APPLICATION_JSON.toString());
        return mapper.writeValueAsString(kafkaConnectMetrics);
    }
}

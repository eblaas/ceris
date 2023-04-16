package io.prometheus.jmx;

import com.google.common.collect.Lists;
import io.ceris.apicall.ApiCallError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.util.Collection;
import java.util.List;

public class KafkaConnectMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(KafkaConnectMetricsCollector.class);

    private final JmxScraper jmxScraper;
    private final MetricsReceiver receiver;

    public KafkaConnectMetricsCollector() {
        try {
            List<ObjectName> whiteList = Lists.newArrayList(ObjectName.getInstance("kafka.connect:*"));
            List<ObjectName> blackList = Lists.newArrayList(
                    ObjectName.getInstance("kafka.connect:type=kafka-metrics-count,*"),
                    ObjectName.getInstance("kafka.connect:type=connect-coordinator-metrics,*"),
                    ObjectName.getInstance("kafka.connect:type=app-info,*"),
                    ObjectName.getInstance("kafka.connect:type=connect-worker-rebalance-metrics,*"),
                    ObjectName.getInstance("kafka.connect:type=connect-node-metrics,client-id=*,*"),
                    ObjectName.getInstance("kafka.connect:type=connect-metrics,client-id=*,*"));

            receiver = new MetricsReceiver();
            jmxScraper = new JmxScraper("", "", "",
                                        false, whiteList, blackList, receiver, new JmxMBeanPropertyCache());
        } catch (Exception e) {
            throw new RuntimeException("Error while creating jmx collector", e);
        }
    }

    public synchronized Collection<KafkaConnectMetric> collectMetrics() {
        try {
            jmxScraper.doScrape();
            return receiver.getMetrics();
        } catch (Exception e) {
            log.error("Failed to load connect metrics", e);
            throw new ApiCallError(500, "Failed to load connect metrics");
        }
    }
}

package io.prometheus.jmx;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsReceiver implements JmxScraper.MBeanReceiver {

    private final ConcurrentHashMap<String, KafkaConnectMetric> metrics = new ConcurrentHashMap<>();

    public void recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {

        String name = domain + beanProperties + attrName;
        Object metricValue = value;

        if (metricValue instanceof Double && (Double.isInfinite((Double) metricValue) || Double.isNaN(
                (Double) metricValue))) {
            metricValue = null;
        }
        if (metricValue != null) {
            metrics.put(name, new KafkaConnectMetric(
                    attrName,
                    beanProperties.get("type"),
                    beanProperties.getOrDefault("connector", "-"),
                    beanProperties.getOrDefault("task", "-"),
                    metricValue
            ));
        }
    }

    public Collection<KafkaConnectMetric> getMetrics() {
        return metrics.values();
    }
}
package io.prometheus.jmx;

public class KafkaConnectMetric {

    private final String name;
    private final String type;
    private final String connector;
    private final String task;
    private final Object value;

    public KafkaConnectMetric(String name, String type, String connector, String task, Object value) {
        this.name = name;
        this.type = type;
        this.connector = connector;
        this.task = task;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getConnector() {
        return connector;
    }

    public String getTask() {
        return task;
    }

    public Object getValue() {
        return value;
    }
}

package io.ceris.apicall.dto;

public record StatusDto(boolean up, String component, String desc, String error) {

    public enum Component {
        KAFKA_CONNECT("kafka-connect", "Connect API"),
        KAFKA("kafka", "Kafka API"),
        SCHEMA_REGISTRY("schema-registry", "Schema Registry API");

        final String component;
        final String desc;

        Component(String component, String desc) {
            this.component = component;
            this.desc = desc;
        }
    }

    public static StatusDto up(Component component) {
        return new StatusDto(true, component.component, component.desc, null);
    }

    public static StatusDto down(Component component, String error) {
        return new StatusDto(false, component.component, component.desc, error);
    }
}

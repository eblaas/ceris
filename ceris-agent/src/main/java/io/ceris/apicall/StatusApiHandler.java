package io.ceris.apicall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ceris.apicall.dto.StatusDto;
import org.apache.http.HttpStatus;
import org.apache.kafka.clients.admin.AdminClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class StatusApiHandler {

    private static final Logger log = LoggerFactory.getLogger(StatusApiHandler.class);

    private final ConnectClients clients;
    private final ObjectMapper mapper;

    public StatusApiHandler(ConnectClients clients, ObjectMapper mapper) {
        this.clients = clients;
        this.mapper = mapper;
    }

    public Object getStatus(Request request, Response resp) throws JsonProcessingException {

        List<StatusDto> responseData = getComponentStatus();
        return mapper.writeValueAsString(responseData);
    }

    public Object getHealth(Request request, Response resp) throws JsonProcessingException {

        List<StatusDto> responseData = getComponentStatus();

        if (responseData.stream().anyMatch(s -> !s.up())) {
            resp.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        return mapper.writeValueAsString(responseData);
    }

    @NotNull
    private List<StatusDto> getComponentStatus() {
        return List.of(
                getConnectStatus(),
                getKafkaStatus(),
                getSchemaRegistryStatus()
        );
    }

    private StatusDto getConnectStatus() {
        try {
            clients.getJavaClient().getConnectServerVersion();
            return StatusDto.up(StatusDto.Component.KAFKA_CONNECT);
        } catch (Exception e) {
            log.debug("Kafka connect status down.", e);
            return StatusDto.down(StatusDto.Component.KAFKA_CONNECT, e.getMessage());
        }
    }

    public StatusDto getKafkaStatus() {
        try (AdminClient client = clients.getKafkaClient()) {
            client.listTopics().names().get(3, TimeUnit.SECONDS);
            return StatusDto.up(StatusDto.Component.KAFKA);
        } catch (Exception e) {
            log.debug("Kafka status down.", e);
            return StatusDto.down(StatusDto.Component.KAFKA, e.getMessage());
        }
    }

    public StatusDto getSchemaRegistryStatus() {
        try {
            clients.getSchemaRegistryClient().getAllSubjects();
            return StatusDto.up(StatusDto.Component.SCHEMA_REGISTRY);
        } catch (Exception e) {
            log.debug("Schema registry status down.", e);
            return StatusDto.down(StatusDto.Component.SCHEMA_REGISTRY, e.getMessage());
        }
    }
}

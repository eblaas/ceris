package io.ceris.apicall;

import com.google.common.base.MoreObjects;
import io.ceris.Configuration;
import io.ceris.utils.SerdeUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;
import spark.Request;
import spark.Response;

import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;

public class KafkaApiHandler {

    private static final Logger log = LoggerFactory.getLogger(KafkaApiHandler.class);

    private final Configuration configuration;
    private final ConnectClients clients;

    public KafkaApiHandler(Configuration configuration, ConnectClients clients) {
        this.configuration = configuration;
        this.clients = clients;
    }

    @SuppressWarnings("ALL")
    public Object getMessages(Request request, Response response) {

        KafkaConsumer consumer = null;
        String topic = request.params(":topic");
        int topn = Integer.parseInt(MoreObjects.firstNonNull(request.queryParams("topn"), "20"));

        ConnectorDefinition definition = clients.getJavaClient()
                .getConnectorsWithExpandedInfo()
                .getAllDefinitions().stream()
                .filter(d -> "source".equals(d.getType()))
                .filter(d -> clients.getJavaClient().getConnectorTopics(d.getName()).getTopics().contains(topic))
                .findFirst()
                .orElseThrow(() -> new ApiCallError(400, "Unknown topic"));


        try {

            Map<String, String> connectorConfig = definition.getConfig();
            String keyConverter = connectorConfig.getOrDefault("key.converter",
                                                               configuration.get("CONNECT_KEY_CONVERTER"));
            String valueConverter = connectorConfig.getOrDefault("value.converter",
                                                                 configuration.get("CONNECT_VALUE_CONVERTER"));

            Map<String, Object> kafkaProperties = configuration.createKafkaProperties();
            kafkaProperties.put("key.deserializer", SerdeUtil.getDeserializer(keyConverter));
            kafkaProperties.put("value.deserializer", SerdeUtil.getDeserializer(valueConverter));
            kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "ceris-agent");
            kafkaProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            consumer = new KafkaConsumer(kafkaProperties);
            consumer.subscribe(Collections.singletonList(topic));

            consumer.poll(Duration.ofSeconds(10));

            List<ConsumerRecord> records = new ArrayList<>();

            Map<TopicPartition, Long> map = consumer.endOffsets(consumer.assignment());

            for (Entry<TopicPartition, Long> partitionEntry : map.entrySet()) {
                consumer.seek(partitionEntry.getKey(), Math.max(0, partitionEntry.getValue() - topn));
                for (Object record : consumer.poll(Duration.ofSeconds(1))) {
                    ConsumerRecord r = (ConsumerRecord) record;
                    if (r.value() != null) {
                        records.add(r);
                    }
                }
            }

            Comparator<ConsumerRecord> comparator = Comparator.comparing(ConsumerRecord::timestamp);
            records.sort(comparator.reversed());

            JSONArray result = new JSONArray();

            for (ConsumerRecord rec : records.stream().limit(topn).toList()) {
                result.put(new JSONObject()
                                   .put("ts", rec.timestamp())
                                   .put("partition", rec.partition())
                                   .put("key", SerdeUtil.convertToJson(rec.key(), "key"))
                                   .put("value", SerdeUtil.convertToJson(rec.value(), "value"))
                );
            }
            return result;
        } catch (Exception e) {
            throw new ApiCallError(500, e.getMessage());
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }
}

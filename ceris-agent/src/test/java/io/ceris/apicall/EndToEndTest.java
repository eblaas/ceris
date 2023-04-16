package io.ceris.apicall;

import io.ceris.util.EnvironmentRule;
import io.ceris.util.HttpClientResolver;
import io.ceris.util.Paths;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.picocontainer.PicoContainer;
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static io.ceris.util.EnvironmentRule.BASE_URL;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({HttpClientResolver.class})
class EndToEndTest {

    @RegisterExtension
    static EnvironmentRule env =
            new EnvironmentRule(Map.of("CERIS_EMBEDDED_PLUGINS_INSTALL", "confluentinc/kafka-connect-datagen:0.5.3"));

    private static final String CON_NAME = "datagen";
    private static final String CON_TOPIC = "stores";

    @BeforeAll
    public static void init(PicoContainer context) throws Exception {
        context.getComponent(ConnectClients.class).getJavaClient().addConnector(
                NewConnectorDefinition.newBuilder()
                        .withName(CON_NAME)
                        .withConfig("connector.class", "io.confluent.kafka.connect.datagen.DatagenConnector")
                        .withConfig("tasks.max", 1)
                        .withConfig("kafka.topic", CON_TOPIC)
                        .withConfig("quickstart", "${env:CERIS_SECRET_TEST_STORES}")
                        .withConfig("max.interval", 1000)
                        .withConfig("errors.log.enable", "true")
                        .build()
        );

        // wait for messages produced
        Thread.sleep(2000);
    }

    @Test
    public void test_secrets_endpoint(CloseableHttpClient client) throws Exception {

        try (var res = client.execute(new HttpGet(Paths.GET_SECRETS))) {
            assertEquals(HttpStatus.SC_OK, res.getStatusLine().getStatusCode());
            JSONArray json = new JSONArray(EntityUtils.toString(res.getEntity(), Charsets.UTF_8));

            assertTrue(json.length() > 0);
            Assertions.assertThat(json.toList())
                    .anyMatch(s -> Objects.equals(s, "${env:CERIS_SECRET_TEST_STORES}"));
        }
    }

    @Test
    public void test_topic_messages_endpoint(CloseableHttpClient client) throws Exception {

        URI uri = new URIBuilder(BASE_URL).setPathSegments("api", "connect", "topics", CON_TOPIC, "messages").build();

        try (var res = client.execute(new HttpGet(uri))) {
            assertEquals(HttpStatus.SC_OK, res.getStatusLine().getStatusCode());
            JSONArray json = new JSONArray(EntityUtils.toString(res.getEntity(), Charsets.UTF_8));

            assertTrue(json.length() > 0);
            JSONObject first = json.getJSONObject(0);
            assertNotNull(first.get("ts"));
            assertNotNull(first.get("key"));
            assertNotNull(first.get("value"));
        }
    }
}
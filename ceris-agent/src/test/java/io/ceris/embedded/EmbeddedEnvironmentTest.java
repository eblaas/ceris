package io.ceris.embedded;

import io.ceris.util.EnvironmentRule;
import io.ceris.util.HttpClientResolver;
import io.ceris.util.Paths;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.internal.guava.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.ceris.util.Paths.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({EnvironmentRule.class, HttpClientResolver.class})
class EmbeddedEnvironmentTest {

    @Test
    public void test_embedded_environment_start_status(CloseableHttpClient client) throws Exception {

        try (var res = client.execute(new HttpGet(Paths.GET_STATUS))) {

            assertEquals(HttpStatus.SC_OK, res.getStatusLine().getStatusCode());

            JSONArray json = new JSONArray(EntityUtils.toString(res.getEntity(), Charsets.UTF_8));
            assertTrue(json.length() > 0);
            Assertions.assertThat(Lists.newArrayList(json))
                    .allMatch(o -> ((JSONObject) o).getBoolean("up"), "Component not started");
        }
    }

    @Test
    public void test_health_endpoint(CloseableHttpClient client) throws Exception {

        try (var res = client.execute(new HttpGet(GET_HEALTH))) {
            assertEquals(HttpStatus.SC_OK, res.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void test_metrics_endpoint(CloseableHttpClient client) throws Exception {

        try (var res = client.execute(new HttpGet(GET_METRICS))) {
            assertEquals(HttpStatus.SC_OK, res.getStatusLine().getStatusCode());
            JSONArray json = new JSONArray(EntityUtils.toString(res.getEntity(), Charsets.UTF_8));

            assertTrue(json.length() > 0);
            JSONObject first = json.getJSONObject(0);
            assertNotNull(first.get("name"));
            assertNotNull(first.get("type"));
            assertNotNull(first.get("value"));
        }
    }

    @Test
    public void test_prometheus_endpoint(CloseableHttpClient client) throws Exception {

        try (var res = client.execute(new HttpGet(GET_PROMETHEUS))) {
            assertEquals(HttpStatus.SC_OK, res.getStatusLine().getStatusCode());
        }
    }
}
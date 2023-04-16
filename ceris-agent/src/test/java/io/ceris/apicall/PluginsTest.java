package io.ceris.apicall;

import io.ceris.util.EnvironmentRule;
import io.ceris.util.HttpClientResolver;
import io.ceris.util.Paths;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;
import java.util.Optional;

import static io.ceris.utils.SerdeUtil.arrayToList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({EnvironmentRule.class, HttpClientResolver.class})
class PluginsTest {

    @Test
    public void test_connector_plugin_install_endpoint(CloseableHttpClient client) throws Exception {

        JSONObject body = new JSONObject().put("pluginId", "confluentinc/kafka-connect-datagen:latest");

        HttpPost httpPost = new HttpPost(Paths.GET_CONNECTOR_PLUGINS);
        httpPost.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

        try (var resp = client.execute(httpPost)) {
            assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
        }

        try (var resp = client.execute(new HttpGet(Paths.GET_CONNECTOR_PLUGINS))) {
            assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
            JSONArray json = new JSONArray(EntityUtils.toString(resp.getEntity(), Charsets.UTF_8));
            Optional<String> id = arrayToList(json).stream()
                    .filter(p -> !p.isNull("manifest"))
                    .filter(p -> Objects.equals("kafka-connect-datagen", p.getJSONObject("manifest").getString("name")))
                    .map(p -> p.getString("id"))
                    .findFirst();

            assertTrue(id.isPresent());

            try (var respDelete = client.execute(new HttpDelete(Paths.GET_CONNECTOR_PLUGINS + "/" + id.get()))) {
                assertEquals(HttpStatus.SC_OK, respDelete.getStatusLine().getStatusCode());
            }
        }
    }
}

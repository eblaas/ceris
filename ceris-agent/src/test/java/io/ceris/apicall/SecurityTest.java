package io.ceris.apicall;

import io.ceris.util.EnvironmentRule;
import io.ceris.util.HttpClientResolver;
import io.ceris.util.Paths;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Base64;

import static io.ceris.util.EnvironmentRule.BASE_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({EnvironmentRule.WithSecurity.class, HttpClientResolver.class})
class SecurityTest {

    @Test
    public void test_connect_endpoint_is_protected(CloseableHttpClient client) throws Exception {

        try (var resp = client.execute(new HttpGet(Paths.GET_SECRETS))) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, resp.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void test_auth_with_token(CloseableHttpClient client) throws Exception {

        JSONObject body = new JSONObject()
                .put("username", "admin")
                .put("password", "admin");

        HttpPost httpPost = new HttpPost(new URIBuilder(BASE_URL).setPathSegments("auth", "login").build());
        httpPost.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

        String token = client.execute(httpPost, response -> {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity(), Charsets.UTF_8));
            assertNotNull(json.getString("token"));
            return json.getString("token");
        });

        HttpGet getSecrets = new HttpGet(Paths.GET_SECRETS);
        getSecrets.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        try (var resp = client.execute(getSecrets)) {
            assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
        }

        HttpGet getMe = new HttpGet(Paths.GET_ME);
        try (var resp = client.execute(getMe)) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, resp.getStatusLine().getStatusCode());
        }

        getMe.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        try (var resp = client.execute(getMe)) {
            assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
            JSONObject me = new JSONObject(EntityUtils.toString(resp.getEntity(), Charsets.UTF_8));
            assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
            assertEquals("admin", me.getString("username"));
            assertEquals("ADMIN", me.getString("role"));
        }
    }

    @Test
    public void test_basic_auth(CloseableHttpClient client) throws Exception {

        String encoding = Base64.getEncoder().encodeToString("admin:admin".getBytes());

        HttpGet getSecrets = new HttpGet(Paths.GET_SECRETS);
        getSecrets.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
        try (var resp = client.execute(getSecrets)) {
            assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void test_user_role_is_read_only(CloseableHttpClient client) throws Exception {

        String encoding = Base64.getEncoder().encodeToString("user:user".getBytes());

        HttpPost post = new HttpPost(new URIBuilder(BASE_URL).setPathSegments("api", "connect", "install").build());
        post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);

        try (var resp = client.execute(post)) {
            assertEquals(HttpStatus.SC_FORBIDDEN, resp.getStatusLine().getStatusCode());
        }
    }
}
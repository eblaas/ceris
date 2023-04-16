package io.ceris.util;

import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import static io.ceris.util.EnvironmentRule.BASE_URL;

public class Paths {
    public static URI GET_ME;
    public static URI GET_SECRETS;
    public static URI GET_STATUS;
    public static URI GET_METRICS;
    public static URI GET_PROMETHEUS;
    public static URI GET_HEALTH;
    public static URI GET_CONNECTOR_PLUGINS;

    static {
        try {
            GET_ME = new URIBuilder(BASE_URL).setPathSegments("api", "connect", "me").build();
            GET_SECRETS = new URIBuilder(BASE_URL).setPathSegments("api", "connect", "secrets").build();
            GET_STATUS = new URIBuilder(BASE_URL).setPathSegments("api", "connect", "status").build();
            GET_METRICS = new URIBuilder(BASE_URL).setPathSegments("api", "connect", "metrics").build();
            GET_PROMETHEUS = new URIBuilder(BASE_URL).setPathSegments("api", "connect", "prometheus").build();
            GET_HEALTH = new URIBuilder(BASE_URL).setPathSegments("health").build();
            GET_CONNECTOR_PLUGINS =
                    new URIBuilder(BASE_URL).setPathSegments("api", "connect", "connector-plugins").build();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}

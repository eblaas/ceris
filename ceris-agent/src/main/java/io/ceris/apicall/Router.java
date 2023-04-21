package io.ceris.apicall;

import io.ceris.Configuration;
import io.ceris.apicall.auth.AuthApiHandler;
import io.ceris.apicall.auth.AuthFilter;
import io.ceris.apicall.auth.CorsFilter;
import org.apache.http.HttpStatus;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import javax.ws.rs.core.MediaType;

import static spark.Spark.*;

public class Router implements Startable {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    public static final String ROUTE_CONNECT_API = "/api/connect";
    public static final String ROUTE_AUTH = "/auth";

    private final int apiPort;
    private final ConnectApiHandler connectApiHandler;
    private final KafkaApiHandler kafkaApiHandler;
    private final StatusApiHandler statusHandler;
    private final MetricsApiHandler metricsApiHandler;
    private final AuthFilter authFilter;
    private final AuthApiHandler authApiHandler;
    private final CorsFilter corsFilter;

    public Router(Configuration configuration,
                  ConnectApiHandler connectApiHandler,
                  KafkaApiHandler kafkaApiHandler,
                  StatusApiHandler statusHandler,
                  MetricsApiHandler metricsApiHandler,
                  AuthFilter authFilter,
                  CorsFilter corsFilter,
                  AuthApiHandler authApiHandler) {
        this.apiPort = Integer.parseInt(configuration.get("CERIS_API_PORT"));
        this.connectApiHandler = connectApiHandler;
        this.kafkaApiHandler = kafkaApiHandler;
        this.statusHandler = statusHandler;
        this.metricsApiHandler = metricsApiHandler;
        this.authFilter = authFilter;
        this.corsFilter = corsFilter;
        this.authApiHandler = authApiHandler;
    }

    @Override
    public void start() {
        log.info("Starting api server. port={}", apiPort);

        port(apiPort);
        staticFiles.location("/public");

        before(corsFilter, authFilter);

        path(ROUTE_AUTH, () -> post("/login", authApiHandler::login));

        path(ROUTE_CONNECT_API, () -> {
            get("/me", authApiHandler::me);
            get("/secrets", connectApiHandler::getSecrets);
            get("/topics", connectApiHandler::getTopics);
            get("/topics/:topic/schema", connectApiHandler::getTopicSchema);
            get("/topics/:topic/messages", kafkaApiHandler::getMessages);
            get("/connector-plugins", connectApiHandler::getConnectorPlugins);
            post("/connector-plugins", connectApiHandler::installPlugin);
            delete("/connector-plugins/:id", connectApiHandler::uninstallPlugin);
            get("/plugins-store", connectApiHandler::getPluginsStore);
            delete("/connectors/:connector", connectApiHandler::deleteConnector);
            get("/status", statusHandler::getStatus);
            get("/metrics", metricsApiHandler::getKafkaConnectMetrics);
            get("/prometheus", metricsApiHandler::getPrometheus);
            get("/*", connectApiHandler::proxyRequest);
            post("/*", connectApiHandler::proxyRequest);
            put("/*", connectApiHandler::proxyRequest);
            delete("/*", connectApiHandler::proxyRequest);
        });

        get("/health", statusHandler::getHealth);

        exception(ApiCallError.class, (exception, request, response) -> {
            ApiCallError e = (ApiCallError) exception;
            response.body(e.toJson());
            response.status(e.getStatus());
            response.type(MediaType.APPLICATION_JSON);
        });

        exception(RuntimeException.class, (exception, request, response) -> {
            log.error("Failed to process request", exception);
            response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.type(MediaType.APPLICATION_JSON);
            response.body(new ApiCallError(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getMessage()).toJson());
        });

        awaitInitialization();
    }

    @Override
    public void stop() {
        Spark.stop();
    }
}

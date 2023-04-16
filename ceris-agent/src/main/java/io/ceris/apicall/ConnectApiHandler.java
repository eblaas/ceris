package io.ceris.apicall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ceris.Configuration;
import io.ceris.apicall.dto.*;
import io.ceris.plugin.EnvConfigProvider;
import io.ceris.plugin.PluginService;
import io.confluent.connect.hub.cli.ExitCode;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.ceris.apicall.Router.ROUTE_CONNECT_API;

public class ConnectApiHandler {

    private static final Logger log = LoggerFactory.getLogger(ConnectApiHandler.class);

    private final Configuration configuration;
    private final ConnectClients clients;
    private final PluginService pluginService;
    private final EnvConfigProvider envConfigProvider;
    private final ObjectMapper mapper;

    public ConnectApiHandler(Configuration configuration, ConnectClients clients, PluginService pluginService,
                             EnvConfigProvider envConfigProvider, ObjectMapper mapper) {
        this.configuration = configuration;
        this.clients = clients;
        this.pluginService = pluginService;
        this.envConfigProvider = envConfigProvider;
        this.mapper = mapper;
    }

    public Object proxyRequest(Request req, Response resp) throws Exception {

        URI uri = new URIBuilder(clients.getBaseUrl()).setPath(req.uri().split(ROUTE_CONNECT_API)[1]).build();
        String body = req.body();

        RequestBuilder connectRequestBuilder = RequestBuilder.create(req.requestMethod()).setUri(uri);

        if (Strings.isNotEmpty(body)) {
            connectRequestBuilder.setEntity(EntityBuilder.create().setText(body).build());
        }

        req.queryParams().forEach(query -> connectRequestBuilder.addParameter(query, req.queryParams(query)));
        req.headers().stream()
                .filter(header -> !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(header))
                .filter(header -> !HttpHeaders.AUTHORIZATION.equalsIgnoreCase(header))
                .forEach(header -> connectRequestBuilder.addHeader(header, req.headers(header)));

        HttpUriRequest request = connectRequestBuilder.build();

        log.debug("New proxy request: {}", request);

        try (CloseableHttpResponse connectResponse = clients.getHttpClient().execute(request)) {
            resp.status(connectResponse.getStatusLine().getStatusCode());
            String json = EntityUtils.toString(connectResponse.getEntity(), Charsets.UTF_8);
            for (Header header : connectResponse.getAllHeaders()) {
                resp.header(header.getName(), header.getValue());
            }
            log.debug("Response body: {}", json);
            return json;
        } catch (IOException e) {
            throw new ApiCallError(500, "Connection to kafka connect api failed. error=" + e.getMessage());
        }
    }

    public Object deleteConnector(Request req, Response resp) {
        String connector = req.params(":connector");

        try {
            ConnectorDefinition definition = clients.getJavaClient().getConnector(connector);

            if (definition.getType().equals("source") && configuration.isEmbeddedEnv()) {

                List<String> topics = clients.getJavaClient().getConnectorTopics(connector).getTopics();

                clients.getJavaClient().deleteConnector(connector);

                for (String topic : topics) {
                    try {
                        clients.getSchemaRegistryClient().deleteSubject(topic + "-value");
                        clients.getSchemaRegistryClient().deleteSubject(topic + "-key");
                    } catch (Exception e) {
                        log.warn("Failed to delete schema for topic={}, error={}", topic, e.getMessage());
                    }
                }

                try (AdminClient client = clients.getKafkaClient()) {
                    client.deleteTopics(topics).all().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Failed to delete topics", e);
                }
            } else {
                clients.getJavaClient().deleteConnector(connector);
            }
        } catch (Exception e) {
            throw new ApiCallError(500, "Failed to delete connector. error=" + e.getMessage());
        }
        return "";
    }


    public Object installPlugin(Request req, Response resp) throws Exception {
        if (configuration.isEmbeddedEnv()) {
            PluginInstall plugin = mapper.readValue(req.body(), PluginInstall.class);
            ExitCode exitCode = pluginService.install(plugin, true, true);
            if (exitCode.equals(ExitCode.SUCCESSFUL_COMPLETION)) {
                return "";
            } else {
                throw new ApiCallError(500, exitCode.getDescription());
            }
        } else {
            throw new ApiCallError(405, "Plugin installation not supported");
        }
    }

    public Object uninstallPlugin(Request req, Response resp) {
        if (configuration.isEmbeddedEnv()) {
            String id = req.params(":id");
            pluginService.uninstall(id);
            return "";
        } else {
            throw new ApiCallError(405, "Plugin uninstallation not supported");
        }
    }

    public Object getConnectorPlugins(Request req, Response resp) throws JsonProcessingException {

        List<ConnectPluginDto> response;

        if (configuration.isEmbeddedEnv()) {
            response = pluginService.getInstalledPlugins().stream()
                    .map(ConnectPluginDto::new)
                    .toList();
        } else {
            response = clients.getJavaClient().getConnectorPlugins().stream()
                    .map(ConnectPluginDto::new)
                    .toList();
        }

        return mapper.writeValueAsString(response);
    }

    public Object getSecrets(Request req, Response resp) throws JsonProcessingException {
        Set<String> secrets = envConfigProvider.getKeys()
                .stream()
                .map(key -> String.format("${env:%s}", key))
                .collect(Collectors.toSet());
        return mapper.writeValueAsString(secrets);
    }

    public Object getTopics(Request req, Response resp) throws JsonProcessingException {

        List<ConnectorDefinition> sourceConnectors = clients.getJavaClient()
                .getConnectorsWithExpandedInfo()
                .getAllDefinitions().stream()
                .filter(d -> "source".equals(d.getType()))
                .toList();


        List<SourceConnectorTopicsDto> responseData = sourceConnectors.stream()
                .map(sourceConnector -> new SourceConnectorTopicsDto(
                        sourceConnector.getName(),
                        clients.getJavaClient().getConnectorTopics(sourceConnector.getName()).getTopics()))
                .toList();

        return mapper.writeValueAsString(responseData);
    }

    public Object getPluginsStore(Request req, Response resp) throws Exception {
        List<PluginManifestDto> responseData = pluginService.getAvailablePlugins().stream()
                .map(PluginManifestDto::new)
                .toList();

        return mapper.writeValueAsString(responseData);
    }

    public Object getTopicSchema(Request req, Response resp) throws IOException {
        String topic = req.params(":topic");

        SchemaMetadata keySchema = null;
        SchemaMetadata valueSchema = null;
        try {
            keySchema = clients.getSchemaRegistryClient().getLatestSchemaMetadata(topic + "-key");
        } catch (RestClientException e) {
            log.warn("Missing key schema for topic={}", topic);
        }

        try {
            valueSchema = clients.getSchemaRegistryClient().getLatestSchemaMetadata(topic + "-value");
        } catch (RestClientException e) {
            log.warn("Missing value schema for topic={}", topic);
        }

        return mapper.writeValueAsString(new SchemaDto(keySchema, valueSchema));
    }
}

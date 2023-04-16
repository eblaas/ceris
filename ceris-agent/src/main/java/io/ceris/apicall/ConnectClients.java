package io.ceris.apicall;

import io.ceris.Configuration;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.kafka.clients.admin.AdminClient;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.util.Map;

public class ConnectClients {

    private final CloseableHttpClient httpClient;
    private final KafkaConnectClient javaClient;
    private final SchemaRegistryClient schemaRegistryClient;
    private final String baseUrl;
    private final Map<String, Object> kafkaProperties;

    public ConnectClients(Configuration configuration) {

        String authUsername = configuration.get("CERIS_CONNECT_AUTH_USERNAME");
        String authPassword = configuration.get("CERIS_CONNECT_AUTH_PASSWORD");
        baseUrl = configuration.get("CERIS_CONNECT_URL").replaceAll("/$", "");

        int timeout = 20;

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (authUsername != null && authPassword != null) {
            HttpHost targetHost = new HttpHost(baseUrl);
            BasicCredentialsProvider provider = new BasicCredentialsProvider();
            AuthScope authScope = new AuthScope(targetHost);
            provider.setCredentials(authScope, new UsernamePasswordCredentials(authUsername, authPassword));
            httpClientBuilder.setDefaultCredentialsProvider(provider);
        }
        httpClient = httpClientBuilder.setDefaultRequestConfig(config).build();

        org.sourcelab.kafka.connect.apiclient.Configuration clientConfig =
                new org.sourcelab.kafka.connect.apiclient.Configuration(baseUrl);
        if (authUsername != null && authPassword != null) {
            clientConfig.useBasicAuth(authUsername, authPassword);
        }
        this.javaClient = new KafkaConnectClient(clientConfig);

        String schemaRegistryUrl = configuration.get("KAFKA_SCHEMA_REGISTRY_URL");
        schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 10);

        kafkaProperties = configuration.createKafkaProperties();
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public KafkaConnectClient getJavaClient() {
        return javaClient;
    }

    public SchemaRegistryClient getSchemaRegistryClient() {
        return schemaRegistryClient;
    }

    public AdminClient getKafkaClient() {
        return AdminClient.create(kafkaProperties);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

}

package io.ceris.util;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.extension.*;


public class HttpClientResolver implements ParameterResolver, BeforeAllCallback, AfterAllCallback {

    private CloseableHttpClient client;

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(CloseableHttpClient.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        return client;
    }

    @Override public void afterAll(ExtensionContext context) throws Exception {
        if (client != null)
            client.close();
    }

    @Override public void beforeAll(ExtensionContext context) {
        client = HttpClientBuilder.create().build();
    }
}
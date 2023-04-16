package io.ceris.util;

import io.ceris.Application;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.test.TestUtils;
import org.junit.jupiter.api.extension.*;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EnvironmentRule implements ParameterResolver, AfterAllCallback, BeforeAllCallback {

    public static final String BASE_URL = "http://localhost:4567";

    private MutablePicoContainer context;
    private File dataDir;
    private final Map<String, String> overwrite;

    public EnvironmentRule() {
        this(Collections.emptyMap());
    }

    public EnvironmentRule(Map<String, String> overwrite) {
        this.overwrite = new HashMap<>();
        this.overwrite.put("CERIS_AUTH_ENABLED", "false");
        this.overwrite.putAll(overwrite);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        dataDir = TestUtils.tempDirectory();
        overwrite.put("CERIS_EMBEDDED_DATA_PATH", dataDir.getAbsolutePath());

        System.setProperty("CERIS_SECRET_TEST_STORES", "stores");

        context = Application.createContext(overwrite);
        context.start();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        context.stop();
        FileUtils.deleteDirectory(dataDir);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(PicoContainer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return context;
    }

    public static class WithSecurity extends EnvironmentRule {

        public WithSecurity() {
            super(Map.of("CERIS_AUTH_ENABLED", "true"));
        }
    }

}

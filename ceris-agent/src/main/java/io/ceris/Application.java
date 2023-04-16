package io.ceris;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ceris.apicall.*;
import io.ceris.apicall.auth.AuthApiHandler;
import io.ceris.apicall.auth.AuthFilter;
import io.ceris.apicall.auth.CorsFilter;
import io.ceris.apicall.auth.UserService;
import io.ceris.embedded.*;
import io.ceris.init.ResourcesInitializer;
import io.ceris.plugin.ConfluentHubPluginSource;
import io.ceris.plugin.EnvConfigProvider;
import io.ceris.plugin.LensesIoPluginSource;
import io.ceris.plugin.PluginService;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.jmx.KafkaConnectMetricsCollector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;

import java.util.Collections;
import java.util.Map;

public class Application {

    public static MutablePicoContainer createContext(Map<String, String> overwrite) {
        MutablePicoContainer context;

        Configuration configuration = new Configuration(overwrite);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        initLogger(configuration);

        context = new DefaultPicoContainer(new Caching());
        context.addComponent(configuration);
        context.addComponent(objectMapper);
        context.addComponent(new CollectorRegistry(true));
        context.addComponent(ConnectClients.class);
        context.addComponent(ConnectApiHandler.class);
        context.addComponent(KafkaApiHandler.class);
        context.addComponent(StatusApiHandler.class);
        context.addComponent(MetricsApiHandler.class);
        context.addComponent(KafkaConnectMetricsCollector.class);
        context.addComponent(PluginService.class);
        context.addComponent(LensesIoPluginSource.class);
        context.addComponent(ConfluentHubPluginSource.class);
        context.addComponent(EnvConfigProvider.class);
        context.addComponent(UserService.class);
        context.addComponent(AuthApiHandler.class);
        context.addComponent(AuthFilter.class);
        context.addComponent(CorsFilter.class);

        boolean startEmbeddedEnv = Boolean.parseBoolean(configuration.get("CERIS_EMBEDDED_ENABLED"));

        if (startEmbeddedEnv) {
            context = context.addChildContainer(new DefaultPicoContainer(context))
                    .addComponent(EmbeddedStarter.class)
                    .addComponent(EmbeddedKafka.class)
                    .addComponent(EmbeddedPluginInstaller.class)
                    .addComponent(EmbeddedSchemaRegistry.class)
                    .addComponent(EmbeddedKafkaConnect.class);
        }
        context = context.addChildContainer(new DefaultPicoContainer(context))
                .addComponent(Router.class)
                .addComponent(ResourcesInitializer.class);

        return context;
    }

    private static void initLogger(Configuration configuration) {
        Configurator.setRootLevel(Level.valueOf(configuration.get("CERIS_ROOT_LOGLEVEL")));

        java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(java.util.logging.Level.SEVERE);
    }

    public static void main(String[] args) throws Exception {
        MutablePicoContainer context = Application.createContext(Collections.emptyMap());
        context.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (context.getLifecycleState().isStarted()) {
                context.stop();
            }
        }));
    }
}

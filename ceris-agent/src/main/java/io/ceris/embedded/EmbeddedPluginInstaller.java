package io.ceris.embedded;

import io.ceris.Configuration;
import io.ceris.apicall.dto.PluginInstall;
import io.ceris.plugin.PluginService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

public class EmbeddedPluginInstaller {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedPluginInstaller.class);

    private final Configuration configuration;
    private final PluginService pluginService;

    public EmbeddedPluginInstaller(Configuration configuration, PluginService pluginService) {
        this.configuration = configuration;
        this.pluginService = pluginService;
    }

    public void start() {

        log.info("Starting plugin installation ...");

        String pluginsToInstall = configuration.get("CERIS_EMBEDDED_PLUGINS_INSTALL");

        if (Strings.isNotEmpty(pluginsToInstall)) {
            Arrays.stream(pluginsToInstall.split(","))
                    .map(pluginId -> new PluginInstall(pluginId, Collections.emptyList()))
                    .forEach(pluginId -> pluginService.install(pluginId, false, false));
        }

        log.info("Plugin installation done");
    }
}

package io.ceris.apicall.dto;

import java.util.List;

public record PluginInstall(String pluginId, List<String> jars) {}

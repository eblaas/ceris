package io.ceris.apicall.dto;

import com.google.common.base.MoreObjects;
import io.confluent.pluginregistry.PluginType;
import io.confluent.pluginregistry.rest.entities.PluginLicense;
import io.confluent.pluginregistry.rest.entities.PluginManifest;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PluginManifestDto {

    public static final String DEFAULT_IMAGE = "https://www.confluent.io/hub/static/57d3434c6f3eec0e68d2588b8b0bdbca"
            + "/fc069/connect-icon.png";
    private final String pluginId;
    private final String name;
    private final String title;
    private final String version;
    private final String owner;
    private final License license;
    private final String description;
    private final String documentation;
    private final String icon;
    private final List<String> type;
    private final Set<String> tags;

    public PluginManifestDto(PluginManifest manifest) {
        pluginId = manifest.getOwner().getUsername() + "/" + manifest.getName() + (manifest.getVersion() != null
                ? ":"
                + manifest.getVersion()
                : "");
        name = manifest.getName();
        title = manifest.getTitle();
        version = manifest.getVersion();
        owner = manifest.getOwner().getName();
        license = MoreObjects.firstNonNull(manifest.getLicenses(), Collections.<PluginLicense>emptyList()).stream()
                .findFirst()
                .map(license -> new License(license.getName(), license.getUrl()))
                .orElse(null);
        description = manifest.getDescription();
        documentation = manifest.getDocumentationUrl();
        icon = MoreObjects.firstNonNull(manifest.getLogo(), DEFAULT_IMAGE);
        type = manifest.getPluginTypes().stream().map(PluginType::getDisplayName).toList();
        tags = manifest.getTags();
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getOwner() {
        return owner;
    }

    public License getLicense() {
        return license;
    }

    public String getDescription() {
        return description;
    }

    public String getDocumentation() {
        return documentation;
    }

    public List<String> getType() {
        return type;
    }

    public String getIcon() {
        return icon;
    }

    public Set<String> getTags() {
        return tags;
    }

    public record License(String name, String url) {}
}

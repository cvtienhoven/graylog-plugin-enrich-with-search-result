package org.graylog.plugins;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class EnrichWithSearchResultFunctionMetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "org.graylog.plugins.graylog-plugin-enrich-with-search-result/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "org.graylog.plugins.EnrichWithSearchResultFunctionPlugin";
    }

    @Override
    public String getName() {
        return "EnrichWithSearchResultFunction";
    }

    @Override
    public String getAuthor() {
        return "cvtienhoven <c_van_tienhoven@hotmail.com>";
    }

    @Override
    public URI getURL() {
        return URI.create("https://github.com/cvtienhoven/graylog-plugin-enrich-with-search-result");
    }

    @Override
    public Version getVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(1, 0, 0, "ga"));
    }

    @Override
    public String getDescription() {
        // TODO Insert correct plugin description
        return "Description of EnrichWithSearchResultFunction plugin";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "graylog.version", Version.from(0, 0, 0, "unknown"));
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}

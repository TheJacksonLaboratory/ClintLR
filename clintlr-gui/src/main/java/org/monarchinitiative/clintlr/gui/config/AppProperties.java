package org.monarchinitiative.clintlr.gui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * The properties that match {@code application.properties} that is bundled with the app.
 * <p>
 * Note, these properties are <em>not</em> the properties we use to serialize the user data, such as path
 * to {@code mondo.json}, LIRICAL data directory, etc.
 */
@ConfigurationProperties(prefix = "l4ci")
public class AppProperties {

    private String title;
    private String version;
    private String mondoJsonUrl;

    @NestedConfigurationProperty // l4ci.lirical
    private LiricalProperties lirical;

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String version() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String mondoJsonUrl() {
        return mondoJsonUrl;
    }

    public void setMondoJsonUrl(String mondoJsonUrl) {
        this.mondoJsonUrl = mondoJsonUrl;
    }

    public LiricalProperties liricalProperties() {
        return lirical;
    }

    public void setLirical(LiricalProperties lirical) {
        this.lirical = lirical;
    }
}


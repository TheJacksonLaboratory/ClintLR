package org.monarchinitiative.l2ci.gui.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The properties that matches {@code application.properties} that is bundled with the app.
 * <p>
 * Note, these properties are <em>not</em> the properties we use to serialize the user data, such as path
 * to {@code mondo.json}, LIRICAL data directory, etc.
 */
@Component
public class AppProperties {

    private final String title;
    private final String version;
    private final String mondoJsonUrl;

    @Autowired
    public AppProperties(@Value("${l4ci.title}") String title,
                         @Value("${l4ci.version}") String version,
                         @Value("${mondo.json.url}") String mondoJsonUrl) {
        this.title = title;
        this.version = version;
        this.mondoJsonUrl = mondoJsonUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getMondoJsonUrl() {
        return mondoJsonUrl;
    }

}


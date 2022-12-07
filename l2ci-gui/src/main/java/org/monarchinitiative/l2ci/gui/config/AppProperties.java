package org.monarchinitiative.l2ci.gui.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The properties that matches {@code application.properties} that is bundled with the app.
 * <p>
 * Note, these properties are <em>not</em> the properties we use to serialize the user data, such as path
 * to {@code hp.json}, LIRICAL data directory, etc.
 */
@Component
public class AppProperties {

    private final String applicationUiTitle;

    private final String version;

    private final String hpoJsonUrl;

    private final String mondoOboUrl;

    private final String mondoOwlUrl;

    private final String mondoJsonUrl;


    private final String phenotypeHpoaUrl;

    @Autowired
    public AppProperties(@Value("${spring.application.ui.title}") String uiTitle,
                         @Value("${l4ci.version}") String version,
                         @Value("${hpo.json.url}") String hpoJson,
                         @Value("${mondo.obo.url}") String mondoObo,
                         @Value("${mondo.owl.url}") String mondoOwl,
                         @Value("${mondo.json.url}") String mondoJson,
                         @Value("${hpo.phenotype.annotations.url}") String annotsUrl) {
        this.applicationUiTitle = uiTitle;
        this.version = version;
        this.hpoJsonUrl = hpoJson;
        this.mondoOboUrl = mondoObo;
        this.mondoOwlUrl = mondoOwl;
        this.mondoJsonUrl = mondoJson;
        this.phenotypeHpoaUrl = annotsUrl;
    }

    public String getApplicationUiTitle() {
        return applicationUiTitle;
    }

    public String getVersion() {
        return version;
    }

    public String getHpoJsonUrl() {
        return hpoJsonUrl;
    }

    public String getMondoOboUrl() {
        return mondoOboUrl;
    }

    public String getMondoOwlUrl() {
        return mondoOwlUrl;
    }

    public String getMondoJsonUrl() {
        return mondoJsonUrl;
    }

    public String getPhenotypeHpoaUrl() {
        return phenotypeHpoaUrl;
    }
}


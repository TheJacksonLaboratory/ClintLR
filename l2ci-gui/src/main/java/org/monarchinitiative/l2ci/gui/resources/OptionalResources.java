package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

@Component
public class OptionalResources {
    // The class is annotated by @Component - Spring will create a singleton instance during App's startup.

    static final String LIRICAL_RESULTS_PROPERTY = "lirical.results.directory";

    private final OntologyResources ontologyResources;
    private final LiricalResources liricalResources;
    private final ObjectProperty<Path> liricalResults = new SimpleObjectProperty<>(this, "liricalResults");

    public OptionalResources() {
        ontologyResources = new OntologyResources();
        liricalResources = new LiricalResources();
    }

    public OntologyResources ontologyResources() {
        return ontologyResources;
    }

    public LiricalResources liricalResources() {
        return liricalResources;
    }

    public Path getLiricalResults() {
        return liricalResults.get();
    }

    public ObjectProperty<Path> liricalResultsProperty() {
        return liricalResults;
    }

    public void setLiricalResults(Path liricalResults) {
        this.liricalResults.set(liricalResults);
    }

    /**
     * Read the app's state from provided {@linkplain Properties}. We expect to find path(s) to resource files
     * and analysis parameters stored under keys defined as static attributes of {@linkplain OptionalResources},
     * {@link LiricalResources}, etc.
     *
     * @param properties to read from.
     */
    public void initResources(Properties properties) {
        Objects.requireNonNull(properties);

        // --------------------------- Mondo ----------------------------
        String mondoJsonPath = properties.getProperty(OntologyResources.MONDO_JSON_PATH_PROPERTY);
        if (mondoJsonPath != null)
            // TODO(mabeckwith) - check if the path is a file, if it ends with *.json, if it is readable, etc.
            ontologyResources.setMondoPath(Path.of(mondoJsonPath));

        // --------------------------- LIRICAL ---------------------------
        String liricalDataPath = properties.getProperty(LiricalResources.LIRICAL_DATA_PROPERTY);
        if (liricalDataPath != null)
            // TODO(mabeckwith) - check if the path is a folder, if it is readable, etc.
            liricalResources.setDataDirectory(Path.of(liricalDataPath));


        // TODO(mabeckwith) - set the remaining LIRICAL properties to LiricalResources

        // TODO - set the remaining paths if present in the properties
        String exomiserHg19VariantPath = properties.getProperty(LiricalResources.EXOMISER_HG19_VARIANT_PROPERTY);
        if (exomiserHg19VariantPath != null)
            // TODO(mabeckwith) - check if the path is a folder, if it is readable, etc.
            liricalResources.setExomiserHg19VariantDbFile(Path.of(exomiserHg19VariantPath));
        String exomiserHg38VariantPath = properties.getProperty(LiricalResources.EXOMISER_HG38_VARIANT_PROPERTY);
        if (exomiserHg38VariantPath != null)
            // TODO(mabeckwith) - check if the path is a folder, if it is readable, etc.
            liricalResources.setExomiserHg38VariantDbFile(Path.of(exomiserHg38VariantPath));

        String backgroundFrequencyPath = properties.getProperty(LiricalResources.BACKGROUND_FREQUENCY_PROPERTY);
        if (backgroundFrequencyPath != null)
            // TODO(mabeckwith) - check if the path is a folder, if it is readable, etc.
            liricalResources.setBackgroundVariantFrequencyFile(Path.of(backgroundFrequencyPath));

        String pathogenicityThreshold = properties.getProperty(LiricalResources.PATHOGENICITY_PROPERTY);
        if (pathogenicityThreshold != null)
            liricalResources.setPathogenicityThreshold(Float.parseFloat(pathogenicityThreshold));

        String defaultVariantBackgroundFrequency = properties.getProperty(LiricalResources.DEFAULT_VARIANT_BACKGROUND_FREQUENCY_PROPERTY);
        if (defaultVariantBackgroundFrequency != null)
            liricalResources.setDefaultVariantBackgroundFrequency(Double.parseDouble(defaultVariantBackgroundFrequency));

        String isStrict = properties.getProperty(LiricalResources.STRICT_PROPERTY);
        if (isStrict != null)
            liricalResources.setStrict(Boolean.parseBoolean(isStrict));

        String defaultAlleleFrequency = properties.getProperty(LiricalResources.DEFAULT_ALLELE_PROPERTY);
        if (defaultAlleleFrequency != null)
            liricalResources.setDefaultAlleleFrequency(Float.parseFloat(defaultAlleleFrequency));

        String genomeBuild = properties.getProperty(LiricalResources.GENOME_BUILD_PROPERTY);
        if (genomeBuild != null)
            liricalResources.setGenomeBuild(GenomeBuild.valueOf(genomeBuild));

        String transcriptDatabase = properties.getProperty(LiricalResources.TRANSCRIPT_DATABASE_PROPERTY);
        if (transcriptDatabase != null)
            liricalResources.setTranscriptDatabase(TranscriptDatabase.valueOf(transcriptDatabase));

        // --------------------------- The rest ---------------------------
        String liricalResults = properties.getProperty(LIRICAL_RESULTS_PROPERTY);
        if (liricalResults != null)
            // TODO(mabeckwith) - check if the path is a folder, if it is readable, etc.
            this.liricalResults.set(Path.of(liricalResults));
    }

    /**
     * Store the non-{@code null} resources into provided {@linkplain Properties}.
     *
     * @param properties where to store the resources.
     */
    public void storeResources(Properties properties) {
        Objects.requireNonNull(properties);

        // MONDO path
        storePathIfNotNull(properties, OntologyResources.MONDO_JSON_PATH_PROPERTY, ontologyResources.getMondoPath());

        // Serialize LIRICAL resources
        storePathIfNotNull(properties, LiricalResources.LIRICAL_DATA_PROPERTY, liricalResources.getDataDirectory());
        storePathIfNotNull(properties, LiricalResources.EXOMISER_HG19_VARIANT_PROPERTY, liricalResources.getExomiserHg19VariantDbFile());
        storePathIfNotNull(properties, LiricalResources.EXOMISER_HG38_VARIANT_PROPERTY, liricalResources.getExomiserHg38VariantDbFile());
        storePathIfNotNull(properties, LiricalResources.BACKGROUND_FREQUENCY_PROPERTY, liricalResources.getBackgroundVariantFrequencyFile());
        // The properties below should never be null
        properties.setProperty(LiricalResources.PATHOGENICITY_PROPERTY, String.valueOf(liricalResources.getPathogenicityThreshold()));
        properties.setProperty(LiricalResources.DEFAULT_VARIANT_BACKGROUND_FREQUENCY_PROPERTY, String.valueOf(liricalResources.getDefaultVariantBackgroundFrequency()));
        properties.setProperty(LiricalResources.STRICT_PROPERTY, String.valueOf(liricalResources.isStrict()));
        properties.setProperty(LiricalResources.DEFAULT_ALLELE_PROPERTY, String.valueOf(liricalResources.getDefaultAlleleFrequency()));
        properties.setProperty(LiricalResources.GENOME_BUILD_PROPERTY, String.valueOf(liricalResources.getGenomeBuild()));
        properties.setProperty(LiricalResources.TRANSCRIPT_DATABASE_PROPERTY, liricalResources.getTranscriptDatabase().name());

        // LIRICAL results directory
        storePathIfNotNull(properties, LIRICAL_RESULTS_PROPERTY, liricalResults.get());
    }

    private static void storePathIfNotNull(Properties properties, String key, Path path) {
        if (path != null)
            properties.setProperty(key, path.toAbsolutePath().toString());
    }
}

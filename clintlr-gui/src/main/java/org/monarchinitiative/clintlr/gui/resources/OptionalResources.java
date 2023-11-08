package org.monarchinitiative.clintlr.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
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
        if (mondoJsonPath != null) {
            Path path = Path.of(mondoJsonPath);
            if ((Files.isRegularFile(path) && Files.isReadable(path) && mondoJsonPath.endsWith(".json")) || mondoJsonPath.equals("/path/to/mondo.json")) {
                ontologyResources.setMondoPath(path);
            }
        }

        // --------------------------- LIRICAL ---------------------------
        String liricalDataPath = properties.getProperty(LiricalResources.LIRICAL_DATA_PROPERTY);
        if (liricalDataPath != null) {
            Path path = Path.of(liricalDataPath);
            if ((Files.isDirectory(path) && Files.isReadable(path)) || liricalDataPath.equals("/path/to/lirical/datadir")) {
                liricalResources.setDataDirectory(path);
            }
        }

        String exomiserHg19VariantPath = properties.getProperty(LiricalResources.EXOMISER_HG19_VARIANT_PROPERTY);
        if (exomiserHg19VariantPath != null) {
            Path path = Path.of(exomiserHg19VariantPath);
            if ((Files.isRegularFile(path) && Files.isReadable(path) && exomiserHg19VariantPath.endsWith("mv.db")) || exomiserHg19VariantPath.equals("/path/to/exomiser.hg19.mv.db")) {
                liricalResources.setExomiserHg19VariantDbFile(path);
            }
        }

        String exomiserHg38VariantPath = properties.getProperty(LiricalResources.EXOMISER_HG38_VARIANT_PROPERTY);
        if (exomiserHg38VariantPath != null) {
            Path path = Path.of(exomiserHg38VariantPath);
            if ((Files.isRegularFile(path) && Files.isReadable(path) && exomiserHg38VariantPath.endsWith("mv.db"))  || exomiserHg38VariantPath.equals("/path/to/exomiser.hg38.mv.db")) {
                liricalResources.setExomiserHg38VariantDbFile(path);
            }
        }

        String backgroundFrequencyPath = properties.getProperty(LiricalResources.BACKGROUND_FREQUENCY_PROPERTY);
        if (backgroundFrequencyPath != null) {
            Path path = Path.of(backgroundFrequencyPath);
            if ((Files.isRegularFile(path) && Files.isReadable(path))  || backgroundFrequencyPath.equals("/path/to/bg.freq.txt")) {
                liricalResources.setBackgroundVariantFrequencyFile(path);
            }
        }

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
        if (liricalResults != null) {
            Path path = Path.of(liricalResults);
            if ((Files.isDirectory(path) && Files.isReadable(path))  || liricalResults.equals("/path/to/lirical/results")) {
                this.liricalResults.set(path);
            }
        }
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

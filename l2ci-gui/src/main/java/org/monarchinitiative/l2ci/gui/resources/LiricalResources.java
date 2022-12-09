package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;

import java.nio.file.Path;

/**
 * Resources and parameters required for setting up {@link org.monarchinitiative.lirical.core.Lirical}.
 */
public class LiricalResources {
    // TODO(ielis) - ensure we persist LiricalResources between sessions
    public static final String LIRICAL_DATA_PROPERTY = "lirical.data.directory";

    public static final String EXOMISER_VARIANT_PROPERTY = "exomiser.variant.file";

    public static final String BACKGROUND_FREQUENCY_PROPERTY = "background.frequency.file";

    public static final String PATHOGENICITY_PROPERTY = "pathogenicity.threshold";

    public static final String DEFAULT_VARIANT_BACKGROUND_FREQUENCY_PROPERTY = "default.variant.background.frequency";

    public static final String STRICT_PROPERTY = "strict";

    public static final String DEFAULT_ALLELE_PROPERTY = "default.allele.frequency";

    public static final String GENOME_BUILD_PROPERTY = "genome.build";

    public static final String TRANSCRIPT_DATABASE_PROPERTY = "transcript.database";
    public static final float DEFAULT_PATHOGENICITY_THRESHOLD = .8f;
    // The frequencies are as a percentage, so this is 1/1000.
    public static final double DEFAULT_VARIANT_BACKGROUND_FREQUENCY = .1;

    private final ObjectProperty<Path> dataDirectory = new SimpleObjectProperty<>(this, "dataDirectory");
    private final ObjectProperty<Path> exomiserVariantDbFile = new SimpleObjectProperty<>(this, "exomiserVariantDbFile");
    private final ObjectProperty<Path> backgroundVariantFrequencyFile = new SimpleObjectProperty<>(this, "backgroundVariantFrequencyFile");
    private final FloatProperty pathogenicityThreshold = new SimpleFloatProperty(this, "pathogenicityThreshold", DEFAULT_PATHOGENICITY_THRESHOLD);
    private final DoubleProperty defaultVariantBackgroundFrequency = new SimpleDoubleProperty(this, "defaultVariantBackgroundFrequency", DEFAULT_VARIANT_BACKGROUND_FREQUENCY);
    private final BooleanProperty strict = new SimpleBooleanProperty(this, "strict", false);
    private final FloatProperty defaultAlleleFrequency = new SimpleFloatProperty(this, "defaultAlleleFrequency", VariantMetadataService.DEFAULT_FREQUENCY);
    private final ObjectProperty<GenomeBuild> genomeBuild = new SimpleObjectProperty<>(this, "genomeBuild", GenomeBuild.HG38);
    private final ObjectProperty<TranscriptDatabase> transcriptDatabase = new SimpleObjectProperty<>(this, "transcriptDatabase", TranscriptDatabase.REFSEQ);
    private final BooleanBinding resourcesAreComplete = dataDirectory.isNotNull()
            .and(exomiserVariantDbFile.isNotNull());

    LiricalResources() {
    }

    /**
     * @return binding that returns {@code true} if all resources
     */
    public BooleanBinding resourcesAreComplete() {
        return resourcesAreComplete;
    }

    public Path getDataDirectory() {
        return dataDirectory.get();
    }

    public ObjectProperty<Path> dataDirectoryProperty() {
        return dataDirectory;
    }

    public void setDataDirectory(Path dataDirectory) {
        this.dataDirectory.set(dataDirectory);
    }

    public Path getExomiserVariantDbFile() {
        return exomiserVariantDbFile.get();
    }

    public ObjectProperty<Path> exomiserVariantDbFileProperty() {
        return exomiserVariantDbFile;
    }

    public void setExomiserVariantDbFile(Path exomiserVariantDbFile) {
        this.exomiserVariantDbFile.set(exomiserVariantDbFile);
    }

    public Path getBackgroundVariantFrequencyFile() {
        return backgroundVariantFrequencyFile.get();
    }

    public ObjectProperty<Path> backgroundVariantFrequencyFileProperty() {
        return backgroundVariantFrequencyFile;
    }

    public void setBackgroundVariantFrequencyFile(Path backgroundVariantFrequencyFile) {
        this.backgroundVariantFrequencyFile.set(backgroundVariantFrequencyFile);
    }

    public float getPathogenicityThreshold() {
        return pathogenicityThreshold.get();
    }

    public FloatProperty pathogenicityThresholdProperty() {
        return pathogenicityThreshold;
    }

    public void setPathogenicityThreshold(float pathogenicityThreshold) {
        this.pathogenicityThreshold.set(pathogenicityThreshold);
    }

    public double getDefaultVariantBackgroundFrequency() {
        return defaultVariantBackgroundFrequency.get();
    }

    public DoubleProperty defaultVariantBackgroundFrequencyProperty() {
        return defaultVariantBackgroundFrequency;
    }

    public void setDefaultVariantBackgroundFrequency(double defaultVariantBackgroundFrequency) {
        this.defaultVariantBackgroundFrequency.set(defaultVariantBackgroundFrequency);
    }

    public boolean isStrict() {
        return strict.get();
    }

    public BooleanProperty strictProperty() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict.set(strict);
    }

    public float getDefaultAlleleFrequency() {
        return defaultAlleleFrequency.get();
    }

    public FloatProperty defaultAlleleFrequencyProperty() {
        return defaultAlleleFrequency;
    }

    public void setDefaultAlleleFrequency(float defaultAlleleFrequency) {
        this.defaultAlleleFrequency.set(defaultAlleleFrequency);
    }

    public GenomeBuild getGenomeBuild() {
        return genomeBuild.get();
    }

    public ObjectProperty<GenomeBuild> genomeBuildProperty() {
        return genomeBuild;
    }

    public void setGenomeBuild(GenomeBuild genomeBuild) {
        this.genomeBuild.set(genomeBuild);
    }

    public TranscriptDatabase getTranscriptDatabase() {
        return transcriptDatabase.get();
    }

    public ObjectProperty<TranscriptDatabase> transcriptDatabaseProperty() {
        return transcriptDatabase;
    }

    public void setTranscriptDatabase(TranscriptDatabase transcriptDatabase) {
        this.transcriptDatabase.set(transcriptDatabase);
    }
}

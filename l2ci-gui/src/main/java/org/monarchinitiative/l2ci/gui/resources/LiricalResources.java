package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.*;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;

import java.nio.file.Path;

/**
 * Resources and parameters required for setting up {@link org.monarchinitiative.lirical.core.Lirical}.
 */
public class LiricalResources {
    static final String LIRICAL_DATA_PROPERTY = "lirical.data.directory";
    static final String EXOMISER_HG19_VARIANT_PROPERTY = "lirical.exomiser.hg19.variant.file";
    static final String EXOMISER_HG38_VARIANT_PROPERTY = "lirical.exomiser.hg38.variant.file";
    static final String BACKGROUND_FREQUENCY_PROPERTY = "lirical.background.frequency.file";
    static final String PATHOGENICITY_PROPERTY = "lirical.pathogenicity.threshold";
    static final String DEFAULT_VARIANT_BACKGROUND_FREQUENCY_PROPERTY = "lirical.default.variant.background.frequency";
    static final String STRICT_PROPERTY = "lirical.strict";
    static final String DEFAULT_ALLELE_PROPERTY = "lirical.default.allele.frequency";
    static final String GENOME_BUILD_PROPERTY = "lirical.genome.build";
    static final String TRANSCRIPT_DATABASE_PROPERTY = "lirical.transcript.database";
    public static final float DEFAULT_PATHOGENICITY_THRESHOLD = .8f;
    // The frequencies are as a percentage, so this is 1/1000.
    public static final double DEFAULT_VARIANT_BACKGROUND_FREQUENCY = .1;

    private final ObjectProperty<Path> dataDirectory = new SimpleObjectProperty<>(this, "dataDirectory");
    private final ObjectProperty<Path> exomiserHg19VariantDbFile = new SimpleObjectProperty<>(this, "exomiserHg19VariantDbFile");
    private final ObjectProperty<Path> exomiserHg38VariantDbFile = new SimpleObjectProperty<>(this, "exomiserHg38VariantDbFile");
    private final ObjectProperty<Path> backgroundVariantFrequencyFile = new SimpleObjectProperty<>(this, "backgroundVariantFrequencyFile");
    private final FloatProperty pathogenicityThreshold = new SimpleFloatProperty(this, "pathogenicityThreshold", DEFAULT_PATHOGENICITY_THRESHOLD);
    private final DoubleProperty defaultVariantBackgroundFrequency = new SimpleDoubleProperty(this, "defaultVariantBackgroundFrequency", DEFAULT_VARIANT_BACKGROUND_FREQUENCY);
    private final BooleanProperty strict = new SimpleBooleanProperty(this, "strict", false);
    private final FloatProperty defaultAlleleFrequency = new SimpleFloatProperty(this, "defaultAlleleFrequency", VariantMetadataService.DEFAULT_FREQUENCY);
    private final ObjectProperty<GenomeBuild> genomeBuild = new SimpleObjectProperty<>(this, "genomeBuild", GenomeBuild.HG38);
    private final ObjectProperty<TranscriptDatabase> transcriptDatabase = new SimpleObjectProperty<>(this, "transcriptDatabase", TranscriptDatabase.REFSEQ);

    LiricalResources() {
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

    public Path getExomiserHg19VariantDbFile() {
        return exomiserHg19VariantDbFile.get();
    }

    public ObjectProperty<Path> exomiserHg19VariantDbFileProperty() {
        return exomiserHg19VariantDbFile;
    }

    public void setExomiserHg19VariantDbFile(Path exomiserHg19VariantDbFile) {
        this.exomiserHg19VariantDbFile.set(exomiserHg19VariantDbFile);
    }

    public Path getExomiserHg38VariantDbFile() {
        return exomiserHg38VariantDbFile.get();
    }

    public ObjectProperty<Path> exomiserHg38VariantDbFileProperty() {
        return exomiserHg38VariantDbFile;
    }

    public void setExomiserHg38VariantDbFile(Path exomiserHg38VariantDbFile) {
        this.exomiserHg38VariantDbFile.set(exomiserHg38VariantDbFile);
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

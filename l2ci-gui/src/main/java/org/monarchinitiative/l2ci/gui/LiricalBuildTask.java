package org.monarchinitiative.l2ci.gui;

import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.gui.controller.MainController;
import org.monarchinitiative.lirical.configuration.GenotypeLrProperties;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * Initialization of the Lirical build is being done here. Information from {@link Properties} parsed from
 * <code>hpo-case-annotator.properties</code> are being read and following resources are initialized:
 * <ul>
 * <li>Human phenotype ontology OBO file</li>
 * </ul>
 * <p>
 * Changes made by user are stored for the next run in
 *
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 2.0.0
 * @since 0.0
 */
public final class LiricalBuildTask extends Task<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalBuildTask.class);

    private final Properties pgProperties;


    public LiricalBuildTask(Properties pgProperties) {
        this.pgProperties = pgProperties;
    }

    /**
     * Read {@link Properties} and initialize app resources in the :
     *
     * <ul>
     * <li>HPO ontology</li>
     * </ul>
     *
     * @return nothing
     */
    @Override
    protected Void call() throws Exception {
        String dataPath = pgProperties.getProperty("lirical.data.path");
        if (dataPath != null) {
            LOGGER.info("Building LIRICAL");
            updateProgress(0.02, 1);
            LOGGER.info("LIRICAL data directory: {}", dataPath);
            Path liricalDataPath = Path.of(dataPath);
            String exomiserVariant = pgProperties.getProperty("exomiser.variant.path");
            String backgroundFrequency = pgProperties.getProperty("background.frequency.path");
            GenomeBuild genomeBuild = getGenomeBuild();
            LiricalBuilder liricalBuilder = LiricalBuilder.builder(liricalDataPath);
            liricalBuilder.setDiseaseDatabases(Set.of(DiseaseDatabase.OMIM));
            if (genomeBuild != null) {
                LOGGER.info("Genome Build: " + genomeBuild);
                liricalBuilder.genomeBuild(genomeBuild);
            } else {
                PopUps.showInfoMessage("No Genome Build specified (see File -> Show Resources menu).", "LIRICAL Configuration Error");
                LOGGER.info("No Genome Build specified (see File -> Show Resources menu).");
            }
            addToBuilder(liricalBuilder, backgroundFrequency, "Background frequency");
            addToBuilder(liricalBuilder, exomiserVariant, "Exomiser variant");
            updateProgress(0.25, 1);
            float pathogenicityThreshold = Float.parseFloat(pgProperties.getProperty("pathogenicity.threshold"));
            double defaultVariantBackgroundFrequency = Double.parseDouble(pgProperties.getProperty("default.variant.background.frequency"));
            boolean strict = Boolean.parseBoolean(pgProperties.getProperty("strict"));
            GenotypeLrProperties genotypeLrProperties = new GenotypeLrProperties(pathogenicityThreshold, defaultVariantBackgroundFrequency, strict);
            LOGGER.info("Genotype Properties: " + genotypeLrProperties);
            liricalBuilder.genotypeLrProperties(genotypeLrProperties);
            float defaultAlleleFrequency = Float.parseFloat(pgProperties.getProperty("default.allele.frequency"));
            LOGGER.info("Default Allele Frequency: " + defaultAlleleFrequency);
            liricalBuilder.defaultVariantAlleleFrequency(defaultAlleleFrequency);
            TranscriptDatabase transcriptDatabase = getTranscriptDB();
            if (transcriptDatabase != null) {
                LOGGER.info("Transcript Database: " + transcriptDatabase);
                liricalBuilder.transcriptDatabase(transcriptDatabase);
            } else {
                PopUps.showInfoMessage("No Transcript Database specified (see File -> Show Resources menu)", "Missing LIRICAL Resource");
                LOGGER.info("No Transcript Database specified (see File -> Show Resources menu)");
            }
            updateProgress(0.5, 1);
            MainController.getController().lirical = liricalBuilder.build();
            updateProgress(0.75, 1);
            updateProgress(1, 1);
            LOGGER.info("Finished building LIRICAL");
        } else {
            PopUps.showInfoMessage("No LIRICAL data directory set (see File -> Show Resources Menu). Aborting building LIRICAL.", "Error Building LIRICAL");
            LOGGER.error("No LIRICAL data directory set (see File -> Show Resources Menu). Aborting building LIRICAL.");
            MainController.getController().lirical = null;
        }
        return null;
    }

    GenomeBuild getGenomeBuild() {
        String genome = pgProperties.getProperty("genome.build");
        GenomeBuild genomeBuild = null;
        if (genome.equals("hg19")) {
            genomeBuild = GenomeBuild.HG19;
        } else if (genome.equals("hg38")) {
            genomeBuild = GenomeBuild.HG38;
        }
        return genomeBuild;
    }

    TranscriptDatabase getTranscriptDB() {
        String transcript = pgProperties.getProperty("transcript.database");
        TranscriptDatabase transcriptDB = null;
        if (transcript.equals("refSeq")) {
            transcriptDB = TranscriptDatabase.REFSEQ;
        } else if (transcript.equals("UCSC")) {
            transcriptDB = TranscriptDatabase.UCSC;
        }
        return transcriptDB;
    }

    void addToBuilder(LiricalBuilder builder, String pathName, String fileName) {
        if (pathName != null && new File(pathName).isFile()) {
            Path path = Path.of(pathName);
            LOGGER.info(fileName + " file: {}", pathName);
            if (fileName.contains("Exomiser")) {
                builder.exomiserVariantDatabase(path);
            } else if (fileName.contains("Background")) {
                builder.backgroundVariantFrequency(path);
            }
        } else {
            PopUps.showInfoMessage("Path to " + fileName + " file not set (see File -> Show Resources menu).", "Missing LIRICAL Resource");
            LOGGER.info("Path to " + fileName + " file not set (see File -> Show Resources menu).");
        }
    }
}

package org.monarchinitiative.l2ci.gui;

import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoaResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalMondoResource;
import org.monarchinitiative.lirical.configuration.GenotypeLrProperties;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Initialization of the GUI resources is being done here. Information from {@link Properties} parsed from
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
public final class StartupTask extends Task<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupTask.class);

    private final OptionalHpoResource optionalHpoResource;

    private final OptionalHpoaResource optionalHpoaResource;

    private final OptionalMondoResource optionalMondoResource;

    private final Properties pgProperties;

    public StartupTask(OptionalHpoResource hpoResource,
                       OptionalHpoaResource hpoaResource,
                       OptionalMondoResource mondoResource, Properties pgProperties) {
        this.pgProperties = pgProperties;
        this.optionalHpoResource = hpoResource;
        this.optionalHpoaResource = hpoaResource;
        this.optionalMondoResource = mondoResource;
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
    protected Void call() {
        /*
        This is the place where we deserialize HPO ontology if we know path to the OBO file.
        We need to make sure to set ontology property of `optionalResources` to null if loading fails.
        This way we ensure that GUI elements dependent on ontology presence (labels, buttons) stay disabled
        and that the user will be notified about the fact that the ontology is missing.
         */
        String hpoJsonPath = pgProperties.getProperty(OptionalHpoResource.HP_JSON_PATH_PROPERTY);
        String hpoAnnotPath = pgProperties.getProperty(OptionalHpoaResource.HPOA_PATH_PROPERTY);
        String mondoJsonPath = pgProperties.getProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY);
        updateProgress(0.02, 1);
        String[] paths = {mondoJsonPath, hpoJsonPath, hpoAnnotPath};
        String[] types = {"Mondo", "HPO", "LIRICAL/data/phenotype.hpoa"};
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            String type = types[i];
            if (path != null) {
                File file = new File(path);
                if (file.isFile()) {
                    String msg = String.format("Loading " + type + " from file '%s'", file.getAbsoluteFile());
                    updateMessage(msg);
                    LOGGER.info(msg);
                    switch (type) {
                        case "Mondo":
                            HPOParser parser = new HPOParser(mondoJsonPath);
                            Ontology ontology = parser.getHPO();
                            setOntology(type, ontology, "");
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case "HPO":
                            ontology = OntologyLoader.loadOntology(file);
                            optionalHpoResource.setOntology(ontology);
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case "LIRICAL/data/phenotype.hpoa":
                            if (optionalHpoResource.getOntology() == null) {
                                LOGGER.error("Cannot load phenotype.hpoa because HP ontology not loaded");
                                return null;
                            }
                            setOntology(type, optionalHpoResource.getOntology(), path);
                            LOGGER.info("Loaded annotation maps");
                            break;
                        }
                    updateProgress((i+1)/paths.length, 1);
                    updateMessage(type + " loaded");
                } else {
                    setOntology(type, null, "");
                }
            } else {
                String msg = "Need to set path to " + type + " file (See File -> Show Resources menu)";
                updateMessage(msg);
                LOGGER.info(msg);
                setOntology(type, null, "");
            }
        }
        updateProgress(1, 1);
        return null;
    }

    void setOntology(String type, Ontology ont, String path) {
        switch (type) {
            case "Mondo":
                optionalMondoResource.setOntology(ont);
                break;
            case "HPO":
                optionalHpoResource.setOntology(ont);
                break;
            case "LIRICAL/data/phenotype.hpoa":
                if (ont != null) {
                    optionalHpoaResource.setAnnotationResources(path, ont);
                } else {
                    optionalHpoaResource.initializeWithEmptyMaps();
                }
                break;
        }
    }

    public Lirical buildLirical() throws Exception {
        Lirical lirical;
        String dataPath = pgProperties.getProperty("lirical.data.path");
        if (dataPath != null) {
            LOGGER.info("Building LIRICAL");
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
                LOGGER.info("No Genome Build specified (see File -> Show Resources menu).");
            }
            addToBuilder(liricalBuilder, backgroundFrequency, "Background frequency");
            addToBuilder(liricalBuilder, exomiserVariant, "Exomiser variant");
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
                LOGGER.info("No Transcript Database specified (see File -> Show Resources menu)");
            }
            lirical = liricalBuilder.build();
            LOGGER.info("Finished building LIRICAL");
        } else {
            LOGGER.error("No LIRICAL data directory set (see File -> Show Resources Menu). Aborting building LIRICAL.");
            lirical = null;
        }

        return lirical;
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
            LOGGER.info("Path to " + fileName + " file not set (see File -> Show Resources menu).");
        }
    }
}

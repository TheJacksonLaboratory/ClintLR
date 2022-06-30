package org.monarchinitiative.l2ci.gui;

import com.google.common.base.Optional;
import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoaResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalMondoResource;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
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
        if (mondoJsonPath != null) {
            final File mondoJsonFile = new File(mondoJsonPath);
            updateProgress(0.03, 1);
            if (mondoJsonFile.isFile()) {
                String msg = String.format("Loading Mondo from file '%s'", mondoJsonFile.getAbsoluteFile());
                updateMessage(msg);
                LOGGER.info(msg);
                HPOParser parser = new HPOParser(mondoJsonPath);
                final Ontology ontology = parser.getHPO();
                updateProgress(0.25, 1);
                optionalMondoResource.setOntology(ontology);
                updateProgress(0.30, 1);
                updateMessage("Mondo loaded");
                LOGGER.info("Loaded Mondo ontology");
            } else {
                optionalMondoResource.setOntology(null);
            }
        } else {
            String msg = "Need to load mondo.json file (See File menu)";
            updateMessage(msg);
            LOGGER.info(msg);
            optionalMondoResource.setOntology(null);
        }
        if (hpoJsonPath != null) {
            final File hpJsonFile = new File(hpoJsonPath);
            updateProgress(0.03, 1);
            if (hpJsonFile.isFile()) {
                String msg = String.format("Loading HPO from file '%s'", hpJsonFile.getAbsoluteFile());
                updateMessage(msg);
                LOGGER.info(msg);
                final Ontology ontology = OntologyLoader.loadOntology(hpJsonFile);
                updateProgress(0.25, 1);
                optionalHpoResource.setOntology(ontology);
                updateProgress(0.30, 1);
                updateMessage("HPO loaded");
                LOGGER.info("Loaded HPO ontology");
            } else {
                optionalHpoResource.setOntology(null);
            }
        } else {
            String msg = "Need to set path to hp.json file (See File -> Show Resources menu)";
            updateMessage(msg);
            LOGGER.info(msg);
            optionalHpoResource.setOntology(null);
        }
        if (hpoAnnotPath != null) {
            String msg = String.format("Loading phenotype.hpoa from file '%s'", hpoAnnotPath);
            updateMessage(msg);
            LOGGER.info(msg);
            final File hpoAnnotFile = new File(hpoAnnotPath);
            updateProgress(0.71, 1);
            if (optionalHpoResource.getOntology() == null) {
                LOGGER.error("Cannot load phenotype.hpoa because HP ontology not loaded");
                return null;
            }
            if (hpoAnnotFile.isFile()) {
                updateProgress(0.78, 1);
                this.optionalHpoaResource.setAnnotationResources(hpoAnnotPath, optionalHpoResource.getOntology());
                updateProgress(0.95, 1);
                LOGGER.info("Loaded annotation maps");
            } else {
                optionalHpoaResource.initializeWithEmptyMaps();
                LOGGER.error("Cannot load phenotype.hpoa File was null");
            }
        } else {
            String msg = "Need to set path to phenotype.hpoa file (See File -> Show Resources menu)";
            updateMessage(msg);
            LOGGER.info(msg);
            optionalHpoResource.setOntology(null);
        }
        updateProgress(1, 1);
        return null;
    }

    public Lirical buildLirical() throws Exception {
        Lirical lirical;
        String dataPath = pgProperties.getProperty("lirical.data.path");
        if (dataPath != null) {
            LOGGER.info("Building LIRICAL");
            LOGGER.info("LIRICAL data directory: {}", dataPath);
            Path liricalDataPath = Path.of(dataPath);
            String exomiserVariant = pgProperties.getProperty("exomiser.variant.path");
            LiricalBuilder liricalBuilder = LiricalBuilder.builder(liricalDataPath);
            liricalBuilder.setDiseaseDatabases(Set.of(DiseaseDatabase.OMIM));
            if (exomiserVariant != null && new File(exomiserVariant).isFile()) {
                LOGGER.info("Exomiser variant file: {}", exomiserVariant);
                liricalBuilder.exomiserVariantDatabase(Path.of(exomiserVariant));
            } else {
                LOGGER.info("Path to Exomiser variant file not set (see Edit menu or File -> Show Resources menu). Building LIRICAL without Exomiser variant file.");
            }
//                .genomeBuild(genomeBuildOptional.get())
//                .backgroundVariantFrequency(dataSection.backgroundFrequencyFile)
//                .genotypeLrProperties(genotypeLrProperties)
//                .transcriptDatabase(runConfiguration.transcriptDb)
//                .defaultVariantAlleleFrequency(runConfiguration.defaultAlleleFrequency)
            lirical = liricalBuilder.build();
            LOGGER.info("Finished building LIRICAL");
        } else {
            LOGGER.error("No LIRICAL data directory set (see Edit Menu). Aborting building LIRICAL.");
            lirical = null;
        }

        return lirical;
    }
}

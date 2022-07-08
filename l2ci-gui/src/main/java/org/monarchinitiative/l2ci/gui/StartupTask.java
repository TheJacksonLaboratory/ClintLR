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
        String[] paths = {mondoJsonPath, hpoJsonPath, hpoAnnotPath};
        String[] types = {"Mondo", "HPO", "phenotype.hpoa"};
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
                        case "phenotype.hpoa":
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
            case "phenotype.hpoa":
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

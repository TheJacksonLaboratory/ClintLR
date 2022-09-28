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
 * <li>Human phenotype and Mondo ontology JSON files</li>
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

    private enum Type {
        HPO("HPO"),
        HPOA("phenotype.hpoa"),
        Mondo("Mondo");

        private final String name;

        Type(String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

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
        Type[] types = {Type.Mondo, Type.HPO, Type.HPOA};
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            Type type = types[i];
            if (path != null) {
                File file = new File(path);
                if (file.isFile()) {
                    String msg = String.format("Loading " + type + " from file '%s'", file.getAbsoluteFile());
                    updateMessage(msg);
                    LOGGER.info(msg);
                    switch (type) {
                        case Mondo:
                            HPOParser parser = new HPOParser(mondoJsonPath);
                            Ontology ontology = parser.getHPO();
                            setOntology(type, ontology, "");
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case HPO:
                            ontology = OntologyLoader.loadOntology(file);
                            optionalHpoResource.setOntology(ontology);
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case HPOA:
                            if (optionalHpoResource.getOntology() == null) {
                                LOGGER.error("Cannot load phenotype.hpoa because HP ontology not loaded");
                                return null;
                            }
                            setOntology(type, optionalHpoResource.getOntology(), path);
                            LOGGER.info("Loaded annotation maps");
                            break;
                        }
                    updateProgress((double) (i+1)/paths.length, 1);
                    updateMessage(type + " loaded");
                } else {
                    setOntology(type, null, "");
                }
                LOGGER.info("Finished loading " + type + " ontology.");
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

    void setOntology(Type type, Ontology ont, String path) {
        switch (type) {
            case Mondo:
                optionalMondoResource.setOntology(ont);
                break;
            case HPO:
                optionalHpoResource.setOntology(ont);
                break;
            case HPOA:
                if (ont != null) {
                    optionalHpoaResource.setAnnotationResources(path, ont);
                } else {
                    optionalHpoaResource.initializeWithEmptyMaps();
                }
                break;
        }
    }

}

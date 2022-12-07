package org.monarchinitiative.l2ci.gui.tasks;

import javafx.beans.value.ChangeListener;
import org.monarchinitiative.l2ci.gui.resources.*;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Initialization of the GUI resources is being done here. Information from {@link Properties} parsed from
 * <code>hpo-case-annotator.properties</code> are being read and following resources are initialized:
 * <ul>
 * <li>Human phenotype and Mondo ontology JSON files</li>
 * </ul>
 * <p>
 * Changes made by user are stored for the next run in a property file.
 *
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 2.0.0
 * @since 0.0
 */
@Component
public class StartupTask implements ApplicationListener<ApplicationStartedEvent>, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupTask.class);

    private final OptionalResources optionalResources;
    private final OptionalServices optionalServices;
    private final Path dataDirectory;
    private final ExecutorService executorService;
    private final Properties pgProperties;

    public StartupTask(OptionalResources optionalResources,
                       OptionalServices optionalServices,
                       Path dataDirectory,
                       ExecutorService executorService,
                       Properties pgProperties) {
        this.optionalResources = optionalResources;
        this.optionalServices = optionalServices;
        this.dataDirectory = dataDirectory;
        this.executorService = executorService;
        this.pgProperties = pgProperties;
    }

    @Override
    public void run() {
        /*
        This is the place where we deserialize HPO ontology if we know path to the OBO file.
        We need to make sure to set ontology property of `optionalResources` to null if loading fails.
        This way we ensure that GUI elements dependent on ontology presence (labels, buttons) stay disabled
        and that the user will be notified about the fact that the ontology is missing.
         */
        LOGGER.debug("Triggering startup tasks");
        triggerLoadingTasks();

        // Mondo
        String mondoJsonPath = pgProperties.getProperty(OntologyResources.MONDO_JSON_PATH_PROPERTY);
        if (mondoJsonPath != null)
            // TODO(mabeckwith) - check if the path is a file, if it ends with *.json, if it is readable, etc.
            optionalResources.ontologyResources().setMondoPath(Path.of(mondoJsonPath));

        // Lirical
        String liricalDataPath = pgProperties.getProperty(LiricalResources.LIRICAL_DATA_PROPERTY);
        if (liricalDataPath != null)
            // TODO(mabeckwith) - check if the path is a folder, if it is readable, etc.
            optionalResources.liricalResources().setDataDirectory(Path.of(liricalDataPath));


        // TODO(mabeckwith) - set the remaining LIRICAL properties to LiricalResources

        // TODO - set the remaining paths if present in the properties

//        String[] paths = {mondoJsonPath, hpoJsonPath, hpoAnnotPath};
//        Type[] types = {Type.Mondo, Type.HPO, Type.HPOA};
//        for (int i = 0; i < paths.length; i++) {
//            String path = paths[i];
//            Type type = types[i];
//            if (path != null) {
//                File file = new File(path);
//                if (file.isFile()) {
//                    String msg = String.format("Loading " + type + " from file '%s'", file.getAbsoluteFile());
////                    updateMessage(msg);
//                    LOGGER.info(msg);
//                    switch (type) {
//                        case Mondo:
//                            HPOParser parser = new HPOParser(mondoJsonPath);
//                            Ontology ontology = parser.getHPO();
//                            setOntology(type, ontology, "");
//                            LOGGER.info("Loaded " + type + " ontology");
//                            break;
//                        case HPO:
//                            ontology = OntologyLoader.loadOntology(file);
//                            optionalHpoResource.setOntology(ontology);
//                            LOGGER.info("Loaded " + type + " ontology");
//                            break;
//                        case HPOA:
//                            if (optionalHpoResource.getOntology() == null) {
//                                LOGGER.error("Cannot load phenotype.hpoa because HP ontology not loaded");
////                                return null;
//                            }
//                            setOntology(type, optionalHpoResource.getOntology(), path);
//                            LOGGER.info("Loaded annotation maps");
//                            break;
//                        }
////                    updateProgress((double) (i+1)/paths.length, 1);
////                    updateMessage(type + " loaded");
//                } else {
//                    setOntology(type, null, "");
//                }
//                LOGGER.info("Finished loading " + type + " ontology.");
//            } else {
//                String msg = "Need to set path to " + type + " file (See File -> Show Resources menu)";
////                updateMessage(msg);
//                LOGGER.info(msg);
//                setOntology(type, null, "");
//            }
//        }
//        updateProgress(1, 1);
//        return null;
    }

    private void triggerLoadingTasks() {
        // Load Mondo
        optionalResources.ontologyResources().mondoPathProperty()
                .addListener(loadOntology(executorService, optionalServices));

        // Load Mondo map data when Mondo is ready
        optionalServices.mondoProperty()
                .addListener(loadMondoMeta(executorService, optionalServices, dataDirectory));

        // Load HPO
        // Load HpoDiseases
        // TODO - implement or not
//        resources.ontologyResources().hpoPathProperty()
//                .addListener(loadOntology(executor, services::setHpo, "HPO"));

        // Load LIRICAL
        optionalResources.liricalResources().resourcesAreComplete()
                .addListener(loadLirical(executorService, optionalServices, optionalResources.liricalResources()));

    }

    private static ChangeListener<Path> loadOntology(ExecutorService executor,
                                                     OptionalServices services) {
        return (obs, old, novel) -> {
            if (novel == null)
                services.setMondo(null);
            else {
                LoadOntologyTask task = new LoadOntologyTask(novel);
                task.setOnSucceeded(e -> {
                    LOGGER.debug("Mondo was loaded");
                    services.setMondo(task.getValue());
                });
                task.setOnFailed(e -> LOGGER.error("Could not load Mondo ontology from {}", novel.toAbsolutePath()));
                executor.submit(task);
            }
        };
    }

    private static ChangeListener<Ontology> loadMondoMeta(ExecutorService executor,
                                                          OptionalServices services,
                                                          Path dataDirectory) {
        return (obs, old, mondo) -> {
            MondoOmimResources mmr = services.mondoOmimResources();
            if (mondo == null) {
                mmr.setOmimToMondo(Map.of());
                mmr.setMondoNDescendents(Map.of());
            } else {
                MondoOmimTask task = new MondoOmimTask(mondo, dataDirectory);
                task.setOnSucceeded(e -> {
                    LOGGER.debug("Mondo meta was loaded");
                    MondoOmim mm = task.getValue();
                    mmr.setOmimToMondo(mm.omimToMondo());
                    mmr.setMondoNDescendents(mm.mondoNDescendents());
                });
                task.setOnFailed(e -> LOGGER.error("Could not load Mondo meta"));
                executor.submit(task);
            }
        };
    }

    private static ChangeListener<? super Boolean> loadLirical(ExecutorService executor,
                                                               OptionalServices services,
                                                               LiricalResources liricalResources) {
        return (obs, old, resourcesAreComplete) -> {
            if (resourcesAreComplete) {
                LiricalBuildTask task = new LiricalBuildTask(liricalResources);
                task.setOnSucceeded(e -> {
                    LOGGER.debug("LIRICAL setup was completed");
                    services.setLirical(task.getValue());
                });
                task.setOnFailed(e -> LOGGER.error("Could not load LIRICAL"));
                executor.submit(task);
            }
        };
    }


    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ExecutorService executorService = event.getApplicationContext().getBean(ExecutorService.class);
        executorService.submit(this);
    }
}

package org.monarchinitiative.clintlr.gui.tasks;

import javafx.beans.value.ChangeListener;
import org.monarchinitiative.clintlr.gui.resources.LiricalResources;
import org.monarchinitiative.clintlr.gui.resources.MondoOmimResources;
import org.monarchinitiative.clintlr.gui.resources.OptionalResources;
import org.monarchinitiative.clintlr.gui.resources.OptionalServices;
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
 * Initialization of the GUI resources is being done here. We trigger listeners on {@link OptionalResources}
 * and then set analysis parameters and paths to resource files. The parameters and paths are extracted
 * from {@link Properties} backed by <code>clintlr.properties</code> stored in the app's directory.
 *
 * @author <a href="mailto:martha.beckwith@jax.org">Martha Beckwith</a>
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
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

        // It is crucial to init resources AFTER triggering the loading tasks.
        optionalResources.initResources(pgProperties);
    }

    private void triggerLoadingTasks() {
        // Load Mondo
        optionalResources.ontologyResources().mondoPathProperty()
                .addListener(loadOntology(executorService, optionalServices));

        // Load Mondo map data when Mondo is ready
        optionalServices.mondoProperty()
                .addListener(loadMondoMeta(executorService, optionalServices, dataDirectory));

        // Load LIRICAL
        optionalResources.liricalResources().dataDirectoryProperty()
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
                task.setOnFailed(e -> LOGGER.error("Could not load Mondo ontology from {}", novel.toAbsolutePath(), e.getSource().getException()));
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
                LOGGER.debug("Mondo was null. Initializing with empty Mondo metadata.");
                mmr.setOmimToMondo(Map.of());
                mmr.mondoToOmimProperty().clear();
                mmr.mondoNDescendentsProperty().clear();
            } else {
                MondoOmimTask task = new MondoOmimTask(mondo, dataDirectory);
                task.setOnSucceeded(e -> {
                    LOGGER.debug("Mondo meta was loaded");
                    MondoOmim mm = task.getValue();
                    mmr.setOmimToMondo(mm.omimToMondo());
                    mmr.mondoToOmimProperty().putAll(mm.mondoToOmim());
                    mmr.mondoNDescendentsProperty().putAll(mm.mondoNDescendents());
                });
                task.setOnFailed(e -> LOGGER.error("Could not load Mondo meta", e.getSource().getException()));
                executor.submit(task);
            }
        };
    }

    private static ChangeListener<? super Path> loadLirical(ExecutorService executor,
                                                               OptionalServices services,
                                                               LiricalResources liricalResources) {
        return (obs, old, novel) -> {
            if (novel == null) {
                LOGGER.debug("Unsetting LIRICAL");
                services.setLirical(null);
            } else {
                LOGGER.debug("Configuring LIRICAL");
                LiricalBuildTask task = new LiricalBuildTask(liricalResources);
                task.setOnSucceeded(e -> {
                    LOGGER.debug("LIRICAL setup was completed");
                    services.setLirical(task.getValue());
                });
                task.setOnFailed(e -> {
                    LOGGER.error("Could not load LIRICAL: {}", e.getSource().getException().getMessage());
                    LOGGER.debug("Could not load LIRICAL", e.getSource().getException());
                });
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

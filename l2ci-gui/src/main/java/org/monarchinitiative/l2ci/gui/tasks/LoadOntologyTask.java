package org.monarchinitiative.l2ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class LoadOntologyTask extends Task<Ontology> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadOntologyTask.class);

    private final Path ontologyPath;

    public LoadOntologyTask(Path ontologyPath) {
        this.ontologyPath = ontologyPath;
    }

    @Override
    protected Ontology call() {
        LOGGER.debug("Loading ontology from {}", ontologyPath.toAbsolutePath());
        return OntologyLoader.loadOntology(ontologyPath.toFile(), "MONDO");
    }
}

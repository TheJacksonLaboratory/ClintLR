package org.monarchinitiative.l4ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.utils.CurieUtil;
import org.monarchinitiative.phenol.io.utils.CurieUtilBuilder;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class LoadOntologyTask extends Task<Ontology> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadOntologyTask.class);

    private final Path ontologyPath;

    public LoadOntologyTask(Path ontologyPath) {
        this.ontologyPath = ontologyPath;
    }

    @Override
    protected Ontology call() {
        LOGGER.debug("Loading ontology from {}", ontologyPath.toAbsolutePath());
        CurieUtil newCurie = CurieUtilBuilder.withDefaultsAnd(Map.of("HGNC", "http://identifiers.org/hgnc/"));
        return OntologyLoader.loadOntology(ontologyPath.toFile(), newCurie, "MONDO");
    }
}

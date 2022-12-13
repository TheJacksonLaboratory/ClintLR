package org.monarchinitiative.l2ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.core.OntologyType;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.gui.resources.OntologyResources;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.utils.CurieUtil;
import org.monarchinitiative.phenol.io.utils.CurieUtilBuilder;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class LoadOntologyTask extends Task<Ontology> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadOntologyTask.class);

    private final Path ontologyPath;
    private final OntologyType type;

    public LoadOntologyTask(Path ontologyPath, OntologyType type) {
        this.ontologyPath = ontologyPath;
        this.type = type;
    }

    @Override
    protected Ontology call() {
        LOGGER.debug("Loading ontology from {}", ontologyPath.toAbsolutePath());
        Ontology ont = null;
        if (type.equals(OntologyType.MONDO)) {
            CurieUtil newCurie = CurieUtilBuilder.withDefaultsAnd(Map.of("HGNC", "http://identifiers.org/hgnc/"));
            ont = OntologyLoader.loadOntology(ontologyPath.toFile(), newCurie, "MONDO");
        } else if (type.equals(OntologyType.HPO)) {
            ont = OntologyLoader.loadOntology(ontologyPath.toFile());
        }
        return ont;
    }
}

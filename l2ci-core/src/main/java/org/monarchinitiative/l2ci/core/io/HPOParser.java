package org.monarchinitiative.l2ci.core.io;

import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HPOParser {
    private static Logger LOGGER = LoggerFactory.getLogger(HPOParser.class);

    private Ontology hpo = null;

    public HPOParser(String hpoPath) {
        LOGGER.trace(String.format("Initializing HPO obo parser for %s", hpoPath));
        parse(hpoPath);
    }

    private void parse(String path) {
        File f = new File(path);
        if (!f.exists()) {
            LOGGER.error(String.format("Unable to find HPO file at %s", path));
            return;
        }
        this.hpo = OntologyLoader.loadOntology(new File(path));
    }

    public Ontology getHPO() {
        return this.hpo;
    }

}

package org.monarchinitiative.l2ci.core.io;

import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.utils.CurieUtil;
import org.monarchinitiative.phenol.io.utils.CurieUtilBuilder;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class HPOParser {
    private static Logger LOGGER = LoggerFactory.getLogger(HPOParser.class);

    private Ontology hpo = null;

    public HPOParser(String hpoPath) {
        LOGGER.trace(String.format("Initializing parser for %s", hpoPath));
        parse(hpoPath);
    }

    private void parse(String path) {
        File f = new File(path);
        System.out.println(path);
        System.out.println(f);
        if (!f.exists()) {
            LOGGER.error(String.format("Unable to find file at %s", path));
            return;
        }
        CurieUtil newCurie = CurieUtilBuilder.withDefaultsAnd(Map.of("HGNC", "http://identifiers.org/hgnc/"));
        this.hpo = OntologyLoader.loadOntology(new File(path), newCurie, "MONDO");//, "HGNC");
    }

    public Ontology getHPO() {
        return this.hpo;
    }

}

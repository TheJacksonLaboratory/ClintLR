package org.monarchinitiative.l2ci.core.mondo;

import org.junit.jupiter.api.BeforeAll;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;

/**
 * Class to test whether we can correctly extract mapped OMIM disease ids from the MONDO JSON file
 */
public class MondoOmimTest {

    @BeforeAll
    private static void init() {
        Ontology mondo = OntologyLoader.loadOntology(new File("fake"));
        TermId respiratoryMalformation = TermId.of("MONDO:0015930");
        Ontology smallOntologyForTesting = mondo.subOntology(respiratoryMalformation);
    }


}

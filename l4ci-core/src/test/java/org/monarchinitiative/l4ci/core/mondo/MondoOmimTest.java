package org.monarchinitiative.l4ci.core.mondo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Class to test whether we can correctly extract mapped OMIM disease ids from the MONDO JSON file
 */
public class MondoOmimTest {
    private static Ontology mondo;
    @BeforeAll
    public static void init() {
        ClassLoader classLoader = MondoOmimTest.class.getClassLoader();
        String mondoFilePath = classLoader.getResource("mondo_toy.json").getFile();
        mondo = OntologyLoader.loadOntology(new File(mondoFilePath));
    }

    @Test
    public void testOMIM() {
        Set<TermId> omimIDs = new HashSet<>();
        mondo.getTerms().forEach(t -> t.getXrefs().stream().filter(r -> r.getName().contains("OMIM:")).map(r -> t.id()).forEach(omimIDs::add));
        System.out.println("OMIM IDs = " + omimIDs);
        assertEquals(omimIDs.size(), 7, 1e0);
    }

    //function to get Mondo Id and return set of OMIM ids
    @Test
    public void testOmimSingle() {
        Set<String> omimIDs = new HashSet<>();
        Term mondoTerm = mondo.getTermMap().entrySet().stream().filter(e -> e.getKey().toString().equals("MONDO:0010174")).toList().get(0).getValue();
        mondoTerm.getXrefs().stream().filter(r -> r.getName().contains("OMIM:")).map(r -> r.getName()).forEach(omimIDs::add);
        System.out.println("OMIM IDs for Mondo Term " + mondoTerm.id() + " = " + omimIDs);
        assertEquals(omimIDs.size(), 1, 1e0);
    }


}

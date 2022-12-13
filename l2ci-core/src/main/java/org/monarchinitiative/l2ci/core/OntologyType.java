package org.monarchinitiative.l2ci.core;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

public enum OntologyType {
    HPO("hpo"),
    HPOA("hpoa"),
    MONDO("mondo");

    private final String name;

    OntologyType(String n) {
        this.name = n;
    }

    @Override
    public String toString() {
        return this.name;
    }

}

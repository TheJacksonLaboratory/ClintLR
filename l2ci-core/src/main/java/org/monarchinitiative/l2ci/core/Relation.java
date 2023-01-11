package org.monarchinitiative.l2ci.core;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

public enum Relation {
    ANCESTOR("ancestor"),
    CHILD("child"),
    DESCENDENT("descendent"),
    PARENT("parent");

    private final String name;

    Relation(String n) {
        this.name = n;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Get the relations of the {@code termId}.
     *
     * @param ontology An ontology
     * @param termId   A term ID of interest
     * @param relation Relation of interest (ancestor, descendent, child, parent)
     * @return stream of terms with the relation of interest (not including term itself).
     */
    public static Set<Term> getTermRelations(Ontology ontology, TermId termId, Relation relation) {
        return getTermRelationsStream(ontology, termId, relation)
                .collect(Collectors.toSet());
    }

    /**
     * Get the relations of the {@code termId}.
     *
     * @param ontology An ontology
     * @param termId   A term ID of interest
     * @param relation Relation of interest (ancestor, descendent, child, parent)
     * @return stream of terms with the relation of interest (not including term itself).
     */
    public static Stream<Term> getTermRelationsStream(Ontology ontology, TermId termId, Relation relation) {
        Set<TermId> relationIds = switch (relation) {
            case ANCESTOR -> getAncestorTerms(ontology, termId, false);
            case DESCENDENT -> getDescendents(ontology, termId);
            case CHILD -> getChildTerms(ontology, termId, false);
            case PARENT -> getParentTerms(ontology, termId, false);
        };

        return relationIds.stream()
                .map(id -> ontology.getTermMap().get(id));
    }
}

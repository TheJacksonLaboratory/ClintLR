package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import org.monarchinitiative.phenol.ontology.data.Ontology;

/**
 * @deprecated use {@link OptionalResources} and {@link OntologyResources} instead.
 */
@Deprecated(forRemoval = true)
public interface OptionalOntologyResource {

    ObjectProperty<Ontology> ontologyProperty();

    default Ontology getOntology() {
        return ontologyProperty().get();
    }

    default void setOntology(Ontology ontology) {
        ontologyProperty().set(ontology);
    }
}


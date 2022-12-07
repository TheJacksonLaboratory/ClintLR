package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.phenol.ontology.data.Ontology;

/**
 * @deprecated use {@link OptionalResources} and {@link OntologyResources} instead.
 */
@Deprecated(forRemoval = true)
public class OptionalMondoResource implements OptionalOntologyResource {

    private final ObjectProperty<Ontology> mondoOntology = new SimpleObjectProperty<>(this, "mondoOntology");

    @Override
    public ObjectProperty<Ontology> ontologyProperty() {
        return mondoOntology;
    }

}


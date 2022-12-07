package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.phenol.ontology.data.Ontology;

/**
 * @deprecated use {@link OptionalResources} and {@link OntologyResources} instead.
 */
@Deprecated(forRemoval = true)
public class OptionalHpoResource implements OptionalOntologyResource {

    private final ObjectProperty<Ontology> hpoOntology = new SimpleObjectProperty<>(this, "hpoOntology");

    @Override
    public ObjectProperty<Ontology> ontologyProperty() {
        return hpoOntology;
    }


}


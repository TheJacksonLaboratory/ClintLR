package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.phenol.ontology.data.Ontology;

public class OptionalMondoResource implements OptionalOntologyResource {

    private final BooleanBinding mondoResourceIsMissing;

    private final ObjectProperty<Ontology> mondoOntology = new SimpleObjectProperty<>(this, "mondoOntology", null);

    /**
     * Use this name to save MONDO.json file on the local filesystem.
     */
    public static final String DEFAULT_MONDO_FILE_NAME = "MONDO.json";

    public final static String MONDO_JSON_PATH_PROPERTY = "mondo.json.path";

    public OptionalMondoResource() {
        mondoResourceIsMissing = Bindings.createBooleanBinding(() -> ontologyProperty().get()==null);
    }


    @Override
    public BooleanBinding ontologyResourceMissing() {
        return null;
    }

    @Override
    public ObjectProperty<Ontology> ontologyProperty() {
        return mondoOntology;
    }

    @Override
    public Ontology getOntology() {
        return mondoOntology.get();
    }

    @Override
    public void setOntology(Ontology ontology) {
        mondoOntology.set(ontology);
    }


}


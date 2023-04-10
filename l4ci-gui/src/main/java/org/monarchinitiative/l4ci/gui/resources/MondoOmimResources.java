package org.monarchinitiative.l4ci.gui.resources;

import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

public class MondoOmimResources {
    // Note: the map below should not be modified, they should be set and unset
    // Mapping of OMIM term IDs to Mondo term IDs.
    private final ObjectProperty<Map<TermId, List<TermId>>> omimToMondo = new SimpleObjectProperty<>(Map.of());
    private final MapProperty<TermId, TermId> mondoToOmim = new SimpleMapProperty<>(FXCollections.observableHashMap());
    // Mapping of Mondo term IDs to descendent count.
    private final MapProperty<TermId, Integer> mondoNDescendents = new SimpleMapProperty<>(FXCollections.observableHashMap());

    public Map<TermId, List<TermId>> getOmimToMondo() {
        return omimToMondo.get();
    }

    public ObjectProperty<Map<TermId, List<TermId>>> omimToMondoProperty() {
        return omimToMondo;
    }

    public void setOmimToMondo(Map<TermId, List<TermId>> omimToMondo) {
        this.omimToMondo.set(omimToMondo);
    }

    public MapProperty<TermId, TermId> mondoToOmimProperty() {
        return mondoToOmim;
    }

    public MapProperty<TermId, Integer> mondoNDescendentsProperty() {
        return mondoNDescendents;
    }
}

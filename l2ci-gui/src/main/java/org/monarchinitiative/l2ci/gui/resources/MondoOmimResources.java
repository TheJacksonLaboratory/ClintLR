package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

public class MondoOmimResources {
    // Note: the maps below should not be modified, they should be set and unset
    // Mapping of OMIM term IDs to Mondo term IDs.
    private final ObjectProperty<Map<TermId, List<TermId>>> omimToMondo = new SimpleObjectProperty<>(Map.of());
    private final ObjectProperty<Map<TermId, TermId>> mondoToOmim = new SimpleObjectProperty<>(Map.of());
    // Mapping of Mondo term IDs to descendent count.
    private final ObjectProperty<Map<TermId, Integer>> mondoNDescendents = new SimpleObjectProperty<>(Map.of());

    public Map<TermId, List<TermId>> getOmimToMondo() {
        return omimToMondo.get();
    }

    public ObjectProperty<Map<TermId, List<TermId>>> omimToMondoProperty() {
        return omimToMondo;
    }

    public void setOmimToMondo(Map<TermId, List<TermId>> omimToMondo) {
        this.omimToMondo.set(omimToMondo);
    }

    public Map<TermId, TermId> getMondoToOmim() {
        return mondoToOmim.get();
    }

    public ObjectProperty<Map<TermId, TermId>> mondoToOmimProperty() {
        return mondoToOmim;
    }

    public void setMondoToOmim(Map<TermId, TermId> mondoToOmim) {
        this.mondoToOmim.set(mondoToOmim);
    }

    public Map<TermId, Integer> getMondoNDescendents() {
        return mondoNDescendents.get();
    }

    public ObjectProperty<Map<TermId, Integer>> mondoNDescendentsProperty() {
        return mondoNDescendents;
    }

    public void setMondoNDescendents(Map<TermId, Integer> mondoNDescendents) {
        this.mondoNDescendents.set(mondoNDescendents);
    }
}
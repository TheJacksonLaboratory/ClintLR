package org.monarchinitiative.clintlr.gui.ui.summary;

import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

public class L4ciDiseaseItem {

    private final TermId mondoId;
    private final String mondoLabel;
    private final Optional<TermId> omimId;

    public L4ciDiseaseItem(TermId mondoId, String mondoLabel, Optional<TermId> opt) {
        this.mondoId = mondoId;
        this.mondoLabel = mondoLabel;
        this.omimId = opt;
    }

    public L4ciDiseaseItem(Term mondoTerm, Optional<TermId> opt) {
        this.mondoId = mondoTerm.id();
        this.mondoLabel = mondoTerm.getName();
        this.omimId = opt;
    }


    public TermId getMondoId() {
        return mondoId;
    }

    public String getMondoLabel() {
        return mondoLabel;
    }

    public Optional<TermId> getOmimId() {
        return omimId;
    }

    public boolean hasOmimSynonym() {
        return this.omimId.isPresent();
    }
}

package org.monarchinitiative.l4ci.gui.ui.summary;

import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class L4ciDiseaseSummary {

    private final Term selectedMondoTerm;

    private final Set<L4ciDiseaseItem> descendentTermSet;

    public L4ciDiseaseSummary(Term selectedDiseaseTerm, Ontology mondo, double adjustment) {
        this.selectedMondoTerm = selectedDiseaseTerm;
        Set<TermId> descendentIdSet = OntologyAlgorithm.getDescendents(mondo, selectedDiseaseTerm.id());
        descendentTermSet = new HashSet<>();
        for (TermId tid : descendentIdSet) {
            Term t = mondo.getTermMap().get(tid);
            if (t != null) {
                Optional<TermId> omimIdOpt = Optional.empty();
                for (TermSynonym tsyn : t.getSynonyms()) {
                    if (TermSynonymScope.EXACT == tsyn.getScope()) {
                        for (TermXref txref : tsyn.getTermXrefs()) {
                            if (txref.id().getValue().startsWith("OMIM:")) {
                                omimIdOpt = Optional.of(txref.id());
                                // assumption with this code is that there is a maximum of one OMIM id EXACT synonym to any Mondo term
                                // we do not test this because nothing really bad will happen if this assumption is not true (it should be true)
                                // because we are using this merely to display the effect of choosing a parent term
                            }
                        }
                    }
                }
                descendentTermSet.add(new L4ciDiseaseItem(t, omimIdOpt));
            }
        }
    }


    public Set<L4ciDiseaseItem> diseaseItemsWithOmim() {
        return this.descendentTermSet.stream().
                filter(L4ciDiseaseItem::hasOmimSynonym).
                collect(Collectors.toSet());
    }

    public Set<L4ciDiseaseItem> diseaseItemsWithNoOmim() {
        return this.descendentTermSet.stream().
                filter(Predicate.not(L4ciDiseaseItem::hasOmimSynonym)).
                collect(Collectors.toSet());
    }

    public String targetMondoId() {
        return this.selectedMondoTerm.id().getValue();
    }

    public String targetMondoLabel() {
        return this.selectedMondoTerm.getName();
    }



}
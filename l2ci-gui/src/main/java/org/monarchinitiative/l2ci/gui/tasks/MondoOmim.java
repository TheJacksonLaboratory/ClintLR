package org.monarchinitiative.l2ci.gui.tasks;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

public record MondoOmim(
        Map<TermId, List<TermId>> omimToMondo,
        Map<TermId, TermId> mondoToOmim,
        Map<TermId, Integer> mondoNDescendents
) {
}

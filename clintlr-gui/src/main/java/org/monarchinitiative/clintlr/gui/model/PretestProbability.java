package org.monarchinitiative.clintlr.gui.model;

import org.monarchinitiative.clintlr.gui.resources.MondoOmimResources;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to adjust the pretest probability
 */
// TODO - make interface with getters for e.g. MondoOmimResources, and use that instead of the gui optional resources
// TODO - this ultimately belongs in core, not gui package
public class PretestProbability {

    public static Map<TermId, Double> of(Map<TermId, Double> mondoIdPretestAdjMap,
                                         MondoOmimResources mm,
                                         Collection<TermId> knownDiseaseIds,
                                         double defaultSliderValue) {

        Map<TermId, Double> pretestMap = new HashMap<>();

        Map<TermId, TermId> mondoToOmim = mm.mondoToOmimProperty();

        // Populate map with OMIM terms corresponding to the Mondo terms and pretest adjustment values
        for (TermId mondoId : mondoIdPretestAdjMap.keySet()) {
            TermId omimId = mondoToOmim.get(mondoId);
            if (omimId != null) {
                double pretestAdjValue = mondoIdPretestAdjMap.get(mondoId);
                pretestMap.put(omimId, pretestAdjValue + 1. + defaultSliderValue);
            }
        }


        // Add default slider values for remaining Mondo and HPOA terms to map
        for (TermId omimId : mm.getOmimToMondo().keySet())
            if (!pretestMap.containsKey(omimId))
                pretestMap.put(omimId, 1. + defaultSliderValue);


        if (knownDiseaseIds != null)
            for (TermId termId : knownDiseaseIds)
                if (!pretestMap.containsKey(termId) & termId.getPrefix().equals("OMIM"))
                    pretestMap.put(termId, 1. + defaultSliderValue);

        // Replace slider values in map with normalized pretest probabilities
        double mapSum = pretestMap.values().stream().reduce(0.0, Double::sum);
        pretestMap.forEach((key, value) -> pretestMap.replace(key, value / mapSum));

        return Map.copyOf(pretestMap);
    }

}

package org.monarchinitiative.l4ci.gui.model;

import org.monarchinitiative.l4ci.gui.resources.MondoOmimResources;
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
                                         double defaultSliderValue,
                                         boolean globalMode) {

        Map<TermId, Double> pretestMap = new HashMap<>();

        Map<TermId, TermId> mondoToOmim = mm.mondoToOmimProperty();

        // Populate map with OMIM terms corresponding to the Mondo terms and pretest adjustment values
        for (TermId mondoId : mondoIdPretestAdjMap.keySet()) {
            TermId omimId = mondoToOmim.get(mondoId);
            if (omimId != null) {
                double pretestAdjValue = mondoIdPretestAdjMap.get(mondoId);
                if (globalMode) {
                    pretestAdjValue = 0.0;
                }
                pretestMap.put(omimId, pretestAdjValue + defaultSliderValue);
            }
        }


        // Add default slider values for remaining Mondo and HPOA terms to map
        for (TermId omimId : mm.getOmimToMondo().keySet())
            if (!pretestMap.containsKey(omimId))
                pretestMap.put(omimId, defaultSliderValue);


        if (knownDiseaseIds != null)
            for (TermId termId : knownDiseaseIds)
                if (!pretestMap.containsKey(termId))
                    pretestMap.put(termId, defaultSliderValue);

        // Replace slider values in map with normalized pretest probabilities
        double mapSum = pretestMap.values().stream().reduce(0.0, Double::sum);
        System.out.println(mapSum);
        pretestMap.forEach((key, value) -> pretestMap.replace(key, value / mapSum));

        return Map.copyOf(pretestMap);
    }

}
package org.monarchinitiative.l2ci.core.pretestprob;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Set;

/**
 * Class to adjust the pretest probability
 */
public class PretestProbability {
    private final Map<TermId, Double> adjustedDiseaseToPretestMap;

    public PretestProbability(Map<TermId, Double> diseaseToPretestMap,
                              Set<TermId> diseaseToBeAdjusted,
                              Double summand) {

        // 1. get cur
        for (var e: diseaseToPretestMap.entrySet()) {
            TermId diseaseId = e.getKey();
            double p = e.getValue();
            diseaseToPretestMap.put(diseaseId, p+summand);
            // next line better than above?
            //diseaseToPretestMap.merge(diseaseId, summand, Double::sum);
        }
        // Normalize the map
        //double sum = diseaseToPretestMap.values().stream().
        //for each diseaseToPretestMap.merge(diseaseId, sum, Double::divide);
        adjustedDiseaseToPretestMap = Map.copyOf(diseaseToPretestMap);
    }
}

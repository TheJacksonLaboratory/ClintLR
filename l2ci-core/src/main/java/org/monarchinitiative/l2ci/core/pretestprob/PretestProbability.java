package org.monarchinitiative.l2ci.core.pretestprob;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Set;

/**
 * Class to adjust the pretest probability
 */
public class PretestProbability {

    public static Map<TermId, Double> of(Map<TermId, Double> diseaseToPretestMap,
                              Set<TermId> diseaseToBeAdjusted,
                              Double summand) {


        for (TermId diseaseId : diseaseToBeAdjusted) {
            diseaseToPretestMap.merge(diseaseId, summand, Double::sum);
        }
        // Normalize the map
        double mapSum = diseaseToPretestMap.values().stream().reduce(0.0, Double::sum);
        diseaseToPretestMap.forEach((key, value) -> diseaseToPretestMap.replace(key, value / mapSum));
        return Map.copyOf(diseaseToPretestMap);
    }

}

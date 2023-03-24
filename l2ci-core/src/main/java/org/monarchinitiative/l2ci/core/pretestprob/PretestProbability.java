package org.monarchinitiative.l2ci.core.pretestprob;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * Class to adjust the pretest probability
 */
public class PretestProbability {

//    public static Map<TermId, Double> of(Map<TermId, Double> diseaseToPretestMap,
//                              Set<TermId> diseaseToBeAdjusted,
//                              Double summand,
//                              List<MapData> mapDataList) {
//
//        for (TermId diseaseId : diseaseToBeAdjusted) {
//            diseaseToPretestMap.merge(diseaseId, summand, Double::sum);
//        }
//        // Normalize the map
//        double mapSum = diseaseToPretestMap.values().stream().reduce(0.0, Double::sum);
//        double fixedSum = mapDataList.stream().filter(MapData::isFixed).mapToDouble(MapData::getProbability).sum();
//        double otherSum = mapSum - fixedSum;
//        final int[] nFixedReplaced = {0};
//        long nFixed = mapDataList.stream().filter(MapData::isFixed).count();
//        diseaseToPretestMap.forEach((key, value) -> {
//            diseaseToPretestMap.replace(key, value * ((1 - fixedSum)/otherSum)); //diseaseToPretestMap.replace(key, value / mapSum);
//            if (nFixedReplaced[0] < nFixed) {
//                for (MapData mapData : mapDataList) {
//                    if (mapData.getOmimId().equals(key) & mapData.isFixed()) {
//                        diseaseToPretestMap.replace(key, mapData.getProbability());
//                        nFixedReplaced[0]++;
//                        break;
//                    }
//                }
//            }
//        });
////        System.out.println(diseaseToPretestMap.values().stream().reduce(0.0, Double::sum));
//        return Map.copyOf(diseaseToPretestMap);
//    }

    public static Map<TermId, Double> of(Map<TermId, Double> pretestAdjMap,
                                         Map<TermId, List<TermId>> omimToMondo,
                                         Collection<TermId> knownDiseaseIds,
                                         double defaultSliderValue) {

        Map<TermId, Double> pretestMap = new HashMap<>();

        // Populate map with OMIM terms and pretest adjustment values
        for (TermId omimId : pretestAdjMap.keySet()) {
            if (omimId != null) {
                double pretestAdjValue = pretestAdjMap.get(omimId);
                pretestMap.put(omimId, pretestAdjValue + defaultSliderValue);
            }
        }


        // Add default slider values for remaining Mondo and HPOA terms to map
        for (TermId omimId : omimToMondo.keySet())
            if (!pretestMap.containsKey(omimId))
                pretestMap.put(omimId, defaultSliderValue);


        if (knownDiseaseIds != null)
            for (TermId termId : knownDiseaseIds)
                if (!pretestMap.containsKey(termId))
                    pretestMap.put(termId, defaultSliderValue);

        // Replace slider values in map with normalized pretest probabilities
        double mapSum = pretestMap.values().stream().reduce(0.0, Double::sum);
        pretestMap.forEach((key, value) -> pretestMap.replace(key, value / mapSum));

        return Map.copyOf(pretestMap);
    }

}

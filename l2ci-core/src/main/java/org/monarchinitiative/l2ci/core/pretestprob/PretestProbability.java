package org.monarchinitiative.l2ci.core.pretestprob;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to adjust the pretest probability
 */
public class PretestProbability {

    public static Map<TermId, Double> of(Map<TermId, Double> diseaseToPretestMap,
                              Set<TermId> diseaseToBeAdjusted,
                              Double summand,
                              List<MapData> mapDataList) {

        for (TermId diseaseId : diseaseToBeAdjusted) {
            diseaseToPretestMap.merge(diseaseId, summand, Double::sum);
        }
        // Normalize the map
        double mapSum = diseaseToPretestMap.values().stream().reduce(0.0, Double::sum);
        double fixedSum = mapDataList.stream().filter(MapData::isFixed).mapToDouble(MapData::getProbability).sum();
        double otherSum = mapSum - fixedSum;
        final int[] nFixedReplaced = {0};
        long nFixed = mapDataList.stream().filter(MapData::isFixed).count();
        diseaseToPretestMap.forEach((key, value) -> {
            diseaseToPretestMap.replace(key, value * ((1 - fixedSum)/otherSum)); //diseaseToPretestMap.replace(key, value / mapSum);
            if (nFixedReplaced[0] < nFixed) {
                for (MapData mapData : mapDataList) {
                    if (mapData.getOmimId().equals(key) & mapData.isFixed()) {
                        diseaseToPretestMap.replace(key, mapData.getProbability());
                        nFixedReplaced[0]++;
                        break;
                    }
                }
            }
        });
//        System.out.println(diseaseToPretestMap.values().stream().reduce(0.0, Double::sum));
        return Map.copyOf(diseaseToPretestMap);
    }

}

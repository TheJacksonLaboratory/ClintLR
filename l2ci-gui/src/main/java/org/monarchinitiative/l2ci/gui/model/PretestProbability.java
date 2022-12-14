package org.monarchinitiative.l2ci.gui.model;

import org.monarchinitiative.l2ci.gui.resources.MondoOmimResources;
import org.monarchinitiative.l2ci.gui.ui.MondoTreeView;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to adjust the pretest probability
 */
public class PretestProbability {

    // TODO - we do not need the entire MondoTreeView, just the relevant Mondo IDs with slider values.
    public static Map<TermId, Double> of(MondoTreeView mondoTreeView,
                                         MondoOmimResources mm,
                                         Collection<TermId> knownDiseaseIds,
                                         double defaultSliderValue) {

        Map<TermId, Double> pretestMap = new HashMap<>();

        Map<TermId, TermId> mondoToOmim = mm.getMondoToOmim();

        // Get updated slider values from TreeView
        mondoTreeView.drainSliderValues()
                .filter(md -> md.getSliderValue() >= defaultSliderValue)
                /*
                  Here we update OMIM -> pretest proba map.
                  However, the `mondoTreeView` provides, well, Mondo IDs. Hence, we first map
                  to the corresponding OMIM (if any) and set the pretest probability found on a tree node.
                 */
                .forEach(d -> {
                    TermId omimId = mondoToOmim.get(d.id());
                    if (omimId != null) {
                        pretestMap.compute(omimId,
                                (OMIM_ID, defaultProba) -> d.getSliderValue()
                        );
                    }
                });

        for (TermId diseaseId : pretestMap.keySet())
            pretestMap.merge(diseaseId, defaultSliderValue, Double::sum);

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
        pretestMap.forEach((key, value) -> pretestMap.replace(key, value / mapSum));

        return Map.copyOf(pretestMap);
    }

}

package org.monarchinitiative.l2ci.gui.model;

import org.monarchinitiative.l2ci.core.pretestprob.MapData;
import org.monarchinitiative.l2ci.gui.resources.MondoOmimResources;
import org.monarchinitiative.l2ci.gui.resources.OptionalResources;
import org.monarchinitiative.l2ci.gui.resources.OptionalServices;
import org.monarchinitiative.l2ci.gui.ui.MondoTreeView;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to adjust the pretest probability
 */
public class PretestProbability {

    public static Map<TermId, Double> of(MondoTreeView mondoTreeView, OptionalServices optionalServices, OptionalResources optionalResources, double defaultSliderValue) {

        Map<TermId, Double> pretestMap = new HashMap<>();

        MondoOmimResources mm = optionalServices.mondoOmimResources();
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


        Map<TermId, HpoDisease> hpoaDiseaseMap = optionalResources.ontologyResources().getId2diseaseModelMap();
        if (hpoaDiseaseMap != null)
            for (TermId termId : hpoaDiseaseMap.keySet())
                if (!pretestMap.containsKey(termId))
                    pretestMap.put(termId, defaultSliderValue);

        // Replace slider values in map with normalized pretest probabilities
        double mapSum = pretestMap.values().stream().reduce(0.0, Double::sum);
        pretestMap.forEach((key, value) -> pretestMap.replace(key, value / mapSum));

        return Map.copyOf(pretestMap);
    }

}

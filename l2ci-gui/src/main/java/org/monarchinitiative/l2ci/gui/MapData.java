package org.monarchinitiative.l2ci.gui;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

public class MapData {

    String diseaseName;
    TermId termId;
    Double diseaseProb;

    public MapData(String name, TermId id, Double probability) {
        this.diseaseName = name;
        this.termId = id;
        this.diseaseProb = probability;
    }

    public String getName() {
        return diseaseName;
    }

    public TermId getTermId() {
        return termId;
    }

    public Double getProbability() {
        return diseaseProb;
    }
}

package org.monarchinitiative.l2ci.gui;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

public class MapData {

    String diseaseName;
    TermId termId;
    Double diseaseProb;
    Double sliderValue;

    public MapData(String name, TermId id, Double probability, Double sliderVal) {
        this.diseaseName = name;
        this.termId = id;
        this.diseaseProb = probability;
        this.sliderValue = sliderVal;
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

    public Double getSliderValue() {
        return sliderValue;
    }
}

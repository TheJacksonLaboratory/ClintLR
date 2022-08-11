package org.monarchinitiative.l2ci.core.pretestprob;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class MapData {

    String diseaseName;
    TermId mondoId;
    TermId omimId;
    Double diseaseProb;
    Double sliderValue;

    boolean isFixed;

    public MapData(String name, TermId mondoId, TermId omimId, Double probability, Double sliderVal, boolean fixed) {
        this.diseaseName = name;
        this.mondoId = mondoId;
        this.omimId = omimId;
        this.diseaseProb = probability;
        this.sliderValue = sliderVal;
        this.isFixed = fixed;
    }

    public String getName() {
        return diseaseName;
    }

    public TermId getMondoId() {
        return mondoId;
    }

    public TermId getOmimId() {
        return omimId;
    }

    public Double getProbability() {
        return diseaseProb;
    }

    public Double getSliderValue() {
        return sliderValue;
    }

    public boolean isFixed() {
        return isFixed;
    }

    public void setFixed(boolean fixed) { this.isFixed = fixed;}
}

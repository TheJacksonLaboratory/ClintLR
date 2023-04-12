package org.monarchinitiative.l4ci.core.pretestprob;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class MapData {

    private final String diseaseName;
    private final TermId mondoId;
    private final TermId omimId;
    private final Double diseaseProb;
    private final Double sliderValue;
    private boolean isFixed;

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

    @Override
    public String toString() {
        return String.format("%s %s %s %.2e %.2f %s", diseaseName,mondoId,omimId,diseaseProb,sliderValue,isFixed);
    }
}

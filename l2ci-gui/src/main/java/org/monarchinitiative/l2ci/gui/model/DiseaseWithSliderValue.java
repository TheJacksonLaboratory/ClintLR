package org.monarchinitiative.l2ci.gui.model;

import javafx.beans.property.DoubleProperty;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.TermId;

public interface DiseaseWithSliderValue extends Identified {

    static DiseaseWithSliderValue of(TermId id, String name, Double sliderValue) {
        return new DiseaseWithSliderValueDefault(id, name, sliderValue);
    }

    /**
     * @return disease name
     */
    String getName();

    DoubleProperty sliderValueProperty();

    /**
     * Get the pretest probability for a disease. Note, the probability can be {@code null}.
     */
    default Double getSliderValue() {
        return sliderValueProperty().getValue();
    }

}

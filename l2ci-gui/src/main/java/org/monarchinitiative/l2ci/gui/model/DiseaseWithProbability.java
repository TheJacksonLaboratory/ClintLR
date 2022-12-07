package org.monarchinitiative.l2ci.gui.model;

import javafx.beans.property.DoubleProperty;
import org.monarchinitiative.phenol.ontology.data.Identified;

public interface DiseaseWithProbability extends Identified {

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

package org.monarchinitiative.l2ci.gui.model;

import javafx.beans.property.DoubleProperty;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * A container for a disease with pretest probability multiplier.
 */
public interface DiseaseWithMultiplier extends Identified {

    static DiseaseWithMultiplier of(TermId id, String name, Double sliderValue) {
        return new DiseaseWithMultiplierDefault(id, name, sliderValue);
    }

    /**
     * @return disease name
     */
    String getName();

    DoubleProperty multiplierProperty();

}

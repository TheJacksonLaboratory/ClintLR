package org.monarchinitiative.l2ci.gui.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

class DiseaseWithSliderValueDefault implements DiseaseWithSliderValue {

    private final TermId id;
    private final String name;
    private final DoubleProperty sliderValue = new SimpleDoubleProperty();

    DiseaseWithSliderValueDefault(TermId id, String name, Double sliderValue) {
        this.id = id;
        this.name = name;
        this.sliderValue.setValue(sliderValue);
    }

    @Override
    public TermId id() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DoubleProperty sliderValueProperty() {
        return sliderValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiseaseWithSliderValueDefault that = (DiseaseWithSliderValueDefault) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(sliderValue, that.sliderValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, sliderValue);
    }

    @Override
    public String toString() {
        return "DiseaseWithSliderValueDefault{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sliderValue=" + sliderValue +
                '}';
    }
}

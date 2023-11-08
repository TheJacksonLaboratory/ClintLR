package org.monarchinitiative.clintlr.gui.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

class DiseaseWithMultiplierDefault implements DiseaseWithMultiplier {

    private final TermId id;
    private final String name;
    private final DoubleProperty multiplier = new SimpleDoubleProperty();

    DiseaseWithMultiplierDefault(TermId id, String name, Double multiplier) {
        this.id = id;
        this.name = name;
        this.multiplier.setValue(multiplier);
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
    public DoubleProperty multiplierProperty() {
        return multiplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiseaseWithMultiplierDefault that = (DiseaseWithMultiplierDefault) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(multiplier, that.multiplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, multiplier);
    }

    @Override
    public String toString() {
        return "DiseaseWithMultiplierDefault{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", multiplier=" + multiplier +
                '}';
    }
}

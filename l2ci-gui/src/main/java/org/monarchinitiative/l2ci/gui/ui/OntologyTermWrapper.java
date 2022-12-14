package org.monarchinitiative.l2ci.gui.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.monarchinitiative.l2ci.gui.model.DiseaseWithSliderValue;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * The wrapper class for keeping a {@link Term} that represents a Mondo disease together with the pretest probability.
 * <p>
 * The pretest probability can be {@code null} or a value in the range of {@code [0, 1]}.
 */
public class OntologyTermWrapper implements DiseaseWithSliderValue {

    private final Term term;
    private final DoubleProperty sliderValue = new SimpleDoubleProperty();

    private final boolean hasOmimXref;

    private final boolean hasOmimPSXref;

    public static OntologyTermWrapper createOmimXref(Term term, double initialAdjustmentValue) {
        boolean hasOmimXref = term.getXrefs().stream().anyMatch(r -> r.getName().startsWith("OMIM:"));
        boolean hasOmimPSXref = term.getXrefs().stream().anyMatch(r -> r.getName().startsWith("OMIMPS:"));
        return new OntologyTermWrapper(term, initialAdjustmentValue, hasOmimXref, hasOmimPSXref);
    }

    public OntologyTermWrapper(Term term, Double initialAdjustmentValue, boolean hasOmimXref, boolean hasOmimPSXref) {
        this.term = term;
        this.sliderValue.setValue(initialAdjustmentValue);
        this.hasOmimXref = hasOmimXref;
        this.hasOmimPSXref = hasOmimPSXref;
    }

    public Term term() {
        return term;
    }

    @Override
    public TermId id() {
        return term.id();
    }

    @Override
    public String getName() {
        return term.getName();
    }

    /**
     * Get slider value property for this disease.
     */
    public DoubleProperty sliderValueProperty() {
        return sliderValue;
    }

    @Override
    public Double getSliderValue() {
        return sliderValue.get();
    }

    public void setSliderValue(Number sliderValue) {
        this.sliderValue.setValue(sliderValue);
    }

    public boolean hasOmimXref() {return hasOmimXref;}

    public boolean hasOmimPSXref() {
        return hasOmimPSXref;
    }
}
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

    private static final double DEFAULT_PRETEST_PROBA = 1.;

    private final Term term;
    private final DoubleProperty sliderValue = new SimpleDoubleProperty();

    public OntologyTermWrapper(Term term) {
        this(term, DEFAULT_PRETEST_PROBA);
    }

    public OntologyTermWrapper(Term term, Double sliderValue) {
        this.term = term;
        this.sliderValue.setValue(sliderValue);
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

}
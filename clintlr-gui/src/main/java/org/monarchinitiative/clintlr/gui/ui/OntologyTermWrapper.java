package org.monarchinitiative.clintlr.gui.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.monarchinitiative.clintlr.gui.model.DiseaseWithMultiplier;
import org.monarchinitiative.clintlr.gui.ui.summary.DiseaseSummary;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * The wrapper class for keeping a {@link Term} that represents a Mondo disease together
 * with the pretest probability multiplier.
 * <p>
 * The pretest probability can be {@code null} or a value in the range of {@code [0, 1]}.
 */
public class OntologyTermWrapper implements DiseaseWithMultiplier, DiseaseSummary {

    private final Term term;
    private final DoubleProperty multiplier = new SimpleDoubleProperty();
    private final boolean hasOmimXref;
    private final boolean hasOmimPSXref;

    public static OntologyTermWrapper createOmimXref(Term term, Double multiplier) {
        boolean hasOmimXref = term.getXrefs().stream()
                .anyMatch(r -> r.getName().startsWith("OMIM:"));
        boolean hasOmimPSXref = term.getXrefs().stream()
                .anyMatch(r -> r.getName().startsWith("OMIMPS:"));
        return new OntologyTermWrapper(term, multiplier, hasOmimXref, hasOmimPSXref);
    }

    private OntologyTermWrapper(Term term, Double multiplier, boolean hasOmimXref, boolean hasOmimPSXref) {
        this.term = term;
        this.multiplier.setValue(multiplier);
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
     * Get multiplier value property for this disease.
     */
    public DoubleProperty multiplierProperty() {
        return multiplier;
    }

    @Override
    public Term getTerm() {
        return term;
    }

    public Double getMultiplier() {
        return multiplier.get();
    }

    public void setMultiplier(Number multiplier) {
        this.multiplier.setValue(multiplier);
    }

    public boolean hasOmimXref() {return hasOmimXref;}

    public boolean hasOmimPSXref() {
        return hasOmimPSXref;
    }
}
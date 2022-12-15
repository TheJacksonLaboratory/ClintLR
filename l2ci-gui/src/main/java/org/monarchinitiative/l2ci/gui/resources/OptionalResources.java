package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class OptionalResources {
    // The class is annotated by @Component - Spring will create a singleton instance during App's startup.

    private final OntologyResources ontologyResources;
    private final LiricalResources liricalResources;
    private final ObjectProperty<Path> liricalResults = new SimpleObjectProperty<>(this, "liricalResults");

    public OptionalResources() {
        ontologyResources = new OntologyResources();
        liricalResources = new LiricalResources();
    }

    public OntologyResources ontologyResources() {
        return ontologyResources;
    }

    public LiricalResources liricalResources() {
        return liricalResources;
    }

    public Path getLiricalResults() {
        return liricalResults.get();
    }

    public ObjectProperty<Path> liricalResultsProperty() {
        return liricalResults;
    }

    public void setLiricalResults(Path liricalResults) {
        this.liricalResults.set(liricalResults);
    }
}

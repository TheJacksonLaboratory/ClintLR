package org.monarchinitiative.clintlr.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;

/**
 * {@linkplain OntologyResources} include paths to ontologies and related files. The path is {@code null}
 * if the path has not been set yet.
 */
public class OntologyResources {

    static final String MONDO_JSON_PATH_PROPERTY = "mondo.json.path";

    private final ObjectProperty<Path> mondoPath = new SimpleObjectProperty<>(this, "mondo");

    OntologyResources() {
    }

    public Path getMondoPath() {
        return mondoPath.get();
    }

    public ObjectProperty<Path> mondoPathProperty() {
        return mondoPath;
    }

    public void setMondoPath(Path mondoPath) {
        this.mondoPath.set(mondoPath);
    }

}

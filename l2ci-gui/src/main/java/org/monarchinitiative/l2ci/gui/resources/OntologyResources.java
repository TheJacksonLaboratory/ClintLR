package org.monarchinitiative.l2ci.gui.resources;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;

/**
 * Ontology resources include paths to ontologies and related files. The path is {@code null}
 * if the path has not been set yet.
 */
public class OntologyResources {

    public final static String MONDO_JSON_PATH_PROPERTY = "mondo.json.path";

    private final ObjectProperty<Path> hpoPath = new SimpleObjectProperty<>(this, "hpoPath");

    private final ObjectProperty<Path> mondoPath = new SimpleObjectProperty<>(this, "mondo");

    private final ObjectProperty<Path> hpoaPath = new SimpleObjectProperty<>(this, "hpoaPath");

    OntologyResources() {
    }

    public Path getHpoPath() {
        return hpoPath.get();
    }

    public ObjectProperty<Path> hpoPathProperty() {
        return hpoPath;
    }

    public void setHpoPath(Path hpoPath) {
        this.hpoPath.set(hpoPath);
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

    public Path getHpoaPath() {
        return hpoaPath.get();
    }

    public ObjectProperty<Path> hpoaPathProperty() {
        return hpoaPath;
    }

    public void setHpoaPath(Path hpoaPath) {
        this.hpoaPath.set(hpoaPath);
    }
}

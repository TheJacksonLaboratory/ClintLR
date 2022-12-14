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

//    public final static String HPO_JSON_PATH_PROPERTY = "hpo.json.path";

//    public final static String HPOA_PATH_PROPERTY = "hpoa.path";

//    private final ObjectProperty<Path> hpoPath = new SimpleObjectProperty<>(this, "hpoPath");

    private final ObjectProperty<Path> mondoPath = new SimpleObjectProperty<>(this, "mondo");

//    private final ObjectProperty<Path> hpoaPath = new SimpleObjectProperty<>(this, "hpoaPath");

    //HPOA resources
//    private final ObjectProperty<Map<TermId, List<HpoDisease>>> indirectAnnotMap =
//            new SimpleObjectProperty<>(this, "indirectAnnotMap", null);
//
//    private final ObjectProperty<Map<TermId, List<HpoDisease>>> directAnnotMap =
//            new SimpleObjectProperty<>(this, "directAnnotMap", null);

//    private Map<String, TermId> name2diseaseIdMap;
//    private Map<TermId, HpoDisease> id2diseaseModelMap;

    OntologyResources() {
    }

//    public Path getHpoPath() {
//        return hpoPath.get();
//    }
//
//    public ObjectProperty<Path> hpoPathProperty() {
//        return hpoPath;
//    }
//
//    public void setHpoPath(Path hpoPath) {
//        this.hpoPath.set(hpoPath);
//    }

    public Path getMondoPath() {
        return mondoPath.get();
    }

    public ObjectProperty<Path> mondoPathProperty() {
        return mondoPath;
    }

    public void setMondoPath(Path mondoPath) {
        this.mondoPath.set(mondoPath);
    }

//    public Path getHpoaPath() {
//        return hpoaPath.get();
//    }

//    public ObjectProperty<Path> hpoaPathProperty() {
//        return hpoaPath;
//    }

//    public void setHpoaPath(Path hpoaPath) {
//        this.hpoaPath.set(hpoaPath);
//    }

//    public ObjectProperty<Map<TermId, List<HpoDisease>>> directAnnotMapProperty() {
//        return directAnnotMap;
//    }

//    public ObjectProperty<Map<TermId, List<HpoDisease>>> indirectAnnotMapProperty() {
//        return indirectAnnotMap;
//    }

    // HPOA resources
//    public void setAnnotationResources(String phenotypeDotHpoaPath, Ontology hpo, Logger LOGGER) {
//        DirectIndirectHpoAnnotationParser parser =
//                new DirectIndirectHpoAnnotationParser(phenotypeDotHpoaPath, hpo);
//        Map<TermId, List<HpoDisease>> directMap = parser.getDirectAnnotMap();
//        LOGGER.info("Setting direct annotation map with size {}", directMap.size());
//        this.directAnnotMap.set(directMap);
//        this.indirectAnnotMap.set(parser.getTotalAnnotationMap());
//        if (directAnnotMap.get() != null) {
//            Set<HpoDisease> diseaseSet = directAnnotMap.get().values().stream()
//                    .flatMap(Collection::stream)
//                    .collect(Collectors.toSet());
//            LOGGER.info("Found {} diseases (diseaseSet)", diseaseSet.size());
//            // for some reason the stream implementation is choking
//            name2diseaseIdMap = new HashMap<>();
//            for (HpoDisease disease : diseaseSet) {
//                name2diseaseIdMap.put(disease.diseaseName(), disease.id());
//            }
//            LOGGER.info("name2diseaseIdMap initialized with {} entries", name2diseaseIdMap.size());
//            id2diseaseModelMap = diseaseSet.stream()
//                    .collect(Collectors.toMap(HpoDisease::id, Function.identity()));
//            LOGGER.info("id2diseaseModelMap initialized with {} entries", id2diseaseModelMap.size());
//        } else {
//            // should never happen
//            LOGGER.error("getDirectAnnotMap() was null after initialization");
//        }
//    }
//
//    public Map<TermId, List<HpoDisease>> getDirectAnnotMap() {
//        return directAnnotMap.get();
//    }
//    public Map<TermId, List<HpoDisease>> getIndirectAnnotMap() {
//        return indirectAnnotMap.get();
//    }
//
//    public Map<String, TermId> getName2diseaseIdMap() {
//        return name2diseaseIdMap;
//    }
//
//    public Map<TermId, HpoDisease> getId2diseaseModelMap() {
//        return id2diseaseModelMap;
//    }

//    /**
//     * If we cannot initialize these resources, create empty maps to avoid null pointer errors.
//     */
//    public void initializeWithEmptyMaps() {
//        directAnnotMap.set(Map.of());
//        indirectAnnotMap.set(Map.of());
//        name2diseaseIdMap = Map.of();
//        id2diseaseModelMap = Map.of();
//    }
}

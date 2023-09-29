package org.monarchinitiative.clintlr.core;

import org.monarchinitiative.clintlr.core.io.DirectIndirectHpoAnnotationParser;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OptionalHpoaResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionalHpoaResource.class);
    private final boolean hpoaResourceIsMissing;

    /**
     * Use this name to save PHENOTYPE.hpoa file on the local filesystem.
     */
    public static final String DEFAULT_HPOA_FILE_NAME = "PHENOTYPE.hpoa";
    public final static String HPOA_PATH_PROPERTY = "hpoa/path";
    private Map<TermId, List<HpoDisease>> indirectAnnotMap;

    private Map<TermId, List<HpoDisease>> directAnnotMap;

    private Map<String, TermId> name2diseaseIdMap;
    private Map<TermId, HpoDisease> id2diseaseModelMap;


    public OptionalHpoaResource(){
        hpoaResourceIsMissing = Stream.of( indirectAnnotMapProperty(), directAnnotMapProperty()).anyMatch(op -> op == null);
    }

    public Map<TermId, List<HpoDisease>> directAnnotMapProperty() {
        return directAnnotMap;
    }

    public Map<TermId, List<HpoDisease>> indirectAnnotMapProperty() {
        return indirectAnnotMap;
    }

    public void setAnnotationResources(String phenotypeDotHpoaPath, Ontology hpo){
        DirectIndirectHpoAnnotationParser parser =
                new DirectIndirectHpoAnnotationParser(phenotypeDotHpoaPath, hpo);
        Map<TermId, List<HpoDisease>> directMap = parser.getDirectAnnotMap();
        LOGGER.info("Setting direct annotation map with size {}", directMap.size());
        this.directAnnotMap = directMap;
        this.indirectAnnotMap = parser.getTotalAnnotationMap();
        if (getDirectAnnotMap() != null) {
            Set<HpoDisease> diseaseSet = directAnnotMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            LOGGER.info("Found {} diseases (diseaseSet)", diseaseSet.size());
            // for some reason the stream implementation is choking
            name2diseaseIdMap = new HashMap<>();
            for (HpoDisease disease : diseaseSet) {
                name2diseaseIdMap.put(disease.diseaseName(), disease.id());
            }
            LOGGER.info("name2diseaseIdMap initialized with {} entries", name2diseaseIdMap.size());
            id2diseaseModelMap = diseaseSet.stream()
                    .collect(Collectors.toMap(HpoDisease::id, Function.identity()));
            LOGGER.info("id2diseaseModelMap initialized with {} entries", id2diseaseModelMap.size());
        } else {
            // should never happen
            LOGGER.error("getDirectAnnotMap() was null after initialization");
        }
    }

    public Map<TermId, List<HpoDisease>> getDirectAnnotMap() {
        return directAnnotMap;
    }
    public Map<TermId, List<HpoDisease>> getIndirectAnnotMap() {
        return indirectAnnotMap;
    }

    public Map<String, TermId> getName2diseaseIdMap() {
        return name2diseaseIdMap;
    }

    public Map<TermId, HpoDisease> getId2diseaseModelMap() {
        return id2diseaseModelMap;
    }

    /**
     * If we cannot initialize these resources, create empty maps to avoid null pointer errors.
     */
    public void initializeWithEmptyMaps() {
        directAnnotMap = Map.of();
        indirectAnnotMap = Map.of();
        name2diseaseIdMap = Map.of();
        id2diseaseModelMap = Map.of();
    }


    @Override
    public String toString() {
        return String.format("OptionalHpoaResource\n\tdirectAnnotMap: n=%d\n\tindirectAnnotMap: n=%d\n\tname2diseaseIdMap: n=%d\n\tid2diseaseModelMap: n=%d\n",
                directAnnotMap == null ? 0 : directAnnotMap.size(),
                indirectAnnotMap == null ? 0 : indirectAnnotMap.size(),
                name2diseaseIdMap == null ? 0 : name2diseaseIdMap.size(),
                id2diseaseModelMap == null ? 0 :  id2diseaseModelMap.size()
        );
    }
}

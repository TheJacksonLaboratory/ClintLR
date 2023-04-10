package org.monarchinitiative.l4ci.core.mondo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.l4ci.core.io.HPOParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MondoStatsTest {

    private static MondoStats mondo = null;

    @BeforeAll
    public static void setup() {
        ClassLoader classLoader = MondoStatsTest.class.getClassLoader();
        String mondoFilePath=classLoader.getResource("mondo_toy.json").getFile();
        HPOParser parser = new HPOParser(mondoFilePath);
        assertNotNull(parser);
        Ontology ont = parser.getHPO();
        assertNotNull(ont);
        mondo = new MondoStats(ont);
    }

    @Test
    public void testMetaInfo() {
        Map<String, String> metaInfo = mondo.getMetaInfo();
        assertNotNull(metaInfo.keySet());
        assertNotNull(metaInfo.entrySet());
        assertEquals(metaInfo.get("data-version"), "http://purl.obolibrary.org/obo/mondo/releases/2022-04-04/mondo.owl");
    }

    @Test
    public void testNTerms() {
        int nTerms = mondo.getNTerms();
        assertEquals(nTerms, 11, 1e0);
    }

    @Test
    public void testNAlternateTermIDs() {
        int nAlternateTermIDs = mondo.getNAlternateTermIDs();
        assertEquals(nAlternateTermIDs, 0, 1e0);
    }

    @Test
    public void testNNonObsoleteTerms() {
        int nNonObsoleteTerms = mondo.getNNonObsoleteTerms();
        assertEquals(nNonObsoleteTerms, 11, 1e0);
    }

    @Test
    public void testNRelations() {
        int nRelations = mondo.getNRelations();
        assertEquals(nRelations, 13, 1e0);
    }

    @Test
    public void testNDefinitions() {
        int nDefinitions = mondo.getNDefinitions();
        assertEquals(nDefinitions, 3, 1e0);
    }

    @Test
    public void testNSynonyms() {
        int nSynonyms = mondo.getNSynonyms();
        assertEquals(nSynonyms, 18, 1e0);
    }
}

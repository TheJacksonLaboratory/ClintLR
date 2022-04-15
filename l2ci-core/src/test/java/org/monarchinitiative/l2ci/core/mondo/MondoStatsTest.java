package org.monarchinitiative.l2ci.core.mondo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.core.io.HPOParserTest;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MondoStatsTest {

    private static MondoStats mondo = null;

    @BeforeAll
    public static void setup() {
        ClassLoader classLoader = HPOParserTest.class.getClassLoader();
        String hpoFilePath=classLoader.getResource("hp.json").getFile();
        HPOParser parser = new HPOParser(hpoFilePath);
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
        assertEquals(metaInfo.get("data-version"), "http://purl.obolibrary.org/obo/hp/releases/2022-02-14/hp.json");
    }

    @Test
    public void testNTerms() {
        int nTerms = mondo.getNTerms();
        assertEquals(nTerms, 16480, 1e0);
    }

    @Test
    public void testNAlternateTermIDs() {
        int nAlternateTermIDs = mondo.getNAlternateTermIDs();
        assertEquals(nAlternateTermIDs, 321, 1e0);
    }

    @Test
    public void testNNonObsoleteTerms() {
        int nNonObsoleteTerms = mondo.getNNonObsoleteTerms();
        assertEquals(nNonObsoleteTerms, 16480, 1e0);
    }

    @Test
    public void testNRelations() {
        int nRelations = mondo.getNRelations();
        assertEquals(nRelations, 20724, 1e0);
    }

    @Test
    public void testNDefinitions() {
        int nDefinitions = mondo.getNDefinitions();
        assertEquals(nDefinitions, 17281, 1e0);
    }

    @Test
    public void testNSynonyms() {
        int nSynonyms = mondo.getNSynonyms();
        assertEquals(nSynonyms, 37521, 1e0);
    }
}

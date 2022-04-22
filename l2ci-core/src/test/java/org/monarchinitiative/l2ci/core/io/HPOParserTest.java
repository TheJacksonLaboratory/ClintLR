package org.monarchinitiative.l2ci.core.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HPOParserTest {

    private static String hpoFilePath=null;

    @BeforeEach
    public void setup() {
        ClassLoader classLoader = HPOParserTest.class.getClassLoader();
        hpoFilePath = classLoader.getResource("mondo.json").getFile();
    }

    @Test
    public void testInputOntology() {
        HPOParser parser = new HPOParser(hpoFilePath);
        assertNotNull(parser);
    }

}

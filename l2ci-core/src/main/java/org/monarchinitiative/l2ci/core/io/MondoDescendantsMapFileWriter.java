package org.monarchinitiative.l2ci.core.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class MondoDescendantsMapFileWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MondoDescendantsMapFileWriter.class);

    public MondoDescendantsMapFileWriter(Map<TermId, Integer> mondoNDescMap, String filePath) {
        LOGGER.trace(String.format("Writing map file %s", filePath));
        write(mondoNDescMap, filePath);
    }

    private void write(Map<TermId, Integer> map, String path) {
        File f = new File(path);
        try (FileWriter writer = new FileWriter(path);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("Term ID", "No. Descendants");
            Set<Map.Entry<TermId, Integer>> entries = map.entrySet();
            for (Map.Entry e : entries) {
                printer.print(e.getKey());
                printer.print(e.getValue());
                printer.println();
            }
            writer.flush();
        } catch (IOException ioe) {
            LOGGER.trace(ioe.getMessage());
        }
    }
}

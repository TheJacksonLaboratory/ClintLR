package org.monarchinitiative.l4ci.core.io;

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

    public MondoDescendantsMapFileWriter(Map<TermId, Map<TermId, TermId>> mondoDescMap, String filePath) {
        LOGGER.trace(String.format("Writing map file %s", filePath));
        write(mondoDescMap, filePath);
    }

    private void write(Map<TermId, Map<TermId, TermId>> map, String path) {
        File f = new File(path);
        try (FileWriter writer = new FileWriter(path);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("Term ID", "Descendants");
            Set<Map.Entry<TermId, Map<TermId, TermId>>> entries = map.entrySet();
            for (Map.Entry e : entries) {
                printer.print(e.getKey());
                Map<TermId, TermId> descMap = (Map<TermId, TermId>) e.getValue();
                for (TermId omimId : descMap.keySet()) {
                    printer.print(omimId + ": " + descMap.get(omimId));
                }
                printer.println();
            }
            writer.flush();
        } catch (IOException ioe) {
            LOGGER.trace(ioe.getMessage());
        }
    }
}

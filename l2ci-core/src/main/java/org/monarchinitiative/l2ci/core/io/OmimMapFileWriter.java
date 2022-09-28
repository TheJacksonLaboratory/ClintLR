package org.monarchinitiative.l2ci.core.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OmimMapFileWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmimMapFileWriter.class);

    public OmimMapFileWriter(Map<TermId, List<TermId>> omim2mondoMap, String filePath) {
        LOGGER.trace(String.format("Writing Omim map file %s", filePath));
        write(omim2mondoMap, filePath);
    }

    private void write(Map<TermId, List<TermId>> map, String path) {
        File f = new File(path);
        try (FileWriter writer = new FileWriter(path);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("Omim Term ID", "Mondo Terms");
            Set<Map.Entry<TermId, List<TermId>>> entries = map.entrySet();
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

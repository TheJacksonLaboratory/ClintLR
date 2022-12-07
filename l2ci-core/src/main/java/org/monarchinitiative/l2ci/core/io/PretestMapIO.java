package org.monarchinitiative.l2ci.core.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class PretestMapIO {
    private static final Logger LOGGER = LoggerFactory.getLogger(PretestMapIO.class);
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT;

    private PretestMapIO() {
    }

    public static void write(Map<TermId, Double> pretestMap, Path destination) throws IOException {
        try (OutputStream os = Files.newOutputStream(destination)) {
            write(pretestMap, os);
        }
    }

    public static void write(Map<TermId, Double> pretestMap, OutputStream os) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
             CSVPrinter printer = CSV_FORMAT.print(writer)) {
            printer.printRecord("OmimID", "PretestProbability");
            for (Map.Entry<TermId, Double> e : pretestMap.entrySet()) {
                printer.print(e.getKey().getValue());
                printer.print(e.getValue());
                printer.println();
            }
        }
    }
}

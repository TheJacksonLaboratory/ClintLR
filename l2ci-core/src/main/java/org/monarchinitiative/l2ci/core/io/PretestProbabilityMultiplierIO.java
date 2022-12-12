package org.monarchinitiative.l2ci.core.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class PretestProbabilityMultiplierIO {
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setCommentMarker('#')
            .build();

    private PretestProbabilityMultiplierIO() {
    }

    public static void write(Map<TermId, Double> multipliersMap, Path destination) throws IOException {
        try (OutputStream os = Files.newOutputStream(destination)) {
            write(multipliersMap, os);
        }
    }

    public static void write(Map<TermId, Double> multipliersMap, OutputStream os) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
             CSVPrinter printer = CSV_FORMAT.print(writer)) {

            // Header
            printer.printComment("Pretest probability multipliers");
            printer.printComment("date=%s".formatted(LocalDateTime.now()));
            printer.printRecord("MondoID", "PretestProbabilityMultiplier");

            // Values
            for (Map.Entry<TermId, Double> e : multipliersMap.entrySet()) {
                printer.print(e.getKey().getValue());
                printer.print(e.getValue());
                printer.println();
            }
        }
    }

    public static Map<TermId, Double> read(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }

    public static Map<TermId, Double> read(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             CSVParser parser = CSV_FORMAT.parse(reader)) {

            Map<TermId, Double> values = new HashMap<>();
            for (CSVRecord record : parser) {
                if (record.hasComment())
                    continue;

                TermId termId = TermId.of(record.get(0));
                double multiplier = Double.parseDouble(record.get(1));
                values.put(termId, multiplier);
            }

            return Map.copyOf(values);
        }
    }
}

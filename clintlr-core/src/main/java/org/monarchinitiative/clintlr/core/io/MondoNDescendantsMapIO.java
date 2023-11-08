package org.monarchinitiative.clintlr.core.io;

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
import java.util.*;

public class MondoNDescendantsMapIO {
    private static final Logger LOGGER = LoggerFactory.getLogger(MondoNDescendantsMapIO.class);

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT;
    private MondoNDescendantsMapIO() {
    }

    public static void write(Map<TermId, Integer> map, Path path) throws IOException {
        try (OutputStream os = Files.newOutputStream(path)) {
            write(map, os);
        }
    }

    public static void write(Map<TermId, Integer> map, OutputStream os) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("MondoID", "NDescendants");
            for (Map.Entry<TermId, Integer> e : map.entrySet()) {
                printer.print(e.getKey().getValue());
                printer.print(e.getValue());
                printer.println();
            }
        }
    }

    public static Map<TermId, Integer> read(Path path) throws IOException {
        LOGGER.debug("Reading Mondo descendant counts from {}", path.toAbsolutePath());
        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }

    public static Map<TermId, Integer> read(InputStream is) throws IOException {
        Map<TermId, Integer> builder = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             CSVParser parser = CSV_FORMAT.parse(reader)) {
            for (CSVRecord record : parser) {
                if (parser.getCurrentLineNumber() == 1)
                    // Skip header
                    continue;

                TermId mondoId = TermId.of(record.get(0));
                Integer count = Integer.parseInt(record.get(1));
                builder.put(mondoId, count);
            }
        }

        return Map.copyOf(builder);
    }
}

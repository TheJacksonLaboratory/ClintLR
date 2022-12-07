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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OmimMapIO {
    private static final Logger LOGGER = LoggerFactory.getLogger(OmimMapIO.class);
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT;

    private OmimMapIO() {
    }

    public static void write(Map<TermId, List<TermId>> omimMap, OutputStream os) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
             CSVPrinter printer = CSV_FORMAT.print(writer)) {
            printer.printRecord("OmimID", "MondoIDs");
            for (Map.Entry<TermId, List<TermId>> e : omimMap.entrySet()) {
                printer.print(e.getKey());
                String mondoIds = e.getValue().stream()
                        .map(TermId::getValue)
                        .collect(Collectors.joining("|"));

                printer.print(mondoIds);
                printer.println();
            }
        }
    }

    public static void write(Map<TermId, List<TermId>> omimMap, Path path) throws IOException {
        LOGGER.debug("Writing OMIM map to {}", path.toAbsolutePath());
        try (OutputStream os = Files.newOutputStream(path)) {
            write(omimMap, os);
        }
    }

    public static Map<TermId, List<TermId>> read(InputStream is) throws IOException {
        Map<TermId, List<TermId>> builder = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             CSVParser parser = CSV_FORMAT.parse(reader)) {
            for (CSVRecord record : parser) {
                TermId omim = TermId.of(record.get(0));
                List<TermId> mondoIds = Arrays.stream(record.get(1).split("\\|"))
                        .map(TermId::of)
                        .toList();
                builder.put(omim, mondoIds);
            }
        }
        return Map.copyOf(builder);
    }

    public static Map<TermId, List<TermId>> read(Path path) throws IOException {
        LOGGER.debug("Reading OMIM to Mondo map from {}", path.toAbsolutePath());
        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }
}

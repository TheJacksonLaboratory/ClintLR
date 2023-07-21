package org.monarchinitiative.l4ci.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.l4ci.core.Relation;
import org.monarchinitiative.l4ci.core.io.HPOParser;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;


@CommandLine.Command(name = "ranges",
        hidden = true,
        mixinStandardHelpOptions = true,
        sortOptions = false,
        description = "Write text file to populate spreadsheet with broad and narrow clinical intuition terms")
public class IntuitionRangesCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntuitionRangesCommand.class);


    @CommandLine.Option(names = {"-p", "--phenopacket"},
            arity = "1..*",
            description = "Path(s) to phenopacket JSON file(s).")
    protected List<Path> phenopacketPaths;

    @CommandLine.Option(names = {"-B", "--batchDir"},
            description = "Path to directory containing phenopackets.")
    protected String batchDir;

    @CommandLine.Option(names = {"-M", "--mondo"},
            required = true,
            description = "Path to Mondo Ontology JSON file.")
    protected String mondoPath;

    @CommandLine.Option(names = {"-s", "--spreadsheet"},
            required = true,
            description = "Path to Spreadsheet file.")
    protected Path spreadsheetPath;

    @Override
    public Integer call() throws Exception {
        // 1 - check input
        if (batchDir != null) {
            phenopacketPaths = new ArrayList<>();
            File folder = new File(batchDir);
            File[] files = folder.listFiles();
            for (File file : Objects.requireNonNull(files)) {
                phenopacketPaths.add(file.toPath());
            }
        }

        // 2 - Get the target diseases from the phenopackets.
        OntologyData ontologyData = loadOntologies();
        Map<TermId, List<TermId>> omimToMondoMap = makeOmimMap(ontologyData.mondo);

        List<CIDiseases> CIDiseasesList = new ArrayList<>();
        for (Path phenopacketPath : phenopacketPaths) {
            System.out.println(phenopacketPath);
            CIDiseases selectedDisease = getSelectedDisease(omimToMondoMap, phenopacketPath, ontologyData.mondo);
            CIDiseasesList.add(selectedDisease);
        }

        // 3 - Write data to CSV file.
        try (BufferedWriter writer = openWriter(spreadsheetPath);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "targetMondoID", "targetOmimID", "targetName", "narrowID", "narrowName", "broadID", "broadName"); // header
            writeData(CIDiseasesList, printer);
        }
        LOGGER.info("Values written to file {}", spreadsheetPath);

        return 0;
    }

    private void writeData(List<CIDiseases> ciDiseasesList, CSVPrinter printer) {

        for (CIDiseases ciDiseases: ciDiseasesList) {
            try {
                printer.print(ciDiseases.phenopacketName);
                printer.print(ciDiseases.targetId == null ? "N/A" : ciDiseases.targetId.mondoId);
                printer.print(ciDiseases.targetId == null ? "N/A" : ciDiseases.targetId.omimId);
                printer.print(ciDiseases.targetName == null ? "N/A" : ciDiseases.targetName);
                printer.print(ciDiseases.narrowId == null ? "N/A" : ciDiseases.narrowId);
                printer.print(ciDiseases.narrowName == null ? "N/A" : ciDiseases.narrowName);
                printer.print(ciDiseases.broadId == null ? "N/A" : ciDiseases.broadId);
                printer.print(ciDiseases.broadName == null ? "N/A" : ciDiseases.broadName);
                printer.println();
            } catch (IOException e) {
                LOGGER.error("Error writing results for {}: {}", ciDiseases.phenopacketName, e.getMessage(), e);
            }
        }
    }

    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }

    private record CIDiseases(String phenopacketName, DiseaseId targetId, String targetName, TermId broadId, String broadName, TermId narrowId, String narrowName) {}

    private record OntologyData(Ontology mondo) {}

    OntologyData loadOntologies() {
        Ontology mondo = null;
        String path = mondoPath;
        if (path != null) {
            File file = new File(path);
            if (file.isFile()) {
                String msg = String.format("Loading Mondo from file '%s'", file.getAbsoluteFile());
                LOGGER.info(msg);
                HPOParser parser = new HPOParser(mondoPath);
                mondo = parser.getHPO();
                LOGGER.info("Loaded Mondo ontology");
            }
        }
        return new OntologyData(mondo);
    }


    private record DiseaseId(TermId mondoId, TermId omimId) {}
    CIDiseases getSelectedDisease(Map<TermId, List<TermId>> omimToMondoMap, Path phenopacketPath, Ontology ontology) throws Exception {
        PhenopacketData data = readPhenopacketData(phenopacketPath);
        String phenopacketName = phenopacketPath.getFileName().toString();
        List<TermId> diseaseIds = data.getDiseaseIds();
        TermId omimId = diseaseIds.get(0);
        if (omimToMondoMap.get(omimId) != null) {
            TermId mondoId = omimToMondoMap.get(omimId).get(0);
            DiseaseId targetId = new DiseaseId(mondoId, omimId);
            Term targetTerm = ontology.getTermMap().get(mondoId);
            String targetName = targetTerm.getName();
            Set<Term> parents = getTermRelations(targetTerm.id(), Relation.PARENT, ontology);
            List<Term> parentsList = parents.stream().toList();
            Term narrowTerm = parentsList.get(0);
            TermId narrowId = narrowTerm.id();
            String narrowName = narrowTerm.getName();
            Set<Term> grandparents = getTermRelations(narrowId, Relation.PARENT, ontology);
            List<Term> grandparentsList = grandparents.stream().toList();
            Term broadTerm = grandparentsList.get(0);
            TermId broadId = broadTerm.id();
            String broadName = broadTerm.getName();
            return new CIDiseases(phenopacketName, targetId, targetName, broadId, broadName, narrowId, narrowName);
        } else {
            return new CIDiseases(phenopacketName, null, null, null, null, null, null);
        }
    }


    private Map<TermId, List<TermId>> makeOmimMap(Ontology mondo) {
        Map<TermId, List<TermId>> omimToMondoMap = new HashMap<>();
        for (Term mondoTerm : mondo.getTerms()) {
            for (Dbxref ref : mondoTerm.getXrefs()) {
                String refName = ref.getName();
                if (refName.contains("OMIM:")) {
                    Term omimTerm = Term.of(refName, refName);
                    TermId omimID = omimTerm.id();
                    if (!omimToMondoMap.containsKey(omimID)) {
                        omimToMondoMap.put(omimID, new ArrayList<>());
                    }
                    List<TermId> termList = omimToMondoMap.get(omimID);
                    termList.add(mondoTerm.id());
                    omimToMondoMap.put(omimID, termList);
                    break;
                }
            }
        }
        return omimToMondoMap;
    }

    /**
     * Get the relations of "term"
     *
     * @param termId Mondo Term ID of interest
     * @param relation Relation of interest (ancestor, descendent, child, parent)
     * @return relations of term (not including term itself).
     */
    private Set<Term> getTermRelations(TermId termId, Relation relation, Ontology mondo) {
        Set<TermId> relationIds;
        switch (relation) {
            case ANCESTOR:
                relationIds = getAncestorTerms(mondo, termId, false);
                break;
            case DESCENDENT:
                relationIds = getDescendents(mondo, termId);
                break;
            case CHILD:
                relationIds = getChildTerms(mondo, termId, false);
                break;
            case PARENT:
                relationIds = getParentTerms(mondo, termId, false);
                break;
            default:
                return Set.of();
        }
        Set<Term> relations = new HashSet<>();
        relationIds.forEach(tid -> {
            Term ht = mondo.getTermMap().get(tid);
            relations.add(ht);
        });
        return relations;
    }


    private static PhenopacketData readPhenopacketData(Path phenopacketPath) throws LiricalParseException {
        PhenopacketData data = null;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.debug("Success!");
        } catch (Exception e) {
            LOGGER.debug("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
                LOGGER.debug("Success!");
            } catch (IOException e) {
                LOGGER.debug("Unable to parser as v1 phenopacket.");
                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
            }
        }

        // Check we have exactly one disease ID.
        if (data.getDiseaseIds().isEmpty())
            throw new LiricalParseException("Missing disease ID which is required for the benchmark!");
        else if (data.getDiseaseIds().size() > 1)
            throw new LiricalParseException("Saw >1 disease IDs {}, but we need exactly one for the benchmark!");
        return data;
    }

}


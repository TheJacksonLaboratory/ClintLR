package org.monarchinitiative.l2ci.cli.cmd;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.l2ci.core.OptionalHpoaResource;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.core.pretestprob.PretestProbability;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.phenol.io.OntologyLoader;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * Benchmark command runs LIRICAL on one or more phenopackets and writes prioritization results into a CSV table.
 * Optionally, a VCF file with background variants can be provided to run variant-aware prioritization.
 * In presence of variants, the benchmark spikes the variants from phenopacket into the background variants
 * and runs prioritization on phenotype terms and variants.
 */
@CommandLine.Command(name = "benchmark",
        hidden = true,
        mixinStandardHelpOptions = true,
        sortOptions = false,
        description = "Benchmark LIRICAL by analyzing a phenopacket (with or without VCF)")
public class BenchmarkCommand extends BaseLiricalCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkCommand.class);

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

    @CommandLine.Option(names = {"-H", "--hpo"},
            required = true,
            description = "Path to HPO Ontolgoy JSON file.")
    protected String hpoPath;

    @CommandLine.Option(names = {"-A", "--hpoa"},
            required = true,
            description = "Path to phenotype.hpoa annotation JSON file.")
    protected String hpoaPath;

    @CommandLine.Option(names = {"-m", "--multiplier"},
            description = "Pretest Multiplier value (default: ${DEFAULT-VALUE}).")
    protected double multiplier = 1.0;

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF with background variants.")
    protected Path vcfPath; // nullable

    @CommandLine.Option(names = {"-o", "--output"},
            required = true,
            description = "Where to write the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
    protected Path outputPath;

    @CommandLine.Option(names = {"--phenotype-only"},
            description = "Run the benchmark with phenotypes only (default: ${DEFAULT-VALUE})")
    protected boolean phenotypeOnly = false;

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    private enum Relation {
        ANCESTOR("ancestor"),
        CHILD("child"),
        DESCENDENT("descendent"),
        PARENT("parent");

        private final String name;

        Relation(String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    Ontology mondo;
    Ontology hpo;
    OptionalHpoaResource optionalHpoaResource = new OptionalHpoaResource();

    @Override
    public Integer call() throws Exception {
        printBanner();
        long start = System.currentTimeMillis();
        // The benchmark has a logic of its own, hence the `call()` method is overridden.
        // 0 - check input
        if (batchDir != null) {
            phenopacketPaths = new ArrayList<>();
            File folder = new File(batchDir);
            File[] files = folder.listFiles();
            for (File file : files) {
                phenopacketPaths.add(file.toPath());
            }
        }
        List<String> errors = checkInput();
        if (!errors.isEmpty())
            throw new LiricalException(String.format("Errors: %s", String.join(", ", errors)));

        // 1 - bootstrap LIRICAL.
        Lirical lirical = bootstrapLirical();

        // 2 - prepare the simulation data shared by all phenopackets.
        List<LiricalVariant> backgroundVariants = readBackgroundVariants(lirical);
        loadOntologies();
        Map<TermId, List<TermId>> omimToMondoMap = makeOmimMap();

        try (BufferedWriter writer = openWriter(outputPath);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "background_vcf", "selected_mondo_term", "selected_omim_term","multiplier","input_pretest_prob","benchmark_pretest_prob","sample_id", "rank",
                    "is_causal", "disease_id", "post_test_proba"); // header

            for (Path phenopacketPath : phenopacketPaths) {
                List<TermId> selectedTermId = getSelectedDisease(omimToMondoMap, phenopacketPath);
                Term selectedTerm = selectedTermId.get(0) == null ? null : Term.of(selectedTermId.get(0).toString(), selectedTermId.get(0).toString());
                Map<TermId, TermId> selectedDiseases = makeSelectedDiseasesMap(omimToMondoMap, selectedTerm);
                Map<TermId, Double> pretestMap = makeSelectedDiseasePretestMap(omimToMondoMap, selectedDiseases, multiplier);
                AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, pretestMap);
                // 3 - prepare benchmark data per phenopacket
                BenchmarkData benchmarkData = prepareBenchmarkData(lirical, backgroundVariants, phenopacketPath);

                // 4 - run the analysis.
                LOGGER.info("Starting the analysis: {}", analysisOptions);
                LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
                AnalysisResults results = analysisRunner.run(benchmarkData.analysisData(), analysisOptions);

                // 5 - summarize the results.
                String phenopacketName = phenopacketPath.toFile().getName();
                String backgroundVcf = vcfPath == null ? "" : vcfPath.toFile().getName();
                writeResults(phenopacketName, backgroundVcf, selectedTermId, selectedDiseases, multiplier, pretestMap, benchmarkData, results, printer);
            }
        }
        LOGGER.info("Benchmark results were stored to {}", outputPath.toAbsolutePath());

        reportElapsedTime(start, System.currentTimeMillis());
        return 0;
    }

    protected List<String> checkInput() {
        List<String> errors = super.checkInput();

        // Check if all phenopackets are valid and die quickly if not.
        LOGGER.info("Checking validity of {} phenopackets", phenopacketPaths.size());
        for (Path phenopacketPath : phenopacketPaths) {
            try {
                readPhenopacketData(phenopacketPath);
            } catch (LiricalParseException e) {
                errors.add("Invalid phenopacket %s: %s".formatted(phenopacketPath.toAbsolutePath(), e.getMessage()));
            }
        }

        return errors;
    }

    Void loadOntologies() {
        String[] paths = {mondoPath, hpoPath, hpoaPath};
        String[] types = {"Mondo", "HPO", "HPOA"};
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            String type = types[i];
            if (path != null) {
                File file = new File(path);
                if (file.isFile()) {
                    String msg = String.format("Loading " + type + " from file '%s'", file.getAbsoluteFile());
                    LOGGER.info(msg);
                    switch (type) {
                        case "Mondo":
                            HPOParser parser = new HPOParser(mondoPath);
                            mondo = parser.getHPO();
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case "HPO":
                            hpo = OntologyLoader.loadOntology(file);
                            LOGGER.info("Loaded " + type + " ontology");
                            break;
                        case "HPOA":
                            if (hpo != null) {
                                optionalHpoaResource.setAnnotationResources(path, hpo);
                            } else {
                                optionalHpoaResource.initializeWithEmptyMaps();
                                LOGGER.error("Cannot load phenotype.hpoa because HP ontology not loaded");
                                return null;
                            }
                            LOGGER.info("Loaded annotation maps");
                            break;
                    }
                }
            }
        }
        return null;
    }


    List<TermId> getSelectedDisease(Map<TermId, List<TermId>> omimToMondoMap, Path phenopacketPath) throws Exception {
        PhenopacketData data = readPhenopacketData(phenopacketPath);
        List<TermId> diseaseIds = data.getDiseaseIds();
        if (omimToMondoMap.get(diseaseIds.get(0)) == null || omimToMondoMap.get(diseaseIds.get(0)).isEmpty()) {
            return Arrays.asList(null, diseaseIds.get(0));
        } else {
            return Arrays.asList(omimToMondoMap.get(diseaseIds.get(0)).get(0), diseaseIds.get(0));
        }
    }

    Map<TermId, Double> makeSelectedDiseasePretestMap(Map<TermId, List<TermId>> omimToMondoMap, Map<TermId, TermId> selectedTerms, double adjProb) {
        Map<TermId, Double> diseaseMap = new HashMap<>();
        if (mondo == null) {
            LOGGER.error("makeSelectedDiseaseMap: Mondo ontology is null.");
            return diseaseMap;
        }
        for (TermId omimId : omimToMondoMap.keySet()) {
            diseaseMap.put(omimId, 1.0);
        }
        if (optionalHpoaResource != null) {
            for (TermId termId : optionalHpoaResource.getId2diseaseModelMap().keySet()) {
                diseaseMap.put(termId, 1.0);
            }
        }
        return PretestProbability.of(diseaseMap, selectedTerms.keySet(), adjProb, new ArrayList<>());
    }

    private Map<TermId, List<TermId>> makeOmimMap() {
        Ontology ontology = mondo;
        Map<TermId, List<TermId>> omimToMondoMap = new HashMap<>();
        for (Term mondoTerm : ontology.getTerms()) {
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

    Map<TermId, TermId> makeSelectedDiseasesMap(Map<TermId, List<TermId>> omimToMondoMap, Term selectedTerm) {
        HashMap<TermId, TermId> selectedTerms = new HashMap<>();
        if (selectedTerm != null) {
            omimToMondoMap.forEach((omimID, mondoIDs) -> {
                for (TermId mondoID : mondoIDs) {
                    if (mondoID.equals(selectedTerm.id()) && omimID != null) {
                        selectedTerms.put(omimID, mondoID);
                        Set<Term> descendents = getTermRelations(selectedTerm, Relation.DESCENDENT);
                        for (Term descendent : descendents) {
                            omimToMondoMap.forEach((omimID2, mondoIDs2) -> {
                                for (TermId mondoID2 : mondoIDs2) {
                                    if (mondoID2.equals(descendent.id())) {
                                        selectedTerms.put(omimID2, mondoID2);
                                        break;
                                    }
                                }
                            });
                        }
                        //                descendents.forEach(t -> selectedTerms.add(mondoTerm.id()));
                    }
                }
            });
        }
        return selectedTerms;
    }

    /**
     * Get the relations of "term"
     *
     * @param term Mondo Term of interest
     * @param relation Relation of interest (ancestor, descendent, child, parent)
     * @return relations of term (not including term itself).
     */
    private Set<Term> getTermRelations(Term term, Relation relation) {
        Ontology ontology = mondo;
        if (ontology == null) {
            LOGGER.error("Ontology null");
            return Set.of();
        }
        TermId termId = term.id();
        Set<TermId> relationIds;
        switch (relation) {
            case ANCESTOR:
                relationIds = getAncestorTerms(ontology, termId, false);
                break;
            case DESCENDENT:
                relationIds = getDescendents(ontology, termId);
                break;
            case CHILD:
                relationIds = getChildTerms(ontology, termId, false);
                break;
            case PARENT:
                relationIds = getParentTerms(ontology, termId, false);
                break;
            default:
                return Set.of();
        }
        Set<Term> relations = new HashSet<>();
        relationIds.forEach(tid -> {
            Term ht = ontology.getTermMap().get(tid);
            relations.add(ht);
        });
        return relations;
    }

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    private List<LiricalVariant> readBackgroundVariants(Lirical lirical) throws LiricalParseException {
        if (vcfPath == null) {
            LOGGER.info("Path to VCF file was not provided.");
            return List.of();
        }
        if (lirical.variantParserFactory().isEmpty()) {
            LOGGER.warn("Cannot process the provided VCF file {}, resources are not set.", vcfPath.toAbsolutePath());
            return List.of();
        }

        try (VariantParser variantParser = lirical.variantParserFactory().get().forPath(vcfPath)) {
            // Read variants
            LOGGER.info("Reading background variants from {}.", vcfPath.toAbsolutePath());
            ProgressReporter progressReporter = new ProgressReporter(10_000);
            List<LiricalVariant> variants = variantParser.variantStream()
                    .peek(v -> progressReporter.log())
                    .toList();
            progressReporter.summarize();
            return variants;
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }
    }

    private BenchmarkData prepareBenchmarkData(Lirical lirical,
                                               List<LiricalVariant> backgroundVariants,
                                               Path phenopacketPath) throws LiricalParseException {
        LOGGER.info("Reading phenopacket from {}.", phenopacketPath.toAbsolutePath());
        PhenopacketData data = readPhenopacketData(phenopacketPath);

        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
        List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();

        GenesAndGenotypes genes;
        if (phenotypeOnly) {
            // We omit the VCF even if provided.
            if (!backgroundVariants.isEmpty())
                LOGGER.warn("The provided VCF file will not be used in `--phenotype-only` mode");
            genes = GenesAndGenotypes.empty();
        } else {
            if (backgroundVariants.isEmpty()) // None or empty VCF file.
                genes = GenesAndGenotypes.empty();
            else {
                // Annotate the causal variants found in the phenopacket.
                FunctionalVariantAnnotator annotator = lirical.functionalVariantAnnotator();
                VariantMetadataService metadataService = lirical.variantMetadataService();
                List<LiricalVariant> backgroundAndCausal = new ArrayList<>(backgroundVariants.size() + 10);
                for (GenotypedVariant variant : data.getVariants()) {
                    List<TranscriptAnnotation> annotations = annotator.annotate(variant.variant());
                    List<VariantEffect> effects = annotations.stream()
                            .map(TranscriptAnnotation::getVariantEffects)
                            .flatMap(Collection::stream)
                            .distinct()
                            .toList();
                    VariantMetadata metadata = metadataService.metadata(variant.variant(), effects);

                    LiricalVariant lv = LiricalVariant.of(variant, annotations, metadata);
                    backgroundAndCausal.add(lv);
                }

                // Read the VCF file.
                genes = prepareGenesAndGenotypes(backgroundAndCausal);
            }
        }

        AnalysisData analysisData = AnalysisData.of(data.getSampleId(),
                data.getAge().orElse(null),
                data.getSex().orElse(null),
                presentTerms,
                excludedTerms,
                genes);

        return new BenchmarkData(data.getDiseaseIds().get(0), analysisData);
    }

    private static PhenopacketData readPhenopacketData(Path phenopacketPath) throws LiricalParseException {
        PhenopacketData data = null;
        try (InputStream is = Files.newInputStream(phenopacketPath)) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.debug("Success!");
        } catch (Exception e) {
            LOGGER.debug("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = Files.newInputStream(phenopacketPath)) {
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

    /**
     * Write results of a single benchmark into the provided {@code printer}.
     */
    private static void writeResults(String phenopacketName,
                                     String backgroundVcfName,
                                     List<TermId> selectedTerm,
                                     Map<TermId, TermId> selectedDiseases,
                                     double multiplier,
                                     Map<TermId, Double> pretestMap,
                                     BenchmarkData benchmarkData,
                                     AnalysisResults results,
                                     CSVPrinter printer) {
        AtomicInteger rankCounter = new AtomicInteger();
        results.resultsWithDescendingPostTestProbability()
                .forEachOrdered(result -> {
                    int rank = rankCounter.incrementAndGet();
                    try {
                        printer.print(phenopacketName);
                        printer.print(backgroundVcfName);
                        printer.print(selectedTerm.get(0) == null ? "N/A": selectedTerm.get(0));
                        printer.print(selectedTerm.get(1) == null ? "N/A": selectedTerm.get(1));
                        printer.print(!selectedDiseases.keySet().contains(result.diseaseId()) ? 1 : multiplier);
                        printer.print(pretestMap.get(result.diseaseId()));
                        printer.print(result.pretestProbability());
                        printer.print(benchmarkData.analysisData().sampleId());
                        printer.print(rank);
                        printer.print(result.diseaseId().equals(benchmarkData.diseaseId()));
                        printer.print(result.diseaseId());
                        printer.print(result.posttestProbability());
                        printer.println();
                    } catch (IOException e) {
                        LOGGER.error("Error writing results for {}: {}", result.diseaseId(), e.getMessage(), e);
                    }
                });
    }

    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }

    private record BenchmarkData(TermId diseaseId, AnalysisData analysisData) {
    }
}
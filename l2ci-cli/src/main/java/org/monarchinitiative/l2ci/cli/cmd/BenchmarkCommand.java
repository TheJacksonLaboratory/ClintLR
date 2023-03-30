package org.monarchinitiative.l2ci.cli.cmd;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.l2ci.core.OptionalHpoaResource;
import org.monarchinitiative.l2ci.core.Relation;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.core.io.OmimMapIO;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.LiricalDataException;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;



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

    protected static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkCommand.class);

    protected static final String OMIM_2_MONDO_NAME = "omim2mondo.csv";

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
            description = "Path to HPO Ontology JSON file.")
    protected String hpoPath;

    @CommandLine.Option(names = {"-A", "--hpoa"},
            description = "Path to phenotype.hpoa annotation JSON file.")
    protected String hpoaPath;

    @CommandLine.Option(names = {"-m", "--multiplier"},
            split=",",
            arity = "1..*",
            description = "Pretest Multiplier value(s).")
    protected List<Double> multipliers;

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF with background variants.")
    protected Path vcfPath; // nullable

    @CommandLine.Option(names = {"-r", "--range"},
            description = "File containing ranges of terms for each phenopacket.")
    protected Path rangeFilePath;

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
            required = true,
            description = "Where to write the benchmark results files.")
    protected Path outputDir;

    @CommandLine.Option(names = {"-o", "--outputFilename"},
            description = "Filename of the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
    protected String outputName;

//    @CommandLine.Option(names = {"--html"},
//            description = "Whether to write out HTML files of the results (default: ${DEFAULT-VALUE})")
//    protected boolean writeHTML = false;

    @CommandLine.Option(names = {"--phenotype-only"},
            description = "Run the benchmark with phenotypes only (default: ${DEFAULT-VALUE})")
    protected boolean phenotypeOnly = false;

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

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
            assert files != null;
            for (File file : files) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile() && !basicFileAttributes.isDirectory() && !file.getName().startsWith(".")) {
                    phenopacketPaths.add(file.toPath());
                }
            }
        }
        List<String> errors = checkInput();
        if (!errors.isEmpty())
            throw new LiricalException(String.format("Errors: %s", String.join(", ", errors)));

        // 1 - bootstrap LIRICAL.
        Lirical lirical = bootstrapLirical();

        // 2 - prepare the simulation data shared by all phenopackets.
        List<LiricalVariant> backgroundVariants = readBackgroundVariants(lirical);
        OntologyData ontologyData = loadOntologies();
        Path dataDirectory = Path.of(String.join(File.separator, System.getProperty("user.home"), ".l4ci", "data"));
        Path omimToMondoPath = dataDirectory.resolve(OMIM_2_MONDO_NAME);
        Map<TermId, List<TermId>> omim2Mondo;
        if (Files.exists(omimToMondoPath)) {
            omim2Mondo = OmimMapIO.read(omimToMondoPath);
        } else {
            omim2Mondo = makeOmimMap(ontologyData.mondo);
        }

        // Flip the OMIM to Mondo map
        Map<TermId, TermId> mondoToOmim = new HashMap<>();
        for (Map.Entry<TermId, List<TermId>> e : omim2Mondo.entrySet()) {
            for (TermId mondoId : e.getValue()) {
                mondoToOmim.put(mondoId, e.getKey());
            }
        }

        List<String> rangeLines = null;
        if (rangeFilePath != null && Files.isRegularFile(rangeFilePath)) {
            try (InputStream is = Files.newInputStream(rangeFilePath)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                Stream<String> rangeStream = reader.lines();
                rangeLines = rangeStream.toList();
                LOGGER.debug("Read file {}.", rangeFilePath);
            } catch (Exception e) {
                LOGGER.debug("Unable to read file {}.", rangeFilePath);
            }
        }

        if (multipliers != null) {
            for (Double multiplier : multipliers) {
                HashMap<String, Path> outputPaths = new HashMap<>();
                String fileExt = outputName.substring(outputName.lastIndexOf("."));
                String targetFileName = outputName.substring(0, outputName.lastIndexOf(".")) + "_" + multiplier + fileExt;
                outputPaths.put("target", outputDir.resolve(targetFileName));
                if (rangeLines != null) {
                    String narrowFileName = targetFileName.substring(0, targetFileName.lastIndexOf(".")) + "_narrow" + fileExt;
                    outputPaths.put("narrow", outputDir.resolve(narrowFileName));
                    String broadFileName = targetFileName.substring(0, targetFileName.lastIndexOf(".")) + "_broad" + fileExt;
                    outputPaths.put("broad", outputDir.resolve(broadFileName));
                }
                runAnalysis(lirical, backgroundVariants, outputPaths, rangeLines, multiplier, omim2Mondo, ontologyData);
            }
        } else {
            HashMap<String, Path> outputPaths = new HashMap<>();
            outputPaths.put("target", outputDir.resolve(outputName));
            if (rangeLines != null) {
                String fileExt = outputName.substring(outputName.lastIndexOf("."));
                String narrowFileName = outputName.substring(0, outputName.lastIndexOf(".")) + "_narrow" + fileExt;
                outputPaths.put("narrow", outputDir.resolve(narrowFileName));
                String broadFileName = outputName.substring(0, outputName.lastIndexOf(".")) + "_broad" + fileExt;
                outputPaths.put("broad", outputDir.resolve(broadFileName));
            }
            runAnalysis(lirical, backgroundVariants, outputPaths, rangeLines, 0.0, omim2Mondo, ontologyData);
        }
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

    protected void runAnalysis(Lirical lirical, List<LiricalVariant> backgroundVariants, HashMap<String, Path> outputPaths, List<String> rangeLines, Double multiplier, Map<TermId, List<TermId>> omimToMondoMap, OntologyData ontologyData) throws Exception {
        String[] types = {"target", "narrow", "broad"};
        Set<TermId> omimIDs = omimToMondoMap.keySet();
        for (String type : types) {
            if (outputPaths.get(type) != null) {
                System.out.print("Starting " + type);
                try (BufferedWriter writer = openWriter(outputPaths.get(type)); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
                    printer.printRecord("phenopacket", "background_vcf", "selected_mondo_term", "selected_omim_term", "multiplier", "input_pretest_prob", "benchmark_pretest_prob", "sample_id", "rank",
                            "is_causal", "disease_id", "post_test_proba", "LR", "numerator", "denominator"); // header

                    for (Path phenopacketPath : phenopacketPaths) {
                        System.out.println("Range Filepath: " + rangeFilePath + ", " + rangeLines.get(phenopacketPaths.indexOf(phenopacketPath)));
                        System.out.println(phenopacketPaths.indexOf(phenopacketPath) + " of " + phenopacketPaths.size() + ": " + type + " " + multiplier + " " + phenopacketPath);
                        TargetDiseases targetDiseases;
                        if (rangeLines != null) {
                            targetDiseases = getSelectedDiseases(omimToMondoMap, phenopacketPath, rangeLines);
                        } else {
                            targetDiseases = new TargetDiseases(getSelectedDisease(omimToMondoMap, phenopacketPath), null, null);
                        }
                        DiseaseId selectedDisease = targetDiseases.targetId();
                        switch (type) {
                            case "narrow" -> selectedDisease = targetDiseases.narrowId();
                            case "broad" -> selectedDisease = targetDiseases.broadId();
                        }
                        if (selectedDisease != null) {
                            Map<TermId, TermId> selectedDiseases = makeSelectedDiseasesMap(omimToMondoMap, selectedDisease.mondoId, ontologyData.mondo, omimIDs);
                            Map<TermId, Double> pretestMap = makeSelectedDiseasePretestMap(selectedDiseases, multiplier);
                            AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, pretestMap, omimToMondoMap);
                            // 3 - prepare benchmark data per phenopacket
                            BenchmarkData benchmarkData = prepareBenchmarkData(lirical, backgroundVariants, phenopacketPath);
                            String sampleId = benchmarkData.analysisData().sampleId();

                            // 4 - run the analysis.
                            LOGGER.info("Starting the analysis: {}", analysisOptions);
                            LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
                            AnalysisResults results = analysisRunner.run(benchmarkData.analysisData(), analysisOptions);

                            // 5 - summarize the results.
                            String phenopacketName = phenopacketPath.toFile().getName();
                            String backgroundVcf = vcfPath == null ? "" : vcfPath.toFile().getName();
                            writeResults(phenopacketName, backgroundVcf, selectedDisease, selectedDiseases, multiplier, pretestMap, benchmarkData, results, printer);

                            // Write out the results into HTML file, if desired.
//                            if (writeHTML) {
//                                writeHTMLFile(selectedDisease, multiplier, phenopacketName, type,
//                                        lirical, sampleId, benchmarkData, results);
//                            }
                        }
                    }
                }
                System.out.println("Finished " + type);
                LOGGER.info("Benchmark results were stored to {}", outputPaths.get(type));
            }
        }
    }

    protected record OntologyData(Ontology mondo, Ontology hpo, OptionalHpoaResource optionalHpoaResource) {}

    protected OntologyData loadOntologies() {
        String[] paths = {mondoPath, hpoPath, hpoaPath};
        String[] types = {"Mondo", "HPO", "HPOA"};
        Ontology mondo = null;
        Ontology hpo = null;
        OptionalHpoaResource optionalHpoaResource = new OptionalHpoaResource();
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
        return new OntologyData(mondo, hpo, optionalHpoaResource);
    }


    protected record DiseaseId(TermId mondoId, TermId omimId) {}
    protected static DiseaseId getSelectedDisease(Map<TermId, List<TermId>> omimToMondoMap, Path phenopacketPath) throws Exception {
        PhenopacketData data = readPhenopacketData(phenopacketPath);
        List<TermId> diseaseIds = data.getDiseaseIds();
        TermId omimId = diseaseIds.get(0);
        TermId mondoId = null;
        if (omimToMondoMap.get(omimId) != null) {
            mondoId = omimToMondoMap.get(omimId).get(0);
        }
        return new DiseaseId(mondoId, omimId);
    }

    protected record TargetDiseases(DiseaseId targetId, DiseaseId narrowId, DiseaseId broadId) {}
    protected static TargetDiseases getSelectedDiseases(Map<TermId, List<TermId>> omimToMondoMap, Path phenopacketPath, List<String> rangeLines) throws Exception {
        String phenopacketName = phenopacketPath.getFileName().toString();
        List<String> ids = new ArrayList<>();
        Term targetOmimTerm = null;
        for (String line : rangeLines) {
            if (line.contains(phenopacketName)) {
                String[] items = line.split("\t");
                for (String item : items) {
                    if (item.contains("MONDO") && !item.contains("TO")) {
                        ids.add(item);
                    } else if (item.contains("OMIM")) {
                        targetOmimTerm = Term.of(item, item);
                    }
                }
                break;
            }
        }
        if (ids.isEmpty()) {
            return new TargetDiseases(new DiseaseId(null, null), new DiseaseId(null, null), new DiseaseId(null, null));
        } else {
            List<DiseaseId> diseaseIds = new ArrayList<>();
            for (String id : ids) {
                TermId omimId = null;
                TermId mondoId = Term.of(id, id).id();
                if (ids.indexOf(id) == 0) {
                    if (targetOmimTerm != null)
                        omimId = targetOmimTerm.id();
                } else {
                    for (TermId omimIdKey : omimToMondoMap.keySet()) {
                        List<TermId> mondoIds = omimToMondoMap.get(omimIdKey);
                        if (mondoIds.contains(mondoId)) {
                            omimId = omimIdKey;
                        }
                    }
                }
                diseaseIds.add(new DiseaseId(mondoId, omimId));
            }
            DiseaseId narrowId = null;
            DiseaseId broadId = null;
            DiseaseId targetId = diseaseIds.get(0);
            if (diseaseIds.size() > 1) {
                narrowId = diseaseIds.get(1);
            }
            if (diseaseIds.size() > 2) {
                broadId = diseaseIds.get(2);
            }


            return new TargetDiseases(targetId, narrowId, broadId);
        }
    }

    protected Map<TermId, Double> makeSelectedDiseasePretestMap(Map<TermId, TermId> selectedTerms, double adjProb) {
        Map<TermId, Double> diseaseMap = new HashMap<>();
        for (TermId omimId : selectedTerms.keySet()) {
            diseaseMap.put(omimId, adjProb);
        }
        return diseaseMap;
    }

    protected Map<TermId, List<TermId>> makeOmimMap(Ontology mondo) {
        Map<TermId, List<TermId>> builder = new HashMap<>();
        for (Term mondoTerm : mondo.getTerms()) {
            for (Dbxref ref : mondoTerm.getXrefs()) {
                String refName = ref.getName();
                if (refName.startsWith("OMIM:")) {
                    TermId omimId = TermId.of(refName);
                    builder.computeIfAbsent(omimId, i -> new ArrayList<>())
                            .add(mondoTerm.id());
                    break;
                }
            }
        }
        return Map.copyOf(builder);
    }

    Map<TermId, TermId> makeSelectedDiseasesMap(Map<TermId, List<TermId>> omimToMondoMap, TermId mondoId,
                                                Ontology mondo, Set<TermId> omimIDs) {
        HashMap<TermId, TermId> selectedTerms = new HashMap<>();
        for (TermId omimID : omimIDs) {
            List<TermId> mondoIDs = omimToMondoMap.get(omimID);
            if (mondoIDs.contains(mondoId)) {
                Set<Term> descendents = Relation.getTermRelations(mondo, mondoId, Relation.DESCENDENT);
                descendents.forEach(d -> System.out.print(d.id() + " "));
                for (Term descendent : descendents) {
                    for (TermId omimID2 : omimIDs) {
                        List<TermId> mondoIDs2 = omimToMondoMap.get(omimID2);
                        if (mondoIDs2.contains(descendent.id())) {
                            selectedTerms.put(omimID2, descendent.id());
                            break;
                        }
                    }
                }
            }
        }
        return selectedTerms;
    }

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    private List<LiricalVariant> readBackgroundVariants(Lirical lirical) throws LiricalDataException {
        if (vcfPath == null) {
            LOGGER.info("Path to VCF file was not provided.");
            return List.of();
        }

        List<LiricalVariant> variants = new ArrayList<>();
        // Read variants
        Optional<VariantParser> optionalVariantParser = lirical.variantParserFactory().forPath(vcfPath, parseGenomeBuild(getGenomeBuild()), runConfiguration.transcriptDb);
        if (optionalVariantParser.isPresent()) {
            LOGGER.info("Reading background variants from {}.", vcfPath.toAbsolutePath());
            VariantParser variantParser = optionalVariantParser.get();
            ProgressReporter progressReporter = new ProgressReporter(10_000);
            variants = variantParser.variantStream()
                    .peek(v -> progressReporter.log())
                    .toList();
            progressReporter.summarize();
        }
        return variants;
    }

    private BenchmarkData prepareBenchmarkData(Lirical lirical,
                                               List<LiricalVariant> backgroundVariants,
                                               Path phenopacketPath) throws LiricalParseException, LiricalDataException {
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
                FunctionalVariantAnnotator annotator = getFunctionalVariantAnnotator(lirical, parseGenomeBuild(getGenomeBuild())); //lirical.functionalVariantAnnotator();
                VariantMetadataService metadataService = lirical.variantMetadataServiceFactory().getVariantMetadataService(parseGenomeBuild(getGenomeBuild())).get();
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

    protected static PhenopacketData readPhenopacketData(Path phenopacketPath) throws LiricalParseException {
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
                                     DiseaseId selectedTerm,
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
                        printer.print(selectedTerm.mondoId == null ? "N/A": selectedTerm.mondoId);
                        printer.print(selectedTerm.omimId == null ? "N/A": selectedTerm.omimId);
                        printer.print(!selectedDiseases.keySet().contains(result.diseaseId()) ? 1 : multiplier);
                        printer.print(pretestMap.get(result.diseaseId()));
                        printer.print(result.pretestProbability());
                        printer.print(benchmarkData.analysisData().sampleId());
                        printer.print(rank);
                        printer.print(result.diseaseId().equals(benchmarkData.diseaseId()));
                        printer.print(result.diseaseId());
                        printer.print(result.posttestProbability());
                        printer.print(result.observedResults().get(0).lr());
                        /**
                        printer.print(result.observedResults().get(0).numerator());
                        printer.print(result.observedResults().get(0).denominator());
                         */
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

    private void writeHTMLFile(DiseaseId selectedDisease, double multiplier, String phenopacketName, String type,
                                 Lirical lirical, String sampleId, BenchmarkData benchmarkData,
                                 AnalysisResults results) throws Exception {
        TermId diseaseId = selectedDisease.mondoId() == null ? selectedDisease.omimId() : selectedDisease.mondoId();
        double multiplierValue = multiplier; //!selectedDiseases.containsKey(diseaseId) ? 1 : multiplier;
        if (runConfiguration.globalAnalysisMode) {
            multiplierValue = 0;
        }
        String diseaseIdString = diseaseId == null ? "NA" : diseaseId.toString();
        String htmlFilename = String.join("_",
                phenopacketName.replace(".json", ""),
                type,
                diseaseIdString.replace(":", ""),
                "multiplier",
                Double.toString(multiplierValue));
        OutputOptions outputOptions = createOutputOptions(outputDir, htmlFilename);
        GenesAndGenotypes gene2Genotypes = benchmarkData.analysisData.genes();
        AnalysisResultsMetadata metadata = prepareAnalysisResultsMetadata(gene2Genotypes, lirical, sampleId);
        Optional<AnalysisResultsWriter> htmlWriter = lirical.analysisResultsWriterFactory()
                .getWriter(OutputFormat.HTML);
        if (htmlWriter.isPresent()) {
            htmlWriter.get()
                    .process(benchmarkData.analysisData(), results, metadata, outputOptions);
            // Finally, return path where the resulting HTML should land at.
            outputDir.resolve(htmlFilename + ".html");
        }
    }
}

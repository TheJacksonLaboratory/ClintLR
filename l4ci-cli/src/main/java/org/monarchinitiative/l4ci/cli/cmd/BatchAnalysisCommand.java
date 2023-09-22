package org.monarchinitiative.l4ci.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.Set;


/**
 * Batch command runs LIRICAL on one or more phenopackets and writes results into HTML files.
 * Optionally, a VCF file with variants can be provided to run variant-aware prioritization.
 */
@CommandLine.Command(name = "batch",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        description = "Batch LIRICAL analysis of phenopackets (with or without VCF)")
public class BatchAnalysisCommand extends BenchmarkCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchAnalysisCommand.class);

    @CommandLine.Option(names = {"--format"},
            paramLabel = "{tsv,html,json}",
            description = "Output format (default: ${DEFAULT-VALUE}).")
    protected String outputFormatArg = "tsv";

    @CommandLine.Option(names = {"--compress"},
            paramLabel = "{tsv,html,json}",
            description = "Whether to output file as a compressed file (default: ${DEFAULT-VALUE}).")
    protected boolean compress = false;



    @Override
    public Integer call() throws Exception {
        printBanner();
        long start = System.currentTimeMillis();

        Lirical lirical = prepareLirical();

        OntologyData ontologyData = loadOntologies();

        Map<TermId, TermId> omim2Mondo = getOmimMap(ontologyData);

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
                runAnalysis(lirical, rangeLines, multiplier, omim2Mondo, ontologyData);
            }
        } else {
            runAnalysis(lirical, rangeLines, 0.0, omim2Mondo, ontologyData);
        }
        reportElapsedTime(start, System.currentTimeMillis());
        return 0;
    }

    protected void runAnalysis(Lirical lirical, List<String> rangeLines, Double multiplier, Map<TermId, TermId> omimToMondoMap, OntologyData ontologyData) throws Exception {
        String[] types = {"target"}; //, "narrow", "broad"};
        if (rangeLines != null)
            types = new String[]{"target", "narrow", "broad"};
        Set<TermId> omimIDs = omimToMondoMap.keySet();

        for (String type : types) {
            LOGGER.info("Starting " + type);

            for (Path phenopacketPath : phenopacketPaths) {
                LOGGER.info(phenopacketPaths.indexOf(phenopacketPath) + " of " + phenopacketPaths.size() + ": " + type + " " + multiplier + " " + phenopacketPath);
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
                    LOGGER.info("Selected Disease = " + selectedDisease.mondoId());
                    Map<TermId, TermId> selectedDiseases = makeSelectedDiseasesMap(omimToMondoMap, selectedDisease.mondoId(), ontologyData.mondo(), omimIDs);
                    Map<TermId, Double> pretestMap = makeSelectedDiseasePretestMap(selectedDiseases, multiplier);
                    AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, pretestMap, omimToMondoMap);
                    // Read phenotypic features.
                    PhenopacketData phenopacketData = readPhenopacketData(phenopacketPath);

                    // Read variants if present.
                    GenesAndGenotypes gene2Genotypes = readVariants(vcfPath, lirical, analysisOptions.genomeBuild());

                    // Assemble the analysis data.
                    AnalysisData analysisData = AnalysisData.of(phenopacketData.getSampleId(),
                            phenopacketData.getAge().orElse(Age.ageNotKnown()),
                            phenopacketData.getSex().orElse(Sex.UNKNOWN),
                            phenopacketData.getHpoTerms().toList(),
                            phenopacketData.getNegatedHpoTerms().toList(),
                            gene2Genotypes);

                    // 4 - run the analysis.
                    LOGGER.info("Starting the analysis: {}", analysisOptions);
                    String sampleId = analysisData.sampleId();
                    LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
                    AnalysisResults results = analysisRunner.run(analysisData, analysisOptions);

                    // Write out the results to output file
                    String phenopacketName = phenopacketPath.toFile().getName();
                    TermId diseaseId = selectedDisease.mondoId() == null ? selectedDisease.omimId() : selectedDisease.mondoId();
                    double multiplierValue = multiplier; //!selectedDiseases.containsKey(diseaseId) ? 1 : multiplier;
                    if (runConfiguration.globalAnalysisMode) {
                        multiplierValue = 0;
                    }
                    String diseaseIdString = diseaseId == null ? "NA" : diseaseId.toString();
                    String outFilename = String.join("_",
                            phenopacketName.replace(".json", ""),
                            type,
//                            diseaseIdString.replace(":", ""),
                            "multiplier",
                            Double.toString(multiplierValue));
                    AnalysisResultsMetadata metadata = prepareAnalysisResultsMetadata(gene2Genotypes, lirical, sampleId);
                    writeResultsToFile(lirical, OutputFormat.parse(outputFormatArg), analysisData, results, metadata, outFilename);
                }
            }
            System.out.println("Finished " + type);
        }
    }

    private void writeResultsToFile(Lirical lirical, OutputFormat outputFormat, AnalysisData analysisData,
                                    AnalysisResults results, AnalysisResultsMetadata metadata, String outFilename) throws IOException {
        OutputOptions outputOptions = createOutputOptions(outputDir, outFilename);
        Optional<AnalysisResultsWriter> writer = lirical.analysisResultsWriterFactory().getWriter(outputFormat);
        if (writer.isPresent()) {
            writer.get().process(analysisData, results, metadata, outputOptions);
            outputDir.resolve(outFilename + "." + outputFormatArg.toLowerCase());
        }
        if (compress) {
            zip(outputDir.resolve(outFilename + "." + outputFormatArg.toLowerCase()));
        }
    }


    private static void zip(Path filePath) throws IOException {
        if (Files.isRegularFile(filePath) && Files.isReadable(filePath)) {
            byte[] buffer = new byte[2048];
            FileInputStream inputStream = new FileInputStream(filePath.toString());
            FileOutputStream outputStream = new FileOutputStream(filePath + ".gz");
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                gzipOutputStream.write(buffer, 0, length);
            }
            Files.delete(filePath);
            inputStream.close();
            gzipOutputStream.close();
        }
    }



}

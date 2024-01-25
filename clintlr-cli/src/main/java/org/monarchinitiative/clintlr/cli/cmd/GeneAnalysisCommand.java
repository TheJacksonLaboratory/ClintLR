package org.monarchinitiative.clintlr.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.AssociationType;
import org.monarchinitiative.phenol.annotations.formats.hpo.DiseaseToGeneAssociations;
import org.monarchinitiative.phenol.annotations.formats.hpo.GeneToAssociation;
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
@CommandLine.Command(name = "genes",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        description = "LIRICAL analysis of phenopackets (with or without VCF) using a gene list")
public class GeneAnalysisCommand extends BatchAnalysisCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneAnalysisCommand.class);

    @CommandLine.Option(names = {"-G", "--genes"},
            description = "File containing a list of genes.")
    protected Path geneFilePath;



    @Override
    public Integer call() throws Exception {
        printBanner();
        long start = System.currentTimeMillis();

        Lirical lirical = prepareLirical();

        OntologyData ontologyData = loadOntologies();

        Map<TermId, TermId> omim2Mondo = getOmimMap(ontologyData);

        List<String> geneLines = null;
        if (geneFilePath != null && Files.isRegularFile(geneFilePath)) {
            try (InputStream is = Files.newInputStream(geneFilePath)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                Stream<String> geneStream = reader.lines();
                geneLines = geneStream.toList();
                LOGGER.debug("Read file {}.", geneFilePath);
            } catch (Exception e) {
                LOGGER.debug("Unable to read file {}.", geneFilePath);
            }
        }

        if (multipliers != null) {
            for (Double multiplier : multipliers) {
                runAnalysis(lirical, geneLines, multiplier, omim2Mondo, ontologyData);
            }
        } else {
            runAnalysis(lirical, geneLines,0.0, omim2Mondo, ontologyData);
        }
        reportElapsedTime(start, System.currentTimeMillis());
        return 0;
    }

    protected void runAnalysis(Lirical lirical, List<String> geneLines, Double multiplier, Map<TermId, TermId> omimToMondoMap, OntologyData ontologyData) throws Exception {
        String[] types = {"target"}; //, "narrow", "broad"};
        if (geneLines != null)
            types = new String[]{"target", "genes"};
        Set<TermId> omimIDs = omimToMondoMap.keySet();

        Map<GeneIdentifier, List<TermId>> geneIdToDiseases = null;
        if (geneLines != null) {
            Map<TermId, Collection<GeneToAssociation>> diseaseToGeneAssociations = getDiseaseToGeneAssociations();
            geneIdToDiseases = getGeneIdToDiseases(diseaseToGeneAssociations);
        }

        for (String type : types) {
            LOGGER.info("Starting " + type);

            for (Path phenopacketPath : phenopacketPaths) {
                LOGGER.info(phenopacketPaths.indexOf(phenopacketPath) + " of " + phenopacketPaths.size() + ": " + type + " " + multiplier + " " + phenopacketPath);
                TargetDiseases targetDiseases = new TargetDiseases(getSelectedDisease(omimToMondoMap, phenopacketPath), null, null);
                DiseaseId selectedDisease = targetDiseases.targetId();
                LOGGER.info("Target Disease = " + selectedDisease.omimId() + ", " + selectedDisease.mondoId());
                TermId diseaseId = selectedDisease.mondoId() == null ? selectedDisease.omimId() : selectedDisease.mondoId();
                Map<TermId, Double> pretestMap;
                if (type.equals("genes")) {
                    pretestMap = makeGenePretestMap(multiplier, geneLines, geneIdToDiseases);
                    LOGGER.info("genes pretest map = " + pretestMap);
                } else {
                    Map<TermId, TermId> selectedDiseases = makeSelectedDiseasesMap(omimToMondoMap, selectedDisease.mondoId(), ontologyData.mondo(), omimIDs);
                    pretestMap = makeSelectedDiseasePretestMap(selectedDiseases, multiplier);
                }

                AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, pretestMap, omimToMondoMap);
                // Read phenotypic features.
                PhenopacketData phenopacketData = readPhenopacketData(phenopacketPath);

                // Read variants if present.
                GenesAndGenotypes gene2Genotypes = readVariants(vcfPath, lirical, analysisOptions.genomeBuild());

                // Assemble the analysis data.
                AnalysisData analysisData = AnalysisData.of(phenopacketData.sampleId(),
                        phenopacketData.parseAge().orElse(null),
                        phenopacketData.parseSex().orElse(Sex.UNKNOWN),
                        phenopacketData.presentHpoTermIds().toList(),
                        phenopacketData.excludedHpoTermIds().toList(),
                        gene2Genotypes);

                // 4 - run the analysis.
                LOGGER.info("Starting the analysis: {}", analysisOptions);
                String sampleId = analysisData.sampleId();
                AnalysisResults results;
                try (LiricalAnalysisRunner analysisRunner = lirical.analysisRunner()) {
                    results = analysisRunner.run(analysisData, analysisOptions);
                }

                // Write out the results to output file
                String phenopacketName = phenopacketPath.toFile().getName();
                double multiplierValue = multiplier; //!selectedDiseases.containsKey(diseaseId) ? 1 : multiplier;
                if (runConfiguration.globalAnalysisMode) {
                    multiplierValue = 0;
                }
                String diseaseIdString = diseaseId == null ? "NA" : diseaseId.toString();
                String outFilename;
                if (outputName != null) {
                    outFilename = outputName;
                } else {
                    outFilename = String.join("_",
                            phenopacketName.replace(".json", ""),
                            type,
                            diseaseIdString.replace(":", ""),
                            "multiplier",
                            Double.toString(multiplierValue));
                }
                AnalysisResultsMetadata metadata = prepareAnalysisResultsMetadata(gene2Genotypes, lirical, sampleId);
                writeResultsToFile(lirical, OutputFormat.parse(outputFormatArg), analysisData, results, metadata, outFilename);
            }
            LOGGER.info("Finished " + type);
        }
    }

    private Map<TermId, Collection<GeneToAssociation>> getDiseaseToGeneAssociations() throws IOException {
        DiseaseToGeneAssociations disease2GeneAssociations = getDisease2GeneAssociations();
        Map<TermId, Collection<GeneToAssociation>> diseaseId2GeneAssociations = new HashMap<>();
        disease2GeneAssociations.diseaseIdToGeneAssociations().forEach((key, value) -> {
            for (GeneToAssociation gene2association : value) {
                if (gene2association.associationType().equals(AssociationType.MENDELIAN) || gene2association.associationType().equals(AssociationType.DIGENIC)) {
                    if (!diseaseId2GeneAssociations.containsKey(key)) {
                        diseaseId2GeneAssociations.put(key, new ArrayList<>());
                    }
                    diseaseId2GeneAssociations.get(key).add(gene2association);
                }
            }
        });
//        LOGGER.info(diseaseId2GeneAssociations);
        return diseaseId2GeneAssociations;
    }

    private Map<GeneIdentifier, List<TermId>> getGeneIdToDiseases(Map<TermId, Collection<GeneToAssociation>> diseaseId2GeneAssociations) {
        // Flip the Disease to Gene Associations map
        Map<GeneIdentifier, List<TermId>> geneId2Diseases = new HashMap<>();
        for (Map.Entry<TermId, Collection<GeneToAssociation>> e : diseaseId2GeneAssociations.entrySet()) {
            for (GeneToAssociation geneToAssociation : e.getValue()) {
                GeneIdentifier geneId = geneToAssociation.geneIdentifier();
                if (!geneId2Diseases.containsKey(geneId)) {
                    geneId2Diseases.put(geneId, new ArrayList<>());
                }
                geneId2Diseases.get(geneId).add(e.getKey());
            }
        }
//        LOGGER.info(geneId2Diseases);
        return geneId2Diseases;
    }

    protected static Map<TermId, Double> makeGenePretestMap(double multiplier, List<String> geneLines, Map<GeneIdentifier, List<TermId>> geneIdToDiseases) {
        Map<TermId, Double> pretestMap = new HashMap<>();
        LOGGER.info(String.valueOf(geneLines));
        for (String line : geneLines) {
            String[] items = line.split(",");
            for (String item : items) {
                List<GeneIdentifier> geneList = geneIdToDiseases.keySet().stream().filter(gId -> gId.symbol().equals(item.strip())).toList();
                geneList.forEach(geneId -> {
                    List<TermId> geneOmimIds = geneIdToDiseases.get(geneId);
                    geneOmimIds.forEach(omimId -> pretestMap.put(omimId, multiplier));
                });
            }
        }
        return pretestMap;
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

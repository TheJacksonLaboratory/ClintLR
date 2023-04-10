package org.monarchinitiative.l4ci.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiricalAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(LiricalAnalysis.class);
    Lirical lirical;
    Properties pgProperties;

    OutputOptions outputOptions;

    // TODO - make interface with getters for LiricalResources, and use that instead of Properties to get LIRICAL resources
    public LiricalAnalysis(Lirical lirical, Properties pgProperties) {
        this.lirical = lirical;
        this.pgProperties = pgProperties;
    }

    public void runAnalysis(Map<TermId, Double> preTestMap, String phenopacketFile, String vcfFile, OutputOptions outputOptions) throws Exception {
        AnalysisData analysisData = prepareAnalysisData(lirical, phenopacketFile, vcfFile);
        AnalysisOptions analysisOptions = AnalysisOptions.of(false, PretestDiseaseProbability.of(preTestMap));
        LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
        AnalysisResults results = analysisRunner.run(analysisData, analysisOptions);
        FilteringStats filteringStats = analysisData.genes().computeFilteringStats();
        AnalysisResultsMetadata metadata = AnalysisResultsMetadata.builder()
                .setLiricalVersion(pgProperties.getProperty("lirical.version"))
                .setHpoVersion(lirical.phenotypeService().hpo().getMetaInfo().getOrDefault("release", "UNKNOWN RELEASE"))
                .setTranscriptDatabase(pgProperties.getProperty("transcript.database"))
                .setLiricalPath(pgProperties.getProperty("lirical.data.path"))
                .setExomiserPath(pgProperties.getProperty("exomiser.variant.path") == null ? "" : pgProperties.getProperty("exomiser.variant.path"))
                .setAnalysisDate(getTodaysDate())
                .setSampleName(analysisData.sampleId())
                .setnGoodQualityVariants(filteringStats.nGoodQualityVariants())
                .setnFilteredVariants(filteringStats.nFilteredVariants())
                .setGenesWithVar(0) // TODO
                .setGlobalMode(false)
                .build();

        lirical.analysisResultsWriterFactory()
                .getWriter(analysisData, results, metadata)
                .process(outputOptions);
    }

    protected AnalysisData prepareAnalysisData(Lirical lirical, String phenopacketFile, String vcfFile) throws Exception {
        Path phenopacketPath = Path.of(phenopacketFile);
        logger.info("Reading phenopacket from {}.", phenopacketPath.toAbsolutePath());

        PhenopacketData data = null;
        try (InputStream is = Files.newInputStream(phenopacketPath)) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            logger.info("Success!");
        } catch (Exception e) {
            logger.info("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = Files.newInputStream(phenopacketPath)) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
            } catch (IOException e) {
                logger.info("Unable to parse as v1 phenopacket.");
                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
            }
        }

        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
        List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();

        // Read VCF file.
        GenesAndGenotypes genes = GenesAndGenotypes.empty();
//        Path vcfPath = data.getVcfPath().orElse(null);
        Path vcfPath = null;
        if (new File(vcfFile).isFile()) {
            vcfPath = Path.of(vcfFile);
        }
        String sampleId = data.getSampleId();
        if (vcfPath != null) {
            genes = readVariantsFromVcfFile(sampleId, vcfPath, lirical.variantParserFactory());
        }
//            System.out.println(String.join(", ", sampleId, data.getAge().orElse(null).toString(), data.getSex().orElse(null).toString(),
//                    presentTerms.toString(), excludedTerms.toString(), genes.toString()));
        return AnalysisData.of(sampleId, data.getAge().orElse(null), data.getSex().orElse(null), presentTerms, excludedTerms, genes);
    }

    protected GenesAndGenotypes readVariantsFromVcfFile(String sampleId,
                                                               Path vcfPath,
                                                               VariantParserFactory parserFactory) throws LiricalParseException {
        if (parserFactory == null) {
            logger.warn("Cannot process the provided VCF file {}, resources are not set.", vcfPath.toAbsolutePath());
            return GenesAndGenotypes.empty();
        }
        try (VariantParser variantParser = parserFactory.forPath(vcfPath)) {
            // Ensure the VCF file contains the sample
//            if (!variantParser.sampleNames().contains(sampleId))
//                throw new LiricalParseException("The sample " + sampleId + " is not present in VCF at '" + vcfPath.toAbsolutePath() + '\'');
            logger.debug("Found sample {} in the VCF file at {}", sampleId, vcfPath.toAbsolutePath());

            // Read variants
            logger.info("Reading variants from {}", vcfPath.toAbsolutePath());
            AtomicInteger counter = new AtomicInteger();
            List<LiricalVariant> variants = variantParser.variantStream()
                    .peek(logProgress(counter))
                    .toList();
            logger.info("Read {} variants", variants.size());

            // Group variants by Entrez ID.
            Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
            for (LiricalVariant variant : variants) {
                variant.annotations().stream()
                        .map(TranscriptAnnotation::getGeneId)
                        .distinct()
                        .forEach(geneId -> gene2Genotype.computeIfAbsent(geneId, e -> new LinkedList<>()).add(variant));
            }

            // Collect the variants into Gene2Genotype container
            List<Gene2Genotype> g2g = gene2Genotype.entrySet().stream()
                    .map(e -> Gene2Genotype.of(e.getKey(), e.getValue()))
                    .toList();

            return GenesAndGenotypes.of(g2g);
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }
    }

    private static Consumer<LiricalVariant> logProgress(AtomicInteger counter) {
        return v -> {
            int current = counter.incrementAndGet();
            if (current % 5000 == 0)
                logger.info("Read {} variants", current);
        };
    }

    /**
     * @return a string with today's date in the format yyyy/MM/dd.
     */
    private static String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return dateFormat.format(date);
    }
}

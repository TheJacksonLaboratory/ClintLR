package org.monarchinitiative.clintlr.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.clintlr.gui.exception.ClintLRException;
import org.monarchinitiative.clintlr.gui.resources.LiricalResources;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImportException;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A task for preparing {@link AnalysisData} and running LIRICAL analysis. The task returns {@link Path} to the
 * HTML report with results.
 */
public class LiricalRunTask extends Task<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalRunTask.class);
    private static final String UNKNOWN_VERSION_PLACEHOLDER = "UNKNOWN VERSION";

    private final Lirical lirical;
    private final LiricalResources liricalResources;
    private final Path phenopacketPath;
    private final Path vcfPath;  // nullable
    private final AnalysisOptions analysisOptions;
    private final OutputOptions outputOptions;

    public LiricalRunTask(Lirical lirical,
                          LiricalResources liricalResources,
                          Path phenopacketPath,
                          Path vcfPath,
                          AnalysisOptions analysisOptions,
                          OutputOptions outputOptions) {
        this.lirical = Objects.requireNonNull(lirical);
        this.liricalResources = liricalResources;
        this.phenopacketPath = Objects.requireNonNull(phenopacketPath);
        this.vcfPath = vcfPath; // nullable
        this.analysisOptions = Objects.requireNonNull(analysisOptions);
        this.outputOptions = Objects.requireNonNull(outputOptions);
    }

    @Override
    protected Path call() throws Exception {
        // Read phenotypic features.
        PhenopacketData phenopacketData = readPhenopacketData(phenopacketPath);

        // Read variants if present.
        GenesAndGenotypes gene2Genotypes = readVariants(vcfPath);

        // Assemble the analysis data.
        AnalysisData analysisData = AnalysisData.of(phenopacketData.sampleId(),
                phenopacketData.parseAge().orElse(null),
                phenopacketData.parseSex().orElse(Sex.UNKNOWN),
                phenopacketData.presentHpoTermIds().toList(),
                phenopacketData.excludedHpoTermIds().toList(),
                gene2Genotypes);

        // Run the analysis.
        AnalysisResults results = lirical.analysisRunner().run(analysisData, analysisOptions);

        // Write out the results into HTML file.
        FilteringStats filteringStats = gene2Genotypes.computeFilteringStats();
        AnalysisResultsMetadata metadata = AnalysisResultsMetadata.builder()
                .setLiricalVersion(lirical.version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                .setHpoVersion(lirical.phenotypeService().hpo().version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                .setTranscriptDatabase(analysisOptions.transcriptDatabase().toString())
                .setLiricalPath(liricalResources.getDataDirectory().toAbsolutePath().toString())
                .setExomiserPath(getExomiserPathForGenomeBuild(liricalResources, analysisOptions.genomeBuild()))
                .setAnalysisDate(getTodaysDate())
                .setSampleName(analysisData.sampleId())
                .setnPassingVariants(filteringStats.nPassingVariants())
                .setnFilteredVariants(filteringStats.nFilteredVariants())
                .setGenesWithVar(filteringStats.genesWithVariants())
                .setGlobalMode(analysisOptions.useGlobal())
                .build();

        Optional<AnalysisResultsWriter> writer = lirical.analysisResultsWriterFactory()
                .getWriter(OutputFormat.HTML);
        if (writer.isPresent()) {
            writer.get()
                    .process(analysisData, results, metadata, outputOptions);
            // Finally, return path where the resulting HTML should land at.
            return outputOptions.outputDirectory().resolve(outputOptions.prefix() + ".html");
        } else
            throw new ClintLRException("Cannot to write the analysis results in HTML format");
    }

    private GenesAndGenotypes readVariants(Path vcfPath) throws Exception {
        if (vcfPath != null && Files.isRegularFile(vcfPath)) {
            Optional<VariantParser> variantParser = lirical.variantParserFactory()
                    .forPath(vcfPath, analysisOptions.genomeBuild(), analysisOptions.transcriptDatabase());
            if (variantParser.isPresent()) {
                try (VariantParser parser = variantParser.get()) {
                    Collection<String> sampleNames = parser.sampleNames();
                    return GenesAndGenotypes.fromVariants(sampleNames, parser);
                }
            }
        }
        return GenesAndGenotypes.empty();
    }

    private static PhenopacketData readPhenopacketData(Path phenopacketPath) throws Exception {
        LOGGER.debug("Reading phenopacket data at {}", phenopacketPath.toAbsolutePath());
        try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
            return PhenopacketImporters.v2().read(is);
        } catch (PhenopacketImportException e) {
            LOGGER.debug("Failed.");
            // swallow
        }

        LOGGER.debug("Trying to decode phenopacket at {} as legacy v1 format", phenopacketPath.toAbsolutePath());
        try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
            return PhenopacketImporters.v1().read(is);
        }
    }

    /**
     * @return a string with today's date in the format yyyy/MM/dd.
     */
    private static String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    private static String getExomiserPathForGenomeBuild(LiricalResources liricalResources, GenomeBuild genomeBuild) {
        Path exomiserFile = switch (genomeBuild) {
            case HG19 -> liricalResources.getExomiserHg19VariantDbFile();
            case HG38 -> liricalResources.getExomiserHg38VariantDbFile();
        };
        return exomiserFile == null
                ? "Unset"
                : exomiserFile.toAbsolutePath().toString();
    }

}

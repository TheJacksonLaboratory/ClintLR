package org.monarchinitiative.l2ci.gui.tasks;

import com.google.protobuf.Message;
import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.gui.exception.L4CIException;
import org.monarchinitiative.l2ci.gui.resources.LiricalResources;
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
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.phenopackettools.core.PhenopacketSchemaVersion;
import org.phenopackets.phenopackettools.io.PhenopacketParser;
import org.phenopackets.phenopackettools.io.PhenopacketParserFactory;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.Individual;
import org.phenopackets.schema.v2.core.PhenotypicFeature;
import org.phenopackets.schema.v2.core.TimeElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Period;
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
                phenopacketData.age,
                phenopacketData.sex,
                phenopacketData.present,
                phenopacketData.excluded,
                gene2Genotypes);

        // Run the analysis.
        LiricalAnalysisRunner runner = lirical.analysisRunner();
        AnalysisResults results = runner.run(analysisData, analysisOptions);

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
                .setnGoodQualityVariants(filteringStats.nGoodQualityVariants())
                .setnFilteredVariants(filteringStats.nFilteredVariants())
                .setGenesWithVar(0) // TODO
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
            throw new L4CIException("Cannot to write the analysis results in HTML format");
    }

    private GenesAndGenotypes readVariants(Path vcfPath) throws Exception {
        if (vcfPath != null && Files.isRegularFile(vcfPath)) {
            Optional<VariantParser> variantParser = lirical.variantParserFactory()
                    .forPath(vcfPath, analysisOptions.genomeBuild(), analysisOptions.transcriptDatabase());
            if (variantParser.isPresent()) {
                List<LiricalVariant> variants;
                try (VariantParser parser = variantParser.get()) {
                    variants = parser.variantStream()
                            .toList();
                }
                return prepareGenesAndGenotypes(variants);
            }
        }
        return GenesAndGenotypes.empty();
    }

    private static PhenopacketData readPhenopacketData(Path phenopacketPath) throws Exception {
        PhenopacketParserFactory parserFactory = PhenopacketParserFactory.getInstance();
        PhenopacketParser phenopacketParser = parserFactory.forFormat(PhenopacketSchemaVersion.V2);
        Message message = phenopacketParser.parse(phenopacketPath);
        String sampleId;
        List<TermId> present = new ArrayList<>();
        List<TermId> excluded = new ArrayList<>();
        if (message instanceof Phenopacket pp) {
            Individual subject = pp.getSubject();
            sampleId = subject.getId();
            Age age = parseAge(subject);
            for (PhenotypicFeature pf : pp.getPhenotypicFeaturesList()) {
                try {
                    TermId termId = TermId.of(pf.getType().getId());
                    if (pf.getExcluded())
                        excluded.add(termId);
                    else
                        present.add(termId);
                } catch (PhenolRuntimeException e) {
                    LOGGER.warn("Ignoring invalid phenotypic feature {}", pf.getType().getId());
                }
            }
            Sex sex = parseSex(subject.getSex());
            return new PhenopacketData(sampleId, age, sex, present, excluded);
        }
        throw new L4CIException("Phenopacket %s is not in V2 format".formatted(phenopacketPath.toAbsolutePath()));
    }

    private static Age parseAge(Individual subject) {
        TimeElement timeAtLastEncounter = subject.getTimeAtLastEncounter();
        return switch (timeAtLastEncounter.getElementCase()) {
            case GESTATIONAL_AGE -> {
                // TODO - revisit once we support gestational age in LIRICAL.
                LOGGER.warn("Ignoring gestational age for subject {}", subject.getId());
                yield Age.ageNotKnown();
            }
            case AGE -> {
                org.phenopackets.schema.v2.core.Age a = timeAtLastEncounter.getAge();
                LOGGER.debug("Parsing age {} of subject {}", a.getIso8601Duration(), subject.getId());
                yield Age.parse(Period.parse(a.getIso8601Duration()));
            }
            case AGE_RANGE, ONTOLOGY_CLASS, TIMESTAMP, INTERVAL -> {
                LOGGER.warn("Ignoring unsupported age format {} for subject {}", timeAtLastEncounter.getElementCase(), subject.getId());
                yield Age.ageNotKnown();
            }
            case ELEMENT_NOT_SET ->  {
                LOGGER.warn("Time at last encounter was not set for subject {}", subject.getId());
                yield Age.ageNotKnown();
            }
        };
    }

    private static Sex parseSex(org.phenopackets.schema.v2.core.Sex sex) {
        return switch (sex) {
            case FEMALE -> Sex.FEMALE;
            case MALE -> Sex.MALE;
            case UNKNOWN_SEX, OTHER_SEX, UNRECOGNIZED -> Sex.UNKNOWN;
        };
    }

    private static GenesAndGenotypes prepareGenesAndGenotypes(List<LiricalVariant> variants) {
        // TODO(mabeckwith) - something similar is in LiricalAnalysis. Please decide where the appropriate place for the code is.
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

    private record PhenopacketData(String sampleId, Age age, Sex sex, List<TermId> present, List<TermId> excluded) {}
}

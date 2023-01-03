package org.monarchinitiative.l2ci.gui.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;


public class OptionalResourcesTest {

    private static final double ERROR = 5E-9;
    private OptionalResources instance;

    @BeforeEach
    public void setUp() {
        instance = new OptionalResources();
    }

    @Test
    public void initResources() throws Exception {
        String payload = """
                #L4CI properties
                #Thu Dec 31 23:59:59 EST 2022
                mondo.json.path=/path/to/mondo.json
                lirical.data.directory=/path/to/lirical/datadir
                lirical.exomiser.hg19.variant.file=/path/to/exomiser.hg19.mv.db
                lirical.exomiser.hg38.variant.file=/path/to/exomiser.hg38.mv.db
                lirical.background.frequency.file=/path/to/bg.freq.txt
                lirical.pathogenicity.threshold=0.1
                lirical.default.variant.background.frequency=0.1234
                lirical.strict=true
                lirical.default.allele.frequency=1.0E-7
                lirical.transcript.database=UCSC
                lirical.genome.build=HG19
                lirical.results.directory=/path/to/lirical/results
                anything.else.should.not.crash=the/application""";
        Properties properties = new Properties();
        properties.load(new StringReader(payload));
        instance.initResources(properties);

        // Mondo
        assertThat(instance.ontologyResources().getMondoPath(), equalTo(Path.of("/path/to/mondo.json")));
        // LiricalResources
        LiricalResources liricalResources = instance.liricalResources();
        assertThat(liricalResources.getDataDirectory(), equalTo(Path.of("/path/to/lirical/datadir")));
        assertThat(liricalResources.getExomiserHg19VariantDbFile(), equalTo(Path.of("/path/to/exomiser.hg19.mv.db")));
        assertThat(liricalResources.getExomiserHg38VariantDbFile(), equalTo(Path.of("/path/to/exomiser.hg38.mv.db")));
        assertThat(liricalResources.getBackgroundVariantFrequencyFile(), equalTo(Path.of("/path/to/bg.freq.txt")));
        assertThat((double) liricalResources.getPathogenicityThreshold(), closeTo(.1, ERROR));
        assertThat(liricalResources.getDefaultVariantBackgroundFrequency(), closeTo(.1234, ERROR));
        assertThat(liricalResources.isStrict(), equalTo(true));
        assertThat((double) liricalResources.getDefaultAlleleFrequency(), closeTo(1E-7, ERROR));
        assertThat(liricalResources.getTranscriptDatabase(), equalTo(TranscriptDatabase.UCSC));
        assertThat(liricalResources.getGenomeBuild(), equalTo(GenomeBuild.HG19));

        assertThat(instance.getLiricalResults(), equalTo(Path.of("/path/to/lirical/results")));
    }

    @Test
    public void dumpResources() throws Exception {
        // Simulate setting all resources
        instance.ontologyResources().setMondoPath(Path.of("/path/to/mondo.json"));
        LiricalResources liricalResources = instance.liricalResources();
        liricalResources.setDataDirectory(Path.of("/path/to/lirical/datadir"));
        liricalResources.setExomiserHg19VariantDbFile(Path.of("/path/to/exomiser.hg19.mv.db"));
        liricalResources.setExomiserHg38VariantDbFile(Path.of("/path/to/exomiser.hg38.mv.db"));
        liricalResources.setBackgroundVariantFrequencyFile(Path.of("/path/to/bg.freq.txt"));
        liricalResources.setPathogenicityThreshold(.5f);
        liricalResources.setDefaultVariantBackgroundFrequency(.1234);
        liricalResources.setStrict(true);
        liricalResources.setDefaultAlleleFrequency(1E-5f);
        liricalResources.setTranscriptDatabase(TranscriptDatabase.UCSC);
        liricalResources.setGenomeBuild(GenomeBuild.HG19);
        instance.setLiricalResults(Path.of("/path/to/lirical/results"));

        // Dump the resources
        Properties properties = new Properties();
        instance.storeResources(properties);

        // And test..
        assertThat(properties.getProperty(OntologyResources.MONDO_JSON_PATH_PROPERTY), equalTo("/path/to/mondo.json"));
        assertThat(properties.getProperty(LiricalResources.LIRICAL_DATA_PROPERTY), equalTo("/path/to/lirical/datadir"));
        assertThat(properties.getProperty(LiricalResources.EXOMISER_HG19_VARIANT_PROPERTY), equalTo("/path/to/exomiser.hg19.mv.db"));
        assertThat(properties.getProperty(LiricalResources.EXOMISER_HG38_VARIANT_PROPERTY), equalTo("/path/to/exomiser.hg38.mv.db"));
        assertThat(properties.getProperty(LiricalResources.BACKGROUND_FREQUENCY_PROPERTY), equalTo("/path/to/bg.freq.txt"));
        assertThat(properties.getProperty(LiricalResources.PATHOGENICITY_PROPERTY), equalTo("0.5"));
        assertThat(properties.getProperty(LiricalResources.DEFAULT_VARIANT_BACKGROUND_FREQUENCY_PROPERTY), equalTo("0.1234"));
        assertThat(properties.getProperty(LiricalResources.STRICT_PROPERTY), equalTo("true"));
        assertThat(properties.getProperty(LiricalResources.DEFAULT_ALLELE_PROPERTY), equalTo("1.0E-5"));
        assertThat(properties.getProperty(LiricalResources.TRANSCRIPT_DATABASE_PROPERTY), equalTo("UCSC"));
        assertThat(properties.getProperty(LiricalResources.GENOME_BUILD_PROPERTY), equalTo("HG19"));
        assertThat(properties.getProperty(OptionalResources.LIRICAL_RESULTS_PROPERTY), equalTo("/path/to/lirical/results"));
    }
}
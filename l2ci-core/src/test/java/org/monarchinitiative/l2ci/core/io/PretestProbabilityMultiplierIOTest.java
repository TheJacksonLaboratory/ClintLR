package org.monarchinitiative.l2ci.core.io;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PretestProbabilityMultiplierIOTest {

    @Test
    public void writeAndRead() throws Exception {
        Map<TermId, Double> values = Map.of(
                TermId.of("MONDO:0000123"), 123.,
                TermId.of("MONDO:0000456"), 456.
        );
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PretestProbabilityMultiplierIO.write(values, os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        Map<TermId, Double> parsed = PretestProbabilityMultiplierIO.read(is);
        assertThat(parsed, equalTo(values));
    }
}
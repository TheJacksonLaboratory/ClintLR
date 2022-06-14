import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

public class LIRICALTest {
    @Test
    public void testRun() throws Exception {
//        Path exomiserVariants = Path.of(String.join(File.separator, "", "Users", "beckwm", "Exomiser", "2109_hg38", "2109_hg38", "2109_hg38_variants.mv.db"));
        String homeDir = new File(".").getAbsolutePath();
        Lirical lirical = LiricalBuilder.builder(Path.of(String.join(File.separator, homeDir.substring(0, homeDir.indexOf("Projects/") + 8), "LIRICAL", "data")))
//                .exomiserVariantDatabase(exomiserVariants)
                .setDiseaseDatabases(Set.of(DiseaseDatabase.OMIM))
                .build();
    }
}

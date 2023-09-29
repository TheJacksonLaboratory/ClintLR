package org.monarchinitiative.l4ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.l4ci.core.Relation;
import org.monarchinitiative.l4ci.core.io.MondoNDescendantsMapIO;
import org.monarchinitiative.l4ci.core.io.OmimMapIO;
import org.monarchinitiative.l4ci.core.mondo.CalculateOmimToMondoMap;
import org.monarchinitiative.phenol.ontology.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class MondoOmimTask extends Task<MondoOmim> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MondoOmimTask.class);

    private static final String OMIM_2_MONDO_NAME = "omim2mondo.csv";
    private static final String MONDO_N_DESC_NAME = "mondoDesc.csv";

    private final Ontology mondo;
    private final Path dataDirectory;

    public MondoOmimTask(Ontology mondo, Path dataDirectory) {
        this.mondo = Objects.requireNonNull(mondo);
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected MondoOmim call() throws Exception {
        LOGGER.debug("Loading Mondo metadata using the data directory at {}", dataDirectory.toAbsolutePath());

        // Load the OMIM to Mondo from the cache file if present, or compute and cache the map.
        Path omimToMondoPath = dataDirectory.resolve(OMIM_2_MONDO_NAME);
        Map<TermId, TermId> omim2Mondo;
        if (Files.exists(omimToMondoPath)) {
            omim2Mondo = OmimMapIO.read(omimToMondoPath);
        } else {
            omim2Mondo = CalculateOmimToMondoMap.calculateOmimMap(mondo);
            OmimMapIO.write(omim2Mondo, omimToMondoPath);
        }

        // Flip the OMIM to Mondo map
        Map<TermId, TermId> mondoToOmim = new HashMap<>();
        for (Map.Entry<TermId, TermId> e : omim2Mondo.entrySet()) {
//            for (TermId mondoId : e.getValue()) {
                mondoToOmim.put(e.getValue(), e.getKey());
//            }
        }

        // Same as above, load from cache or calculate and cache the map.
        Path mondoNDescendantsPath = dataDirectory.resolve(MONDO_N_DESC_NAME);
        Map<TermId, Integer> mondoNDescendents;
        if (Files.exists(mondoNDescendantsPath)) {
            mondoNDescendents = MondoNDescendantsMapIO.read(mondoNDescendantsPath);
        } else {
            mondoNDescendents = makeMondoDescendantsMaps(mondo);
            MondoNDescendantsMapIO.write(mondoNDescendents, mondoNDescendantsPath);
        }

        return new MondoOmim(omim2Mondo, Map.copyOf(mondoToOmim), mondoNDescendents);
    }


    private static Map<TermId, Integer> makeMondoDescendantsMaps(Ontology mondo) {
        Map<TermId, Integer> builder = new HashMap<>();
        for (Term mondoTerm : mondo.getTerms()) {
            long nDescendents = Relation.getTermRelations(mondo, mondoTerm.id(), Relation.DESCENDENT).stream()
                    .flatMap(d -> d.getXrefs().stream())
                    .filter(r -> r.getName().startsWith("OMIM"))
                    .count();
            builder.put(mondoTerm.id(), Math.toIntExact(nDescendents));
        }

        return Map.copyOf(builder);
    }

}

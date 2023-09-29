package org.monarchinitiative.clintlr.core.mondo;

import org.monarchinitiative.phenol.ontology.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CalculateOmimToMondoMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalculateOmimToMondoMap.class);

    public static Map<TermId, TermId> calculateOmimMap(Ontology mondo) {
        Map<TermId, TermId> builder = new HashMap<>();
        for (Term mondoTerm : mondo.getTerms()) {
            for (TermSynonym tsyn : mondoTerm.getSynonyms()) {
                if (tsyn.getTermXrefs().stream().map(TermXref::id).map(TermId::getPrefix).anyMatch(p -> p.equals("OMIMPS"))) {
                    break;
                }
                if (TermSynonymScope.EXACT == tsyn.getScope()) {
                    Optional<TermId> opt = tsyn.getTermXrefs().stream()
                            .map(TermXref::id)
                            .filter(id -> id.getPrefix().equals("OMIM"))
                            .findAny();
                    if (opt.isPresent()) {
                        TermId omimId = opt.get();
                        if (builder.containsKey(omimId) && !builder.get(omimId).equals(mondoTerm.id())) {
                            LOGGER.error("Multiple Mondo IDs associated with OMIM ID " + omimId.toString());
                            LOGGER.info(String.join(" ", omimId.toString(), builder.get(omimId).toString(), mondoTerm.id().toString()));
                        }
                        builder.put(omimId, mondoTerm.id());
                    }
                }
            }
        }
        long omimPScount = builder.keySet().stream().map(TermId::getPrefix).filter(p -> p.equals("OMIMPS")).count();
        if (omimPScount > 0) {
            System.err.println(omimPScount);
            System.exit(1);
        }
        return Map.copyOf(builder);
    }

}

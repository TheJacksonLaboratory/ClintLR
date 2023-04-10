package org.monarchinitiative.l4ci.core.mondo;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * Class to get Phenotypic Series from Mondo
 */
public class PhenotypicSeries {

    private static final Logger logger = LoggerFactory.getLogger(PhenotypicSeries.class);
    private Ontology mondo;
    private Set<TermId> omimPSTerms = new HashSet<>();
    private Set<TermId> hrmdTerms = new HashSet<>();

    /**
     * Get OMIM Phenotypic Series terms from Mondo.
     *
     * @param mondo The Mondo ontology.
     * @return List of Mondo Terms that have OMIM Phenotypic Series cross-references.
     */
    public PhenotypicSeries(Ontology mondo) {
        if (mondo == null) {
            logger.error("Mondo ontology is null");
        } else {
            this.mondo = mondo;
            mondo.getTerms().forEach(t -> t.getXrefs().stream().filter(r -> r.getName().contains("OMIMPS")).map(r -> t.id()).forEach(omimPSTerms::add));
        }
    }

    public Set<TermId> getHrmdTerms(String hrmdPath) {
        Collection<Term> mondoTerms = mondo.getTerms();
        File hrmdDir = new File(hrmdPath);
        for (File f : hrmdDir.listFiles()) {
            try {
                String s = Files.readString(f.toPath());
                Pattern p = Pattern.compile(".*database.*:\\s+(.*),\\n.*diseaseId.*:\\s+(.*),");
                Matcher m = p.matcher(s);
                if (m.find()) {
                    String dbTerm = m.group(1).replace("\"", "") + ":" + m.group(2).replace("\"", "");
                    mondoTerms.forEach(t -> t.getXrefs().stream().filter(r -> r.getName().contains(dbTerm)).map(r -> t.id()).forEach(hrmdTerms::add));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hrmdTerms;
    }

    public Set<TermId> getTermChildren(Term term) {
        return getChildTerms(mondo, term.id(), false);
    }

    public Set<TermId> getTermParents(Term term) {
        return getParentTerms(mondo, term.id(), false);
    }

    public Set<TermId> getChildrenOfTerms(Set<TermId> termIds) {
        return getChildTerms(mondo, termIds);
    }

    public Set<TermId> getParentsOfTerms(Set<TermId> termIds) {
        return getParentTerms(mondo, termIds);
    }

    private void dumpPhenotypicSeries() {
        System.out.println("Number of OMIMPS terms = " + omimPSTerms.size());
        System.out.println("Mondo OMIMPS terms: " + omimPSTerms);
        System.out.println("Parent Terms: " + getParentsOfTerms(omimPSTerms));
        System.out.println("Child Terms: " + getChildrenOfTerms(omimPSTerms));
        System.out.println("HRMD: " + getHrmdTerms("/Users/beckwm/IdeaProjects/hrmd/data/casereports"));
        System.out.println("Number of hrmd terms = " + hrmdTerms.size());
    }

    public void run() {
        dumpPhenotypicSeries();
    }
}

package org.monarchinitiative.l2ci.core.mondo;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

public class MondoStats {

    private final Ontology mondo;
    private int nTerms;
    private int nAlternateTermIDs;
    private int nNonObsoleteTerms;
    private int nRelations;
    private int nDefinitions;
    private int nSynonyms;
    private Map<String,String> metaInfo;

    public MondoStats(Ontology mondo) {
        this.mondo = mondo;
        calculateCounts();
    }

    public int getNTerms() {
        return nTerms;
    }

    public int getNAlternateTermIDs() {
        return nAlternateTermIDs;
    }

    public int getNNonObsoleteTerms() {
        return nNonObsoleteTerms;
    }

    public int getNRelations() {
        return nRelations;
    }

    public Map<String, String> getMetaInfo() {
        return metaInfo;
    }

    public int getNDefinitions() {
        return nDefinitions;
    }

    public int getNSynonyms() {
        return nSynonyms;
    }

    private void calculateCounts() {
        nTerms = mondo.countAllTerms();
        nNonObsoleteTerms = mondo.countNonObsoleteTerms();
        nAlternateTermIDs = mondo.countAlternateTermIds();
        nRelations = mondo.getRelationMap().size();
        metaInfo = mondo.getMetaInfo();
        nDefinitions = 0;
        nSynonyms = 0;
        for (TermId tid : mondo.getTermMap().keySet()) {
            Term term = mondo.getTermMap().get(tid);
            if (term.getDefinition() != null && term.getDefinition().length() > 0) nDefinitions++;
            if (term.getSynonyms() != null)
                nSynonyms += term.getSynonyms().size();

        }
    }

    private void dumpMondoStats() {
        System.out.println("Meta Info: " + getMetaInfo());
        System.out.println("Term Count: " + getNTerms());
        System.out.println("Alternate TermID Count: " + getNAlternateTermIDs());
        System.out.println("Non-Obsolete Term Count: " + getNNonObsoleteTerms());
        System.out.println("Relations: " + getNRelations());
        System.out.println("Definitions: " + getNDefinitions());
        System.out.println("Synonyms: " + getNSynonyms());
    }

    public void run() {
        dumpMondoStats();
    }
}

package org.monarchinitiative.l2ci.core;


/**
 * @deprecated the enum is redundant since L4CI only directly works with Mondo.
 */
@Deprecated(forRemoval = true)
public enum OntologyType {
    HPO("hpo"),
    HPOA("hpoa"),
    MONDO("mondo");

    private final String name;

    OntologyType(String n) {
        this.name = n;
    }

    @Override
    public String toString() {
        return this.name;
    }

}

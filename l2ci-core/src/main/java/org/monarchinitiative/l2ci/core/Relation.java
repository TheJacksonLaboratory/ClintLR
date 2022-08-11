package org.monarchinitiative.l2ci.core;

public enum Relation {
    ANCESTOR("ancestor"),
    CHILD("child"),
    DESCENDENT("descendent"),
    PARENT("parent");

    private final String name;

    Relation(String n) {
        this.name = n;
    }

    @Override
    public String toString() {
        return this.name;
    }
}

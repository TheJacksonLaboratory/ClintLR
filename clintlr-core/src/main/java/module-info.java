module clintlr.core {
    requires transitive lirical.core;
    requires lirical.io;
    requires org.monarchinitiative.phenol.io;
    requires org.monarchinitiative.phenol.annotations; // TODO - consider removing
    requires org.monarchinitiative.biodownload;

    requires org.slf4j;
    requires commons.csv;

    exports org.monarchinitiative.clintlr.core;
    exports org.monarchinitiative.clintlr.core.io;
    exports org.monarchinitiative.clintlr.core.mondo;
    exports org.monarchinitiative.clintlr.core.pretestprob;
}
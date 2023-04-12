module l4ci.core {
    requires transitive lirical.core;
    requires lirical.io;
    requires org.monarchinitiative.phenol.io;
    requires org.monarchinitiative.phenol.annotations; // TODO - consider removing
    requires org.monarchinitiative.biodownload;

    requires org.slf4j;
    requires commons.csv;

    exports org.monarchinitiative.l4ci.core;
    exports org.monarchinitiative.l4ci.core.io;
    exports org.monarchinitiative.l4ci.core.mondo;
    exports org.monarchinitiative.l4ci.core.pretestprob;
}
module l2ci.core {
    requires transitive lirical.core;
    requires lirical.io;
    requires org.monarchinitiative.phenol.io;
    requires org.monarchinitiative.phenol.annotations;
    requires org.monarchinitiative.biodownload;

    requires org.slf4j;
    requires commons.csv;

    exports org.monarchinitiative.l2ci.core;
    exports org.monarchinitiative.l2ci.core.io;
    exports org.monarchinitiative.l2ci.core.mondo;
    exports org.monarchinitiative.l2ci.core.pretestprob;
}
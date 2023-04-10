module l4ci.gui {
    requires l4ci.core;
    requires lirical.io;
    requires lirical.configuration;
    requires org.monarchinitiative.phenol.io;
    requires org.monarchinitiative.phenol.annotations;
    requires org.monarchinitiative.biodownload;

    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.context;
    requires spring.core;

    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.slf4j;

    exports org.monarchinitiative.l4ci.gui to javafx.graphics;
    exports org.monarchinitiative.l4ci.gui.controller to javafx.fxml, spring.beans;
    exports org.monarchinitiative.l4ci.gui.resources to spring.beans;
    exports org.monarchinitiative.l4ci.gui.tasks to javafx.graphics;
    exports org.monarchinitiative.l4ci.gui.ui to javafx.fxml;


    opens org.monarchinitiative.l4ci.gui;
    opens org.monarchinitiative.l4ci.gui.config to spring.beans, spring.boot, spring.context, spring.core;
    opens org.monarchinitiative.l4ci.gui.controller to javafx.fxml;
    opens org.monarchinitiative.l4ci.gui.tasks to spring.beans;
    opens org.monarchinitiative.l4ci.gui.ui.mondotree to javafx.fxml;
    opens org.monarchinitiative.l4ci.gui.ui.summary to javafx.fxml;
}
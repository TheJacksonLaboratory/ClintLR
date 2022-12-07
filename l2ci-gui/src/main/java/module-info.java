module l2ci.gui {
    requires l2ci.core;
    requires lirical.configuration;
    requires org.monarchinitiative.phenol.io;
    requires org.monarchinitiative.phenol.annotations;

    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.context;

    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires commons.csv;
    requires org.slf4j;
    requires spring.core;
    requires org.monarchinitiative.biodownload;

    exports org.monarchinitiative.l2ci.gui to javafx.graphics;
    exports org.monarchinitiative.l2ci.gui.controller to javafx.fxml, spring.beans;
//    exports org.monarchinitiative.l2ci.gui.controller;
    exports org.monarchinitiative.l2ci.gui.resources to spring.beans;
    exports org.monarchinitiative.l2ci.gui.tasks to javafx.graphics;
    exports org.monarchinitiative.l2ci.gui.ui to javafx.fxml;


    opens org.monarchinitiative.l2ci.gui;
    opens org.monarchinitiative.l2ci.gui.config;
    opens org.monarchinitiative.l2ci.gui.controller to javafx.fxml;
    opens org.monarchinitiative.l2ci.gui.tasks;
}
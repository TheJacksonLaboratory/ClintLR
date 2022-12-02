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

    requires commons.lang;
    requires org.slf4j;
    requires spring.core;
    requires org.monarchinitiative.biodownload;

    exports org.monarchinitiative.l2ci.gui to javafx.graphics;
//    exports org.monarchinitiative.l2ci.gui.controller to javafx.fxml;
    exports org.monarchinitiative.l2ci.gui.controller;

    opens org.monarchinitiative.l2ci.gui;
    opens org.monarchinitiative.l2ci.gui.config;
    opens org.monarchinitiative.l2ci.gui.controller;
}
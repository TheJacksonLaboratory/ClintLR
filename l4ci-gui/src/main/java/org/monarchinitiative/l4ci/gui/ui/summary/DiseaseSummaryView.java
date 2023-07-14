package org.monarchinitiative.l4ci.gui.ui.summary;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Term;

import java.io.IOException;
import java.util.List;

public class DiseaseSummaryView extends VBox {

    private static final String HTML_VIEW_PLACEHOLDER = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>HPO tree browser</title></head>" +
            "<body><p>Click on Mondo term in the tree browser to display additional information</p></body></html>";

    private final ObjectProperty<L4ciDiseaseSummary> data = new SimpleObjectProperty<>();

    @FXML
    private WebView infoWebView;
    private WebEngine infoWebEngine;

    public DiseaseSummaryView() {
        FXMLLoader loader = new FXMLLoader(DiseaseSummaryView.class.getResource("DiseaseSummaryView.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        infoWebEngine = infoWebView.getEngine();
        infoWebEngine.loadContent(HTML_VIEW_PLACEHOLDER);
        data.addListener((obs, old, novel) -> updateDescription(novel));
    }

    public ObjectProperty<L4ciDiseaseSummary> dataProperty() {
        return data;
    }

    /**
     * Update content of the {@link #infoWebView} with currently selected {@link Term}.
     *
     * @param diseaseSummary currently selected {@link TreeItem} containing {@link Term}
     */
    private void updateDescription(L4ciDiseaseSummary diseaseSummary) {
        if (diseaseSummary == null) {
            infoWebEngine.loadContent(HTML_VIEW_PLACEHOLDER);
            return;
        }
        String content = L4ciHtmlPageGenerator.getHTML(this.data.get());
        infoWebEngine.loadContent(content);
    }
}

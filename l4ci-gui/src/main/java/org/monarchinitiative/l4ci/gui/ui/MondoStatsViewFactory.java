package org.monarchinitiative.l4ci.gui.ui;

import javafx.stage.Stage;
import org.monarchinitiative.l4ci.core.mondo.MondoStats;

public class MondoStatsViewFactory extends WebViewerPopup {

    private final String TITLE = "Mondo stats";

    private final String data_version;
    private final String release;

    private final int termCount;
    private final int alternativeTermIdCount;

    private final int nonObsoleteTermIdCount;

    private final int relationCount;

    private final int definitionCount;

    private final int synonymCount;

    public MondoStatsViewFactory(Stage stage, MondoStats mondo) {
        super(stage);
        var metainfo = mondo.getMetaInfo();
        data_version = metainfo.getOrDefault("data-version", "n/a");
        release = metainfo.getOrDefault("release", "n/a");
        this.termCount = mondo.getNTerms();
        this.alternativeTermIdCount = mondo.getNAlternateTermIDs();
        this.nonObsoleteTermIdCount = mondo.getNNonObsoleteTerms();
        this.relationCount = mondo.getNRelations();
        this.definitionCount = mondo.getNRelations();
        this.synonymCount = mondo.getNSynonyms();
    }

    @Override
    public void popup() {
        String html = String.format("<html><head>%s</head>\n" +
                        "<body><h2>Mondo stats</h2>" +
                        "<p><ul>" +
                        "<li>data-version: %s</li>" +
                        "<li>release: %s</li>" +
                        "<li>Terms (n): %d</li>" +
                        "<li>Alternative term ids (n): %d</li>" +
                        "<li>Non-obsolete terms (n): %d</li>" +
                        "<li>Relations (n): %d</li>" +
                        "<li>Definitions (n): %d</li>" +
                        "<li>Synonyms (n): %d</li>" +
                        "</ul></p>" +
                        "</body></html>",
                inlineCSS(),
                data_version,
                release,
                this.termCount,
                this.alternativeTermIdCount,
                this.nonObsoleteTermIdCount,
                this.relationCount,
                this.definitionCount,
                this.synonymCount);
        showHtmlContent(TITLE, html);
    }
}

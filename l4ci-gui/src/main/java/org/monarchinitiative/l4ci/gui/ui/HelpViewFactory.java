package org.monarchinitiative.l4ci.gui.ui;



import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A helper class that displays the Read-the-docs documentation for L4CI in a JavaFX webview browser.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.4 (2018-03-04)
 */
public class HelpViewFactory {
    private static final Logger logger = LoggerFactory.getLogger(HelpViewFactory.class);

    private static final String READTHEDOCS_SITE = "https://thejacksonlaboratory.github.io/L4CI/";


    /**
     * Open a JavaFW Webview window and confirmDialog our read the docs help documentation in it.
     */
    private static void openBrowser() {
        try{
            Stage window;
            window = new Stage();
            WebView web = new WebView();
            WebEngine engine=web.getEngine();
            engine.load(READTHEDOCS_SITE);
            engine.getLoadWorker().stateProperty().addListener((observable, oldvalue,newvalue)->{
                if (newvalue== Worker.State.FAILED) {
                    engine.loadContent(getHTMLError());
                }
            });
            Scene scene = new Scene(web);
            window.setScene(scene);
            window.showAndWait();
        } catch (Exception e){
            logger.error(String.format("Could not open browser to show RTD: %s", e));
            e.printStackTrace();
        }
    }

    private static String getHTMLError() {
        return  "<html><body>\n" +
                inlineCSS() +
                "<h1>PhenoteFX: Connection error</h1>" +
                "<p><Unable to conect to the internet.</p>" +
                "</body></html>";
    }

    private static String inlineCSS() {
        return "<head><style>\n" +
                "  html { margin: 0; padding: 0; }" +
                "body { font: 75% georgia, sans-serif; line-height: 1.88889;color: #001f3f; margin: 10; padding: 10; }"+
                "p { margin-top: 0;text-align: justify;}"+
                "h2,h3 {font-family: 'serif';font-size: 1.4em;font-style: normal;font-weight: bold;"+
                "letter-spacing: 1px; margin-bottom: 0; color: #001f3f;}"+
                "  </style></head>";
    }


    /** Open a dialog that provides concise help for using PhenoteFX. */
    public static void openHelpDialog() {
        openBrowser();
    }

}

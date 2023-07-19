package org.monarchinitiative.l4ci.gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.*;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PopUps {

    /*
     * See this http://code.makery.ch/blog/javafx-dialogs-official/ to get a bit of inspiration
     */


    /**
     * Show information to user.
     *
     * @param text        - message text
     * @param windowTitle - Title of PopUp window
     */
    public static void showInfoMessage(String text, String windowTitle) {
        Alert al = new Alert(AlertType.INFORMATION);
        DialogPane dialogPane = al.getDialogPane();
        dialogPane.getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label) node).setMinHeight(Region.USE_PREF_SIZE));

      /*
         Todo -- not finding css file.
        ClassLoader classLoader = PopUps.class.getClassLoader();
        dialogPane.getStylesheets().add(classLoader.getResource("popup.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        */
        al.setTitle(windowTitle);
        al.setHeaderText(null);
        al.setContentText(text);
        al.showAndWait();
    }


    public static void showException(String windowTitle, String header, Throwable exception) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(windowTitle);
        alert.setHeaderText(header);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }


    public static Stage setupProgressDialog(String title, String labl, ProgressIndicator pb) {

        javafx.scene.control.Label label = new javafx.scene.control.Label(labl);
        FlowPane root = new FlowPane();
        root.setPadding(new Insets(10));
        root.setHgap(10);
        root.getChildren().addAll(label, pb);
        Scene scene = new Scene(root, 400, 100);
        Stage window = new Stage();
        window.setTitle(title);
        window.setScene(scene);
        return window;
    }
}




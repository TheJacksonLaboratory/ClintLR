package org.monarchinitiative.clintlr.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import org.monarchinitiative.clintlr.gui.PopUps;
import org.monarchinitiative.clintlr.gui.config.AppProperties;
import org.monarchinitiative.clintlr.gui.resources.LiricalResources;
import org.monarchinitiative.clintlr.gui.resources.OntologyResources;
import org.monarchinitiative.clintlr.gui.resources.OptionalResources;
import org.monarchinitiative.clintlr.gui.tasks.DownloadLiricalData;
import org.monarchinitiative.clintlr.gui.tasks.DownloadMondoTask;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * This class is the controller of the Set Resources dialog. The user performs initial setup of the
 * resources that are required to run the GUI.
 * The resource paths are set to {@link OptionalResources} observable properties.
 * The rest of the GUI should watch the observable properties and update accordingly.
 */
public class ResourcesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesController.class);

    private final OptionalResources optionalResources;
    private final AppProperties appProperties;
    private final Path dataDirectory;
    private final ExecutorService executorService;

    @FXML
    private VBox contentBox;
    @FXML
    private Label mondoLabel;

    @FXML
    private Label liricalDataDirLabel;

    @FXML
    private Label liricalResultsDirLabel;

    @FXML
    private Label exomiserHg19FileLabel;
    @FXML
    private Label exomiserHg38FileLabel;

    @FXML
    private Label bkgFreqFileLabel;

    @FXML
    private ChoiceBox<GenomeBuild> genomeBuildChoiceBox;

    @FXML
    private ChoiceBox<TranscriptDatabase> transcriptDBChoiceBox;

    @FXML
    private TextField alleleFreqTextField;
    private final TextFormatter<Float> alleleFreqTextFormatter = new TextFormatter<>(new FloatStringConverter());

    @FXML
    private TextField pathogenicityTextField;
    private final TextFormatter<Float> pathogenicityTextFormatter = new TextFormatter<>(new FloatStringConverter());

    @FXML
    private TextField variantBkgFreqTextField;
    private final TextFormatter<Double> variantBkgFreqTextFormatter = new TextFormatter<>(new DoubleStringConverter());

    @FXML
    private CheckBox strictCheckBox;

    ResourcesController(OptionalResources optionalResources,
                        AppProperties appProperties,
                        Path dataDirectory,
                        ExecutorService executorService) {
        this.optionalResources = optionalResources;
        this.appProperties = appProperties;
        this.dataDirectory = dataDirectory;
        this.executorService = executorService;
    }


    /**
     * Initialize elements of this controller.
     */
    @FXML
    private void initialize() {
        // Mondo
        OntologyResources ontologyResources = optionalResources.ontologyResources();

        StringBinding mondoPath = preparePathBinding(ontologyResources.mondoPathProperty(), "Unset");
        mondoLabel.textProperty().bind(mondoPath);

        // Lirical resources
        LiricalResources liricalResources = optionalResources.liricalResources();
        StringBinding liricalData = preparePathBinding(liricalResources.dataDirectoryProperty(), "Unset");
        liricalDataDirLabel.textProperty().bind(liricalData);

        StringBinding exomiserHg19Db = preparePathBinding(liricalResources.exomiserHg19VariantDbFileProperty(), "Unset");
        exomiserHg19FileLabel.textProperty().bind(exomiserHg19Db);

        StringBinding exomiserHg38Db = preparePathBinding(liricalResources.exomiserHg38VariantDbFileProperty(), "Unset");
        exomiserHg38FileLabel.textProperty().bind(exomiserHg38Db);

        StringBinding bgFreq = preparePathBinding(liricalResources.backgroundVariantFrequencyFileProperty(), "Default background frequency");
        bkgFreqFileLabel.textProperty().bind(bgFreq);

        StringBinding liricalResultsDir = preparePathBinding(optionalResources.liricalResultsProperty(), "Unset");
        liricalResultsDirLabel.textProperty().bind(liricalResultsDir);

        // We persist the selected genome build and transcript into the next session.
        // However, the persistence is implemented elsewhere.
        genomeBuildChoiceBox.getItems().addAll(GenomeBuild.values());
        genomeBuildChoiceBox.valueProperty().bindBidirectional(liricalResources.genomeBuildProperty());
        transcriptDBChoiceBox.getItems().addAll(TranscriptDatabase.values());
        transcriptDBChoiceBox.valueProperty().bindBidirectional(liricalResources.transcriptDatabaseProperty());


        // TODO - remove pathogenicity threshold from ResourcesController and only have it in the main GUI?
        pathogenicityTextField.setTextFormatter(pathogenicityTextFormatter);
        pathogenicityTextFormatter.valueProperty().bindBidirectional(liricalResources.pathogenicityThresholdProperty().asObject());

        variantBkgFreqTextField.setTextFormatter(variantBkgFreqTextFormatter);
        variantBkgFreqTextFormatter.valueProperty().bindBidirectional(liricalResources.defaultVariantBackgroundFrequencyProperty().asObject());

        alleleFreqTextField.setTextFormatter(alleleFreqTextFormatter);
        alleleFreqTextFormatter.valueProperty().bindBidirectional(liricalResources.defaultAlleleFrequencyProperty().asObject());

        strictCheckBox.selectedProperty().bindBidirectional(liricalResources.strictProperty());
    }

    private static StringBinding preparePathBinding(ObjectProperty<Path> pathProperty, String placeholder) {
        return Bindings.createStringBinding(
                () -> pathProperty.get() == null
                        ? placeholder
                        : pathProperty.get().toAbsolutePath().toString(),
                pathProperty);
    }

    @FXML
    private void close() {
        this.mondoLabel.getScene().getWindow().hide();
    }

    /**
     * Open DirChooser and ask user to provide a file where the local mondo.json file is located.
     * DEPRECATING BECAUSE WE WANT USER TO JUST DOWNLOAD NOT TO SET (DO NOT PROVIDE UNNECESSARY OPTIONS)
     */
    //@FXML
    @Deprecated(forRemoval = true)
    private void setMondoFileButtonAction(Event e) {
        loadFileType(FileType.MONDO);
        e.consume();
    }

    @FXML
    private void downloadLiricalDataButtonAction(ActionEvent e) {
        // TODO - consider asking how does the user feel regarding overwriting.
        boolean overwrite = true;
        Path liricalDataDirectory = dataDirectory.resolve("lirical");
        DownloadLiricalData downloadLiricalData = new DownloadLiricalData(appProperties.liricalProperties(), liricalDataDirectory, overwrite);
            downloadLiricalData.setOnSucceeded(event -> optionalResources.liricalResources().setDataDirectory(liricalDataDirectory));
            downloadLiricalData.setOnFailed(event -> PopUps.showException(
                    "Download Lirical Data",
                    event.getSource().getException().getMessage(),
                    event.getSource().getException())
            );

            executorService.submit(downloadLiricalData);

        e.consume();
    }

    /**
     * Open DirChooser and ask user to provide the LIRCAL data directory.
     * DEPRECATING BECAUSE IT ADDS COMPLICATION
     * NOW THE USER COULD USE THIS TO SET TO AN INVALID DIRECTORY AND WE DO NOT CHECK FOR IT
     */
   // @FXML
    @Deprecated(forRemoval = true)
    private void setLiricalDataDirButtonAction(ActionEvent e) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Set Path to LIRICAL Data Directory");
        File liricalDataDir = directoryChooser.showDialog(contentBox.getScene().getWindow());
        if (liricalDataDir != null)
            optionalResources.liricalResources().setDataDirectory(liricalDataDir.toPath());
        else
            optionalResources.liricalResources().setDataDirectory(null);
        e.consume();
    }

    /**
     * Open DirChooser and ask user to provide a directory where the LIRCAL results should be saved.
     */
    @FXML
    private void setLiricalResultsDirButtonAction(ActionEvent e) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Set Path to LIRICAL Results Directory");
        File resultsDir = directoryChooser.showDialog(contentBox.getScene().getWindow());
        if (resultsDir != null)
            optionalResources.setLiricalResults(resultsDir.toPath());
        else
            optionalResources.setLiricalResults(null);
        e.consume();
    }

    /**
     * Open DirChooser and ask user to provide a path to the Exomiser hg19 variant file.
     */
    @FXML
    private void setExomiserHg19VariantFileButtonAction() {
        loadFileType(FileType.EXOMISER_HG19);
    }

    /**
     * Open DirChooser and ask user to provide a path to the Exomiser hg38 variant file.
     */
    @FXML
    private void setExomiserHg38VariantFileButtonAction() {
        loadFileType(FileType.EXOMISER_HG38);
    }

    /**
     * Open DirChooser and ask user to provide a path to the Background Frequency file.
     */
    @FXML
    private void setBackgroundFrequencyFileButtonAction() {
       loadFileType(FileType.BACKGROUND_VARIANT_FREQUENCY);
    }

    private void loadFileType(FileType fileType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Set Local %s File".formatted(fileType));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(fileType.description(), fileType.extensions()));
        File selectedFile = fileChooser.showOpenDialog(contentBox.getScene().getWindow());

        // A consumer for setting the selected file.
        Consumer<Path> consumer = switch (fileType) {
            case MONDO -> optionalResources.ontologyResources()::setMondoPath;
            case EXOMISER_HG19 -> optionalResources.liricalResources()::setExomiserHg19VariantDbFile;
            case EXOMISER_HG38 -> optionalResources.liricalResources()::setExomiserHg38VariantDbFile;
            case BACKGROUND_VARIANT_FREQUENCY -> optionalResources.liricalResources()::setBackgroundVariantFrequencyFile;
        };

        // Note that we can also unset a resource path.
        consumer.accept(selectedFile == null ? null : selectedFile.toPath());
    }


    /**
     * Download MONDO.json file to the application home directory.
     */
    @FXML
    private void downloadMondoFileButtonAction(ActionEvent e) {
        Path mondoPath = optionalResources.ontologyResources().mondoPathProperty().getValue();
        if (mondoPath != null) {
            Path mondoDir = mondoPath.getParent();
            String message = String.join(" ", "Path to Mondo file already set. To download new Mondo file, delete all files in",
                                        mondoDir.toString(), "and restart L4CI");
            PopUps.showInfoMessage(message,"Download Mondo");
        } else {
            // Construct Mondo URL
            URL mondoUrl;
            try {
                mondoUrl = new URL(appProperties.mondoJsonUrl());
            } catch (MalformedURLException ex) {
                PopUps.showException("Download Mondo JSON", ex.getMessage(), ex);
                return;
            }
            LOGGER.debug("Downloading Mondo JSON from {}", mondoUrl);

            DownloadMondoTask downloadMondoTask = new DownloadMondoTask(mondoUrl, dataDirectory);
            downloadMondoTask.setOnSucceeded(event -> optionalResources.ontologyResources().setMondoPath(downloadMondoTask.getValue()));
            downloadMondoTask.setOnFailed(event -> PopUps.showException(
                    "Download MONDO JSON file",
                    event.getSource().getException().getMessage(),
                    event.getSource().getException())
            );

            executorService.submit(downloadMondoTask);
            e.consume();
        }
    }

    private enum FileType {
        MONDO("Mondo JSON File", "*.json"),
        EXOMISER_HG19("Exomiser hg19 Variant Database", "*.mv.db"),
        EXOMISER_HG38("Exomiser hg38 Variant Database", "*.mv.db"),
        BACKGROUND_VARIANT_FREQUENCY("Background Variant Frequency", "*.tsv");

        private final String description;
        private final String[] extensions;

        FileType(String description, String... extensions) {
            this.description = description;
            this.extensions = extensions;
        }

        String description() {
            return description;
        }

        String[] extensions() {
            return extensions;
        }
    }

}

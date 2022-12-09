package org.monarchinitiative.l2ci.gui.controller;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.BioDownloaderBuilder;
import org.monarchinitiative.l2ci.gui.PopUps;
import org.monarchinitiative.l2ci.gui.resources.*;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static javafx.beans.binding.Bindings.when;

/**
 * This class is the controller of the Set Resources dialog. The user performs initial setup of the
 * resources that are required to run the GUI.
 * The resource paths are set to {@link OptionalResources} observable properties.
 * The rest of the GUI should watch the observable properties and update accordingly.
 */
public class ResourcesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesController.class);

    private final OptionalResources optionalResources;
    private final Path dataDirectory;
    private final ExecutorService executorService;

    @FXML
    private VBox contentBox;

    @FXML
    private ProgressIndicator hpoProgressIndicator;

    @FXML
    private ProgressIndicator hpoaProgressIndicator;

    @FXML
    private ProgressIndicator mondoProgressIndicator;

    @FXML
    private Label hpJsonLabel;

    @FXML
    private Label hpoaLabel;

    @FXML
    private Label mondoLabel;

    @FXML
    private Label liricalDataDirLabel;

    @FXML
    private Label liricalResultsDirLabel;

    @FXML
    private Label exomiserFileLabel;

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
                        Path dataDirectory, ExecutorService executorService) {
        this.optionalResources = optionalResources;
        this.dataDirectory = dataDirectory;
        this.executorService = executorService;
    }


    /**
     * Initialize elements of this controller.
     */
    @FXML
    private void initialize() {
        // HPO/Mondo resources
        OntologyResources ontologyResources = optionalResources.ontologyResources();
//        StringBinding hpoPath = preparePathBinding(ontologyResources.hpoPathProperty(), "Unset");
//        hpJsonLabel.textProperty().bind(hpoPath);
//
//        StringBinding hpoaPath = preparePathBinding(ontologyResources.hpoaPathProperty(), "Unset");
//        hpoaLabel.textProperty().bind(hpoaPath);

        StringBinding mondoPath = preparePathBinding(ontologyResources.mondoPathProperty(), "Unset");
        mondoLabel.textProperty().bind(mondoPath);

        // TODO - set progress indicator status/visibility

        // Lirical resources
        LiricalResources liricalResources = optionalResources.liricalResources();
        StringBinding liricalData = preparePathBinding(liricalResources.dataDirectoryProperty(), "Unset");
        liricalDataDirLabel.textProperty().bind(liricalData);

        StringBinding exomiserDb = preparePathBinding(liricalResources.exomiserVariantDbFileProperty(), "Unset");
        exomiserFileLabel.textProperty().bind(exomiserDb);

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


        InvalidationListener genomeBuildChecker = obs -> {
            String genomeBuild = genomeBuildChoiceBox.getValue().toString().toLowerCase();

            Path exomiserPath = liricalResources.getExomiserVariantDbFile();
            Path background = liricalResources.getBackgroundVariantFrequencyFile();
            if ((exomiserPath != null && !exomiserPath.toString().toLowerCase().contains(genomeBuild))
                    || (background != null && !background.toString().toLowerCase().contains(genomeBuild))) {
                PopUps.showInfoMessage("Genome build of Exomiser variant or background frequency file does not match the selected genome build.", "Warning");
            }
        };

        genomeBuildChoiceBox.valueProperty().addListener(genomeBuildChecker);
        exomiserFileLabel.textProperty().addListener(genomeBuildChecker);
        bkgFreqFileLabel.textProperty().addListener(genomeBuildChecker);

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
    void close() {
        this.mondoLabel.getScene().getWindow().hide();
    }

    /**
     * Open DirChooser and ask user to provide a file where the local hp.json file is located.
     */
    @FXML
    private void setHPOFileButtonAction(Event e) {
        loadFileType(FileType.HPO);
        e.consume();
    }

    /**
     * Open DirChooser and ask user to provide a file where the local phenotype.hpoa file is located.
     */
    @FXML
    private void setHPOAFileButtonAction(Event e) {
        loadFileType(FileType.HPOA);
        e.consume();
    }

    /**
     * Open DirChooser and ask user to provide a file where the local mondo.json file is located.
     */
    @FXML
    private void setMondoFileButtonAction(Event e) {
        loadFileType(FileType.MONDO);
        e.consume();
    }

    /**
     * Open DirChooser and ask user to provide the LIRCAL data directory.
     */
    @FXML
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
     * Open DirChooser and ask user to provide a path to the Exomiser variant file.
     */
    @FXML
    void setExomiserVariantFileButtonAction() {
        loadFileType(FileType.EXOMISER);
    }

    /**
     * Open DirChooser and ask user to provide a path to the Background Frequency file.
     */
    @FXML
    void setBackgroundFrequencyFileButtonAction() {
       loadFileType(FileType.BACKGROUND_VARIANT_FREQUENCY);
    }

    private void loadFileType(FileType fileType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Set Local %s File".formatted(fileType));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(fileType.description(), fileType.extensions()));
        File selectedFile = fileChooser.showOpenDialog(contentBox.getScene().getWindow());

        // A consumer for setting the selected file.
        Consumer<Path> consumer = switch (fileType) {
            case HPO -> optionalResources.ontologyResources()::setHpoPath;
            case HPOA -> optionalResources.ontologyResources()::setHpoaPath;
            case MONDO -> optionalResources.ontologyResources()::setMondoPath;
            case EXOMISER -> optionalResources.liricalResources()::setExomiserVariantDbFile;
            case BACKGROUND_VARIANT_FREQUENCY -> optionalResources.liricalResources()::setBackgroundVariantFrequencyFile;
        };

        // Note that we can also unset a resource path.
        consumer.accept(selectedFile == null ? null : selectedFile.toPath());
    }

    private void executeCommand(String[] args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            reader.lines()
                    .forEach(System.out::println);
        }
    }


    /**
     * Download HP.json file to the application home directory.
     */
    @FXML
    private void downloadHPOFileButtonAction() {
        downloadFileButtonAction(DownloadableResource.HPO);
    }

    /**
     * Download PHENOTYPE.hpoa file to the application home directory.
     */
    @FXML
    private void downloadHPOAFileButtonAction() {
        downloadFileButtonAction(DownloadableResource.HPOA);
    }

    /**
     * Download MONDO.json file to the application home directory.
     */
    @FXML
    private void downloadMondoFileButtonAction() {
        downloadFileButtonAction(DownloadableResource.MONDO);
    }

    private class DownloadTask extends Task<Void> {

        private final DownloadableResource downloadableResource;
        private final Path path;

        private DownloadTask(DownloadableResource downloadableResource, Path path) {
            this.downloadableResource = downloadableResource;
            this.path = path;
        }

        @Override
        protected Void call() throws Exception {
            updateProgress(0.02, 1);
            BioDownloaderBuilder builder = BioDownloader.builder(path);
            switch (downloadableResource) {
                case HPO -> {
                    BioDownloader downloader = builder.hpoJson().build();
                    updateProgress(0.5, 1);
                    downloader.download();
                    updateProgress(1, 1);
                }
                case HPOA -> {
                    BioDownloader downloader = builder.hpDiseaseAnnotations().build();
                    updateProgress(0.5, 1);
                    downloader.download();
                    updateProgress(1, 1);
                }
                case MONDO -> {
                    try {
                        BioDownloader downloader = builder.mondoOwl().build();
                        updateProgress(0.25, 1);
                        downloader.download();
                        updateProgress(0.5, 1);
                        Path owl = path.getParent().resolve("mondo.owl");
                        Path obographsJar = Path.of("non-existent.jar"); // TODO - can we drop this?
//                        String jarPath = pgProperties.getProperty("obographs.jar.path");
                        if (obographsJar != null && Files.isRegularFile(obographsJar)) {
                            String[] command = {
                                    "java",  "-jar", obographsJar.toAbsolutePath().toString(),
                                    "convert", "-f", "json", owl.toAbsolutePath().toString()
                            };
                            LOGGER.info("Converting mondo.owl to readable mondo.json file using obographs.");
                            LOGGER.info(String.join(" ", command));
                            executeCommand(command);
                            updateProgress(0.75, 1);
                            // We assume that Obographs writes the Mondo JSON here.
                            Path assumedMondoJsonFile = owl.getParent().resolve("mondo.json");
                            if (!Files.exists(assumedMondoJsonFile)) {
                                LOGGER.warn("The Mondo JSON is not at {}", assumedMondoJsonFile.toAbsolutePath());
                                return null;
                            }
                            optionalResources.ontologyResources().setMondoPath(assumedMondoJsonFile);
                        } else {
                            LOGGER.info("Cannot find obographs-cli jar file to convert mondo.owl to readable mondo.json file.");
                        }
                        updateProgress(1,1);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            return null;
        }
    }

    private void downloadFileButtonAction(DownloadableResource resource) {
        Path target = dataDirectory.resolve(resource.fileName());
        if (Files.isDirectory(target)) {
            // An exotic and unlikely case - the data directory has a subdirectory that has the same name as
            // the file we're trying to download.
            PopUps.showInfoMessage("Unable to use %s as data directory".formatted(dataDirectory.toAbsolutePath()), "Download %s".formatted(resource));
            return;
        }

        boolean abort = Files.exists(target) && PopUps.getBooleanFromUser("Overwrite?",
                resource + " file already exists at " + target.toAbsolutePath(),
                "Download " + resource + " file");

        if (abort)
            // The user chose not to overwrite the folder.
            return;

        DownloadTask task = new DownloadTask(resource, target);
        switch (resource) {
            case HPO -> trackProgress(task, hpoProgressIndicator);
            case HPOA -> trackProgress(task, hpoaProgressIndicator);
            case MONDO -> trackProgress(task, mondoProgressIndicator);
        }
        task.setOnSucceeded(e -> setResource(resource, target));
        // TODO(mabeckwith) - address task failure - notify the user.
        executorService.submit(task);
    }

    private void trackProgress(Task<?> task, ProgressIndicator pb) {
        pb.progressProperty().unbind();
        pb.setProgress(0);
        pb.progressProperty().bind(task.progressProperty());
    }

    private void setResource(DownloadableResource type, Path target) {
        switch (type) {
            case HPO -> optionalResources.ontologyResources().setHpoPath(target);
            case HPOA -> optionalResources.ontologyResources().setHpoaPath(target);
            case MONDO -> optionalResources.ontologyResources().setMondoPath(target);
        }
    }

    private enum FileType {
        HPO("HPO JSON File", "*.json"),
        HPOA("HPO Annotation File", "*.hpoa"),
        MONDO("Mondo JSON File", "*.json"),
        EXOMISER("Exomiser Variant Database", "*.mv.db"),
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

    private enum DownloadableResource {
        HPO("hp.json"),
        HPOA("phenotype.hpoa"),
        MONDO("mondo.json");

        private final String fileName;

        DownloadableResource(String fileName) {
            this.fileName = fileName;
        }

        String fileName() {
            return fileName;
        }
    }

}

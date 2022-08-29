package org.monarchinitiative.l2ci.gui.controller;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.BioDownloaderBuilder;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.l2ci.gui.PopUps;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoaResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalMondoResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * This class is the controller of the setResources dialog. The user performs initial setup of the
 * resources that are required to run the GUI. The resource paths are stored in e.g. {@link OptionalMondoResource} object. No
 * setting to {@link Properties} object is being done.
 * <p>
 * Created by Daniel Danis on 7/16/17.
 */
public final class ResourcesController {

    private final MainController mainController = MainController.getController();

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesController.class);

    private final OptionalHpoResource optionalHpoResource;

    private final OptionalHpoaResource optionalHpoaResource;

    private final OptionalMondoResource optionalMondoResource;


    private final Properties pgProperties;

    private final ExecutorService executorService;

    @FXML
    public ProgressIndicator hpoProgressIndicator;

    @FXML
    public ProgressIndicator hpoaProgressIndicator;

    @FXML
    public ProgressIndicator mondoProgressIndicator;

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
    private Button downloadHPOAButton;

    @FXML
    private ChoiceBox genomeBuildChoiceBox;

    @FXML
    private ChoiceBox transcriptDBChoiceBox;

    @FXML
    private TextField alleleFreqTextField;

    @FXML
    private TextField pathogenicityTextField;

    @FXML
    private TextField variantBkgFreqTextField;

    @FXML
    private CheckBox strictCheckBox;

    @FXML
    private Button closeButton;


    @Autowired
    ResourcesController(OptionalHpoResource hpoResource, OptionalHpoaResource hpoaResource,
                        OptionalMondoResource mondoResource, Properties properties, ExecutorService executorService) {
        this.optionalHpoResource = hpoResource;
        this.optionalHpoaResource = hpoaResource;
        this.optionalMondoResource = mondoResource;
        this.pgProperties = properties;
        this.executorService = executorService;
    }


    /**
     * Initialize elements of this controller.
     */
    @FXML
    private void initialize() {
        String hpPath = pgProperties.getProperty(OptionalHpoResource.HP_JSON_PATH_PROPERTY);
        String hpoaPath = pgProperties.getProperty(OptionalHpoaResource.HPOA_PATH_PROPERTY);
        String mondoPath = pgProperties.getProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY);
        String liricalDataPath = pgProperties.getProperty("lirical.data.path");
        String liricalResultsPath = pgProperties.getProperty("lirical.results.path");
        String exomiserPath = pgProperties.getProperty("exomiser.variant.path");
        String bkgFreqPath = pgProperties.getProperty("background.frequency.path");
        downloadHPOAButton.setDisable(optionalHpoResource.getOntology() == null);
        String[] paths = {hpPath, hpoaPath, mondoPath};
        Label[] labels = {hpJsonLabel, hpoaLabel, mondoLabel};
        ProgressIndicator[] indicators = {hpoProgressIndicator, hpoaProgressIndicator, mondoProgressIndicator};
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null && new File(paths[i]).isFile()) {
                labels[i].setText(paths[i]);
                indicators[i].setProgress(1);
            } else {
                labels[i].setText("unset");
                indicators[i].setProgress(0);
            }
        }
        liricalDataDirLabel.setText(Objects.requireNonNullElse(liricalDataPath, "unset"));
        liricalResultsDirLabel.setText(Objects.requireNonNullElse(liricalResultsPath, "unset"));
        exomiserFileLabel.setText(Objects.requireNonNullElse(exomiserPath, "unset"));
        bkgFreqFileLabel.setText(Objects.requireNonNullElse(bkgFreqPath, "unset"));
        HashMap<ChoiceBox, String> choiceBoxHashMap = new HashMap<>();
        choiceBoxHashMap.put(genomeBuildChoiceBox, "genome.build");
        choiceBoxHashMap.put(transcriptDBChoiceBox, "transcript.database");
        choiceBoxHashMap.forEach((choiceBox, propName) -> {
            String property = pgProperties.getProperty(propName);
            List<String> choices = new ArrayList<>();
            if (propName.contains("genome")) {
                choices = Arrays.asList("hg19", "hg38");
            } else if (propName.contains("transcript")) {
                choices = Arrays.asList("refSeq", "UCSC");
            }
            choiceBox.getItems().addAll(choices);
            choiceBox.setValue(property);
            choiceBox.valueProperty().addListener(x -> {
                pgProperties.setProperty(propName, choiceBox.getValue().toString());
                LOGGER.info(propName + ": " + pgProperties.getProperty(propName));
            });
        });
        genomeBuildChoiceBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            File exomiserFile = new File(exomiserPath);
            File bkgFreqFile = new File(bkgFreqPath);
            if (!(exomiserFile.isFile() && exomiserFile.getName().contains(newVal.toString()))
                    || !(bkgFreqFile.isFile() && bkgFreqFile.getName().contains(newVal.toString()))) {
                PopUps.showInfoMessage("Genome build of Exomiser variant or background frequency file does not match the selected genome build.", "Warning");
            }
        });
        HashMap<TextField, String> textFieldHashMap = new HashMap<>();
        textFieldHashMap.put(pathogenicityTextField, "pathogenicity.threshold");
        textFieldHashMap.put(variantBkgFreqTextField, "default.variant.background.frequency");
        textFieldHashMap.put(alleleFreqTextField, "default.allele.frequency");
        textFieldHashMap.forEach((textField, propName) -> {
            String property = pgProperties.getProperty(propName);
            textField.setText(property);
            textField.setOnKeyPressed(e -> {
                if (e.getCode() != KeyCode.ENTER) {
                    return;
                }
                pgProperties.setProperty(propName, textField.textProperty().get());
                LOGGER.info(propName + ": " + pgProperties.getProperty(propName));
            });
        });
        strictCheckBox.setSelected(Boolean.parseBoolean(pgProperties.getProperty("strict")));
        strictCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            pgProperties.setProperty("strict", newVal.toString());
            LOGGER.info("strict: " + pgProperties.getProperty("strict"));
        });
    }

    @FXML
    void close() {
        this.mondoLabel.getScene().getWindow().hide();
    }


    /**
     * Open DirChooser and ask user to provide a directory where the local hp.json file is located.
     */
    @FXML
    void setHPOFileButtonAction(Event e) {
        setFileButtonAction(e, "HPO", OptionalHpoResource.HP_JSON_PATH_PROPERTY, hpJsonLabel);
    }

    /**
     * Open DirChooser and ask user to provide a directory where the local phenotype.hpoa file is located.
     */
    @FXML
    void setHPOAFileButtonAction(Event e) {
        setFileButtonAction(e, "HPOA", OptionalHpoaResource.HPOA_PATH_PROPERTY, hpoaLabel);
    }

    /**
     * Open DirChooser and ask user to provide a directory where the local Mondo.json file is located.
     */
    @FXML
    void setMondoFileButtonAction(Event e) {
        setFileButtonAction(e, "MONDO", OptionalMondoResource.MONDO_JSON_PATH_PROPERTY, mondoLabel);
    }

    void setFileButtonAction(Event e, String type, String pathProperty, Label label) {
        String filepath = pgProperties.getProperty(pathProperty);
        if (filepath == null) {
            loadFile(e, type, pathProperty);
        } else {
            boolean response = PopUps.getBooleanFromUser("Overwrite " + type + " File with Local File?",
                    "Current " + type + " File: " + filepath,
                    "Set Local " + type + " file");
            if (response) {
                loadFile(e, type, pathProperty);
            }
        }
        label.setText(pgProperties.getProperty(pathProperty));
    }

    void loadFile(Event e, String type, String pathProperty) {
        try {
            switch (type) {
                case "HPO" -> mainController.loadHPOFile(e);
                case "HPOA" -> mainController.loadHPOAFile(e);
                case "MONDO" -> mainController.loadMondoFile(e);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error occurred during opening the file '{}'", pgProperties.getProperty(pathProperty), ex);
        }
    }

    /**
     * Open DirChooser and ask user to provide a directory where the LIRCAL data directory is located.
     */
    @FXML
    void setLiricalDataDirButtonAction() {
        mainController.setLiricalDataDirectory();
        String liricalDataPath = pgProperties.getProperty("lirical.data.path");
        liricalDataDirLabel.setText(liricalDataPath);
    }

    /**
     * Open DirChooser and ask user to provide a directory where the LIRCAL results should be saved.
     */
    @FXML
    void setLiricalResultsDirButtonAction() {
        mainController.setLiricalResultsDirectory();
        String liricalResultsPath = pgProperties.getProperty("lirical.results.path");
        liricalResultsDirLabel.setText(liricalResultsPath);
    }

    /**
     * Open DirChooser and ask user to provide a path to the Exomiser variant file.
     */
    @FXML
    void setExomiserVariantFileButtonAction() {
        mainController.setExomiserVariantFile();
        String exomiserFilePath = pgProperties.getProperty("exomiser.variant.path");
        exomiserFileLabel.setText(exomiserFilePath);
    }

    /**
     * Open DirChooser and ask user to provide a path to the Background Frequency file.
     */
    @FXML
    void setBackgroundFrequencyFileButtonAction() {
        mainController.setBackgroundFrequencyFile();
        String bkgFreqFilePath = pgProperties.getProperty("background.frequency.path");
        bkgFreqFileLabel.setText(bkgFreqFilePath);
    }


    void executeCommand(String[] args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) { break; }
            System.out.println(line);
        }
        r.close();
    }


    /**
     * Download HP.json file to the application home directory.
     */
    @FXML
    void downloadHPOFileButtonAction() {
        downloadFileButtonAction("HPO", OptionalHpoResource.HP_JSON_PATH_PROPERTY);
    }

    /**
     * Download PHENOTYPE.hpoa file to the application home directory.
     */
    @FXML
    void downloadHPOAFileButtonAction() {
        downloadFileButtonAction("HPOA", OptionalHpoaResource.HPOA_PATH_PROPERTY);
    }


    /**
     * Download MONDO.json file to the application home directory.
     */
    @FXML
    void downloadMondoFileButtonAction() {
        downloadFileButtonAction("MONDO", OptionalMondoResource.MONDO_JSON_PATH_PROPERTY);
    }

    public class DownloadTask extends Task<Void> {

        private final String path;

        private final String type;

        public DownloadTask(String path, String type) {
            this.path = path;
            this.type = type;
        }
        @Override
        protected Void call() throws Exception {
            Path filePath = Path.of(path);
            updateProgress(0.02, 1);
            switch (type) {
                case "HPO":
                    BioDownloaderBuilder builder = BioDownloader.builder(filePath);
                    builder.hpoJson();
                    BioDownloader downloader = builder.build();
                    updateProgress(0.5, 1);
                    downloader.download();
                    updateProgress(1, 1);
                    break;
                case "HPOA":
                    builder = BioDownloader.builder(filePath);
                    builder.hpDiseaseAnnotations();
                    downloader = builder.build();
                    updateProgress(0.5, 1);
                    downloader.download();
                    updateProgress(1, 1);
                    break;
                case "MONDO":
                    try {
                        builder = BioDownloader.builder(filePath);
                        builder.mondoOwl();
                        downloader = builder.build();
                        updateProgress(0.25, 1);
                        downloader.download();
                        updateProgress(0.5, 1);
                        File owl = new File(path, "mondo.owl");
                        String jarPath = pgProperties.getProperty("obographs.jar.path");
                        if (jarPath != null && new File(jarPath).isFile()) {
                            String[] command = {"java",  "-jar", jarPath, "convert", "-f", "json", owl.getAbsolutePath()};
                            mainController.logger.info("Converting mondo.owl to readable mondo.json file using obographs.");
                            mainController.logger.info(String.join(" ", command));
                            executeCommand(command);
                            updateProgress(0.75, 1);
                            mainController.loadMondoFile(new File(path, "mondo.json").getAbsolutePath());
                        } else {
                            mainController.logger.info("Cannot find obographs-cli jar file to convert mondo.owl to readable mondo.json file.");
                        }
                        updateProgress(1,1);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    break;
            }
            return null;
        }
    }

    void downloadFileButtonAction(String type, String pathProperty) {
        String path = pgProperties.getProperty("download.path");
        File target = new File(path, pathProperty.replace(".path", "").replace("hpoa/path", "phenotype.hpoa"));
        if (target.isFile()) {
            boolean response = PopUps.getBooleanFromUser("Overwrite?",
                    type + " file already exists at " + target.getAbsolutePath(),
                    "Download " + type + " file");
            if (!response) {
                setResource(type, target);
                return;
            }
        }
        try {
            DownloadTask task = new DownloadTask(path, type);
            switch (type) {
                case "HPO":
                    trackProgress(task, hpoProgressIndicator);
                    break;
                case "HPOA":
                    trackProgress(task, hpoaProgressIndicator);
                    break;
                case "MONDO":
                    trackProgress(task, mondoProgressIndicator);
                    break;
            }
            task.setOnSucceeded(e -> setResource(type, target));
            mainController.executor.submit(task);
        } catch (Exception ex) {
            LOGGER.warn("Error occurred downloading the file '{}'", target, ex);
        }
    }

    void trackProgress(Task task, ProgressIndicator pb) {
        pb.setProgress(0);
        pb.progressProperty().unbind();
        pb.progressProperty().bind(task.progressProperty());
    }

    void setResource(String type, File target) {
        try {
            String filePath = target.getAbsolutePath();
            switch (type) {
                case "HPO":
                    mainController.loadHPOFile(target);
                    hpJsonLabel.setText(filePath);
                    downloadHPOAButton.setDisable(false);
                    break;
                case "HPOA":
                    mainController.loadHPOAFile(filePath);
                    hpoaLabel.setText(filePath);
                    break;
                case "MONDO":
                    mainController.loadMondoFile(filePath);
                    mondoLabel.setText(filePath);
                    break;
            }
        } catch (Exception ex) {
            LOGGER.warn("Error occurred during opening the file '{}'", target, ex);
        }
    }

}

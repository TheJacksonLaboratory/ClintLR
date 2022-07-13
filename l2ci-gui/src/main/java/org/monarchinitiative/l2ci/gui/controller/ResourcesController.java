package org.monarchinitiative.l2ci.gui.controller;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
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

import java.io.File;
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
    public ProgressIndicator hpoProgressIndicator = new ProgressIndicator();

    @FXML
    public ProgressIndicator hpoaProgressIndicator = new ProgressIndicator();

    @FXML
    public ProgressIndicator mondoProgressIndicator = new ProgressIndicator();

   @FXML
    private Label hpJsonLabel = new Label();

    @FXML
    private Label hpoaLabel = new Label();

    @FXML
    private Label mondoLabel = new Label();

    @FXML
    private Label liricalDataDirLabel = new Label();

    @FXML
    private Label exomiserFileLabel = new Label();

    @FXML
    private Label bkgFreqFileLabel = new Label();

    @FXML
    private Button downloadHPOAButton = new Button();

    @FXML
    private ChoiceBox genomeBuildChoiceBox = new ChoiceBox<>();

    @FXML
    private ChoiceBox transcriptDBChoiceBox = new ChoiceBox<>();

    @FXML
    private TextField alleleFreqTextField = new TextField();

    @FXML
    private TextField pathogenicityTextField = new TextField();

    @FXML
    private TextField variantBkgFreqTextField = new TextField();

    @FXML
    private ChoiceBox strictChoiceBox = new ChoiceBox();


    @Autowired
    ResourcesController(OptionalHpoResource hpoResource, OptionalHpoaResource hpoaResource,
                        OptionalMondoResource mondoResource, Properties properties, ExecutorService executorService) {
        this.optionalHpoResource = hpoResource;
        this.optionalHpoaResource = hpoaResource;
        this.optionalMondoResource = mondoResource;
        this.pgProperties = properties;
        this.executorService = executorService;
        initialize();
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
        exomiserFileLabel.setText(Objects.requireNonNullElse(exomiserPath, "unset"));
        bkgFreqFileLabel.setText(Objects.requireNonNullElse(bkgFreqPath, "unset"));
        HashMap<ChoiceBox, String> choiceBoxHashMap = new HashMap<>();
        choiceBoxHashMap.put(genomeBuildChoiceBox, "genome.build");
        choiceBoxHashMap.put(transcriptDBChoiceBox, "transcript.database");
        choiceBoxHashMap.put(strictChoiceBox, "strict");
        choiceBoxHashMap.forEach((choiceBox, propName) -> {
            String property = pgProperties.getProperty(propName);
            List<String> choices = new ArrayList<>();
            if (propName.contains("genome")) {
                choices = Arrays.asList("hg19", "hg38");
            } else if (propName.contains("transcript")) {
                choices = Arrays.asList("refSeq", "UCSC");
            } else if (propName.contains("strict")) {
                choices = Arrays.asList("true", "false");
            }
            choiceBox.getItems().addAll(choices);
            choiceBox.setValue(property);
            choiceBox.valueProperty().addListener(x -> {
                pgProperties.setProperty(propName, choiceBox.getValue().toString());
                LOGGER.info(propName + ": " + pgProperties.getProperty(propName));
            });
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
        String liricalDataPath = pgProperties.getProperty("lirical.data.path");
        if (liricalDataPath == null) {
            mainController.setLiricalDataDirectory();
        } else {
            liricalDataDirLabel.setText(liricalDataPath);
        }
    }

    /**
     * Open DirChooser and ask user to provide a path to the Exomiser variant file.
     */
    @FXML
    void setExomiserVariantFileButtonAction() {
        String exomiserFilePath = pgProperties.getProperty("exomiser.variant.path");
        if (exomiserFilePath == null) {
            mainController.setExomiserVariantFile();
        } else {
            exomiserFileLabel.setText(exomiserFilePath);
        }
    }

    /**
     * Open DirChooser and ask user to provide a path to the Background Frequency file.
     */
    @FXML
    void setBackgroundFrequencyFileButtonAction() {
        String bkgFreqFilePath = pgProperties.getProperty("background.frequency.path");
        if (bkgFreqFilePath == null) {
            mainController.setBackgroundFrequencyFile();
        } else {
            bkgFreqFileLabel.setText(bkgFreqFilePath);
        }
    }

    public void downloadFile(String path, String type) throws FileDownloadException {
        switch (type) {
            case "HPO":
                BioDownloaderBuilder builder = BioDownloader.builder(Path.of(path));
                builder.hpoJson();
                BioDownloader downloader = builder.build();
                downloader.download();
                break;
            case "HPOA":
                builder = BioDownloader.builder(Path.of(path));
                builder.hpDiseaseAnnotations();
                downloader = builder.build();
                downloader.download();
                break;
            case "MONDO":
                mainController.downloadMondoFile(pgProperties.getProperty("download.path"));
                break;
        }
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
            downloadFile(path, type);
            return null;
        }
    }

    void downloadFileButtonAction(String type, String pathProperty) {
        String path = pgProperties.getProperty("download.path");
        File target = new File(path, pathProperty.replace(".path", "").replace("hpoa/path", "LIRICAL/data/phenotype.hpoa"));
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
            task.setOnSucceeded(e -> setResource(type, target));
            mainController.executor.submit(task);
        } catch (Exception ex) {
            LOGGER.warn("Error occurred downloading the file '{}'", target, ex);
        }
    }

    void setResource(String type, File target) {
        try {
            String filePath = target.getAbsolutePath();
            switch (type) {
                case "HPO":
                    mainController.loadHPOFile(target);
                    hpJsonLabel.setText(filePath);
                    downloadHPOAButton.setDisable(false);
                    pgProperties.setProperty(OptionalHpoResource.HP_JSON_PATH_PROPERTY, filePath);
                    break;
                case "HPOA":
                    mainController.loadHPOAFile(filePath);
                    hpoaLabel.setText(filePath);
                    pgProperties.setProperty(OptionalHpoaResource.HPOA_PATH_PROPERTY, filePath);
                    break;
                case "MONDO":
                    mainController.loadMondoFile(filePath);
                    mondoLabel.setText(filePath);
                    pgProperties.setProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY, filePath);
                    break;
            }
        } catch (Exception ex) {
            LOGGER.warn("Error occurred during opening the file '{}'", target, ex);
        }
    }

}

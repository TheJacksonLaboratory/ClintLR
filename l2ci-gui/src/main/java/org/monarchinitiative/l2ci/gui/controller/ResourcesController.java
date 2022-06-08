package org.monarchinitiative.l2ci.gui.controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
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

    private final String dataDir = "data";

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
    private Button downloadHPOAButton = new Button();


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
        downloadHPOAButton.setDisable(optionalHpoResource.getOntology() == null);
        if (hpPath != null) {
            hpJsonLabel.setText(hpPath);
            hpoProgressIndicator.setProgress(1);
        } else {
            hpJsonLabel.setText("unset");
            hpoProgressIndicator.setProgress(0);
        }
        if (hpoaPath != null) {
            hpoaLabel.setText(hpoaPath);
            hpoaProgressIndicator.setProgress(1);
        } else {
            hpoaLabel.setText("unset");
            hpoaProgressIndicator.setProgress(0);
        }
        if (mondoPath != null) {
            mondoLabel.setText(mondoPath);
            mondoProgressIndicator.setProgress(1);
        } else {
            mondoLabel.setText("unset");
            mondoProgressIndicator.setProgress(0);
        }
        if (liricalDataPath != null) {
            liricalDataDirLabel.setText(liricalDataPath);
        } else {
            liricalDataDirLabel.setText("unset");
        }
        if (exomiserPath != null) {
            exomiserFileLabel.setText(exomiserPath);
        } else {
            exomiserFileLabel.setText("unset");
        }
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
     * Open DirChooser and ask user to provide a directory where the Exomiser variant file is located.
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

    public void downloadFile(String path, String type) throws FileDownloadException {
        BioDownloaderBuilder builder = BioDownloader.builder(Path.of(path));
        switch (type) {
            case "HPO" -> builder.hpoJson();
            case "HPOA" -> builder.hpDiseaseAnnotations();
            case "MONDO" -> builder.mondoJson();
        }
        BioDownloader downloader = builder.build();
        downloader.download();
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

    void downloadFileButtonAction(String type, String pathProperty) {
        String homeDir = new File(".").getAbsolutePath();
        String path = String.join(File.separator, homeDir.substring(0, homeDir.length() - 2), dataDir);
        File target = new File(path, pathProperty);
        if (target.isFile()) {
            boolean response = PopUps.getBooleanFromUser("Overwrite?",
                    type + " file already exists at " + target.getAbsolutePath(),
                    "Download " + type + " file");
            if (!response) {
                setResource(type, target, true);
                return;
            }
        }
        try {
            downloadFile(path, type);
            setResource(type, target, false); //FIXME HPO Parser fails parsing downloaded mondo.json file
        } catch (Exception ex) {
            LOGGER.warn("Error occurred downloading the file '{}'", target, ex);
        }
    }

    void setResource(String type, File target, boolean load) {
        try {
            switch (type) {
                case "HPO" -> {
                    if (load) {
                        mainController.loadHPOFile(target);
                    }
                    hpJsonLabel.setText(target.getAbsolutePath());
                    downloadHPOAButton.setDisable(false);
                }
                case "HPOA" -> {
                    if (load) {
                        mainController.loadHPOAFile(target.getAbsolutePath());
                    }
                    hpoaLabel.setText(target.getAbsolutePath());
                }
                case "MONDO" -> {
                    String filepath = target.getAbsolutePath();
                    if (load) {
                        mainController.loadMondoFile(filepath);
                    }
                    mondoLabel.setText(filepath);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Error occurred during opening the file '{}'", target, ex);
        }
    }

}

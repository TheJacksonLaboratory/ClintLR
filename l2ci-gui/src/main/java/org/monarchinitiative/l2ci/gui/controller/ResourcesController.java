package org.monarchinitiative.l2ci.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.BioDownloaderBuilder;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.gui.PopUps;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoaResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalMondoResource;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesController.class);

    private final OptionalHpoResource optionalHpoResource;

    private final OptionalHpoaResource optionalHpoaResource;

    private final OptionalMondoResource optionalMondoResource;


    private final Properties pgProperties;

    private final File appHomeDir;

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
                        OptionalMondoResource mondoResource, Properties properties, @Qualifier("appHomeDir") File appHomeDir,
                        ExecutorService executorService) {
        this.optionalHpoResource = hpoResource;
        this.optionalHpoaResource = hpoaResource;
        this.optionalMondoResource = mondoResource;
        this.pgProperties = properties;
        this.appHomeDir = appHomeDir;
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
     * Open DirChooser and ask user to provide a directory where the LIRCAL data directory is located.
     */
    @FXML
    void setLiricalDataDirButtonAction() {
        String liricalDataPath = pgProperties.getProperty("lirical.data.path");
        if (liricalDataPath == null) {
            MainController.getController().setLiricalDataDirectory();
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
            MainController.getController().setExomiserVariantFile();
        } else {
            exomiserFileLabel.setText(exomiserFilePath);
        }
    }

    public void downloadFile(String type) throws FileDownloadException {
        BioDownloaderBuilder builder = BioDownloader.builder(appHomeDir.toPath());
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
        File target = new File(appHomeDir, OptionalHpoResource.DEFAULT_HPO_FILE_NAME);
        if (target.isFile()) {
            boolean response = PopUps.getBooleanFromUser("Overwrite?", "HPO file already exists at the target " +
                    "location", "Download " +
                    "HPO JSON file");
            if (!response) {
                try {
                    final Ontology ontology = OntologyLoader.loadOntology(target);
                    optionalHpoResource.setOntology(ontology);
                    hpJsonLabel.setText(target.getAbsolutePath());
                    pgProperties.setProperty(OptionalHpoResource.HP_JSON_PATH_PROPERTY, target.getAbsolutePath());
                    downloadHPOAButton.setDisable(false);
                } catch (Exception ex) {
                    LOGGER.warn("Error occurred during opening the ontology file '{}'", target, ex);
                }
                return;
            }
        }
        try {
            downloadFile("HPO");
            final Ontology ontology = OntologyLoader.loadOntology(target);
            optionalHpoResource.setOntology(ontology);
            hpJsonLabel.setText(target.getAbsolutePath());
            pgProperties.setProperty(OptionalHpoResource.HP_JSON_PATH_PROPERTY, target.getAbsolutePath());
            downloadHPOAButton.setDisable(false);
        } catch (Exception ex) {
            LOGGER.warn("Error occurred during opening the ontology file '{}'", target, ex);
        }
    }

    /**
     * Download PHENOTYPE.hpoa file to the application home directory.
     */
    @FXML
    void downloadHPOAFileButtonAction() {
        File target = new File(appHomeDir, OptionalHpoaResource.DEFAULT_HPOA_FILE_NAME);
        if (target.isFile()) {
            boolean response = PopUps.getBooleanFromUser("Overwrite?", "HPOA file already exists at the target " +
                    "location", "Download " +
                    "HPOA file");
            if (!response) {
                try {
                    optionalHpoaResource.setAnnotationResources(target.getAbsolutePath(), optionalHpoResource.getOntology());
                    hpoaLabel.setText(target.getAbsolutePath());
                    pgProperties.setProperty(OptionalHpoaResource.HPOA_PATH_PROPERTY, target.getAbsolutePath());
                } catch (Exception ex) {
                    LOGGER.warn("Error occurred during opening the ontology file '{}'", target, ex);
                }
                return;
            }
        }
        try {
            downloadFile("HPOA");
            optionalHpoaResource.setAnnotationResources(target.getAbsolutePath(), optionalHpoResource.getOntology());
            hpoaLabel.setText(target.getAbsolutePath());
            pgProperties.setProperty(OptionalHpoaResource.HPOA_PATH_PROPERTY, target.getAbsolutePath());
        } catch (Exception ex) {
            LOGGER.warn("Error occurred during opening the ontology file '{}'", target, ex);
        }
    }


    /**
     * Download MONDO.json file to the application home directory.
     */
    @FXML
    void downloadMondoFileButtonAction() {
        File target = new File(appHomeDir, OptionalMondoResource.DEFAULT_MONDO_FILE_NAME);
        if (target.isFile()) {
            boolean response = PopUps.getBooleanFromUser("Overwrite?", "Mondo file already exists at the target " +
                    "location", "Download " +
                    "Mondo JSON file");
            if (!response) {
                try {
                    String path = target.getAbsolutePath();
                    HPOParser parser = new HPOParser(path);
                    final Ontology ontology = parser.getHPO();
                    optionalMondoResource.setOntology(ontology);
                    mondoLabel.setText(path);
                    pgProperties.setProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY, target.getAbsolutePath());
                } catch (Exception ex) {
                    LOGGER.warn("Error occurred during opening the ontology file '{}'", target, ex);
                }
                return;
            }
        }
        try {
            downloadFile("MONDO");
            String path = target.getAbsolutePath();
            //FIXME: HPOParser fails trying to parse the downloaded Mondo file.
//            HPOParser parser = new HPOParser(path);
//            final Ontology ontology = parser.getHPO();
//            optionalMondoResource.setOntology(ontology);
            mondoLabel.setText(path);
//            pgProperties.setProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY, target.getAbsolutePath());
        } catch (Exception ex) {
            LOGGER.warn("Error occurred during opening the ontology file '{}'", target, ex);
        }
    }

}

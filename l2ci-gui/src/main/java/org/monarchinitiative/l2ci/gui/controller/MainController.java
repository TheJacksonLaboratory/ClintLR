package org.monarchinitiative.l2ci.gui.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.BioDownloaderBuilder;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.l2ci.core.io.Download;
import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.core.io.MapFileWriter;
import org.monarchinitiative.l2ci.core.mondo.MondoStats;
import org.monarchinitiative.l2ci.core.pretestprob.PretestProbability;
import org.monarchinitiative.l2ci.gui.*;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoaResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalMondoResource;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

@Component
public class MainController {

    @Autowired
    private ApplicationContext applicationContext;
    private static MainController mainController;
    private Ontology ont;

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String EVENT_TYPE_CLICK = "click";

    /**
     * Application-specific properties (not the System properties!) defined in the 'application.properties' file that
     * resides in the classpath.
     */
    public final Properties pgProperties;

    public final ExecutorService executor;

    public final OptionalHpoResource optionalHpoResource;

    public final OptionalHpoaResource optionalHpoaResource;

    public final OptionalMondoResource optionalMondoResource;
    /**
     * Directory, where ontologies and HPO annotation files are being stored.
     */
    private final File l4ciDir;

    /**
     * Place at the bottom of the window for showing messages to user
     */
    @FXML
    private Label copyrightLabel;
    @FXML
    public HBox statusHBox;
    @FXML
    private TextField autocompleteTextfield;

    /**
     * The term that is currently selected in the Browser window.
     */
    private Term selectedTerm = null;

    /**
     * Key: a term name such as "Myocardial infarction"; value: the corresponding HPO id as a {@link TermId}.
     */
    private final Map<String, TermId> ontologyLabelsAndTermIdMap = new HashMap<>();
    /**
     * WebView for displaying details of the Term that is selected in the {@link #ontologyTreeView}.
     */
    @FXML
    private WebView infoWebView;
    /**
     * WebEngine backing up the {@link #infoWebView}.
     */
    private WebEngine infoWebEngine;

    @FXML
    private TreeView<OntologyTermWrapper> ontologyTreeView;

    /**
     * Slider to adjust pretest probability before running LIRICAL
     */
    @FXML
    private Slider probSlider = new Slider();
    @FXML
    private TextField sliderTextField = new TextField();
    @FXML
    private Button liricalButton = new Button();
    @FXML
    private Label treeLabel = new Label();

    public static double preTestProb;

    private Map<TermId, Double> pretestMap;
    public MapDisplayInterface mapDisplay;
    public static List<MapData> mapDataList;
    private Lirical lirical;

    private File mondoJSONFile;

    private Image redIcon;

    private Image redArrowIcon;

    private Image blackArrowIcon;

    public enum MessageType {
        INFO, WARNING, ERROR
    }

    private enum Relation {
        ANCESTOR("ancestor"),
        CHILD("child"),
        DESCENDENT("descendent"),
        PARENT("parent");

        private final String name;

        Relation(String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    @Value("${mondo.json.url}")
    private String mondoJsonUrl;

    @Autowired
    public MainController(OptionalHpoResource optionalHpoResource,
                          OptionalHpoaResource optionalHpoaResource,
                          OptionalMondoResource optionalMondoResource,
                          @Qualifier("configProperties") Properties properties,
                          @Qualifier("appHomeDir") File l4ciDir,
                          ExecutorService executorService) {
        this.optionalHpoResource = optionalHpoResource;
        this.optionalHpoaResource = optionalHpoaResource;
        this.optionalMondoResource = optionalMondoResource;
        this.pgProperties = properties;
        this.l4ciDir = l4ciDir;
        this.executor = executorService;
    }

    public static MainController getController() {
        return mainController;
    }

    @FXML
    private void initialize() throws IOException {
        mainController = this;
        logger.info("Initializing main controller");
        StartupTask task = new StartupTask(optionalHpoResource, optionalHpoaResource, optionalMondoResource, pgProperties);
        liricalButton.setDisable(true);
        String liricalData = pgProperties.getProperty("lirical.data.path");
        if (liricalData == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("No LIRICAL data directory set. Click OK to set LIRICAL data directory.");
            alert.showAndWait();
            if (alert.getResult().equals(ButtonType.OK)) {
                setLiricalDataDirectory();
            } else if (alert.getResult().equals(ButtonType.CANCEL)) {
                return;
            }
        }
        CompletableFuture.runAsync(() -> {
            try {
                lirical = task.buildLirical();
                if (lirical != null) {
                    liricalButton.setDisable(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        publishMessage("Loading resources");
        ProgressIndicator pb = new ProgressIndicator();
        pb.setProgress(0);
        pb.progressProperty().unbind();
        pb.progressProperty().bind(task.progressProperty());
        Stage window = PopUps.setupProgressDialog("Initializing", "Loading resources...", pb);
        window.show();
        window.toFront();
        task.setOnSucceeded(e -> {
            publishMessage("Successfully loaded files");
            window.close();
        });
        task.setOnFailed(e -> {
            publishMessage("Unable to load ontologies/annotations", MessageType.ERROR);
            window.close();
        });
        this.executor.submit(task);
        String ver = MainController.getVersion();
        copyrightLabel.setText("L4CI, v. " + ver + ", \u00A9 Monarch Initiative 2022");
        probSlider.setMin(1);
        probSlider.setMax(10);
        probSlider.setValue(5);
        probSlider.setMajorTickUnit(2);
        probSlider.setShowTickLabels(true);
        probSlider.setShowTickMarks(true);
        preTestProb = probSlider.getValue();
        probSlider.valueProperty().addListener(e -> sliderAction());
        sliderTextField.setText(String.valueOf(probSlider.getValue()));
        sliderTextField.setOnKeyReleased(event ->  {
            if (event.getCode().equals(KeyCode.ENTER)) {
                String text = sliderTextField.textProperty().get();
                if (!text.equals("")) {
                    try {
                        double value = Double.parseDouble(text);
                        adjustLimits(probSlider, value);
                        probSlider.setValue(value);
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }
                }
            }
        });

        InputStream redStream = getClass().getResourceAsStream("/icons/red_circle.png");
        InputStream redArrowStream = getClass().getResourceAsStream("/icons/red_circle_up_arrow.png");
        InputStream blackArrowStream = getClass().getResourceAsStream("/icons/black_circle_up_arrow.png");
        redIcon = new Image(Objects.requireNonNull(redStream));
        redArrowIcon = new Image(Objects.requireNonNull(redArrowStream));
        blackArrowIcon = new Image(Objects.requireNonNull(blackArrowStream));
        redStream.close();
        redArrowStream.close();
        blackArrowStream.close();

        String downloadPath = pgProperties.getProperty("download.path");
        if (downloadPath == null) {
            String appDir = new File(".").getAbsolutePath();
            String path = String.join(File.separator, appDir.substring(0, appDir.length() - 2), "data");
            pgProperties.setProperty("download.path", path);
        }

        if (pgProperties.getProperty("obographs.jar.path") == null) {
            String homeDir = System.getProperty("user.home");
            FileSearch fileSearch = new FileSearch();
            Files.walkFileTree(Path.of(homeDir), fileSearch);
            List<Path> files = fileSearch.getFileList();
            if (!files.isEmpty()) {
                String jarPath = files.get(0).toString();
                pgProperties.setProperty("obographs.jar.path", jarPath);
            } else {
                logger.info("Cannot find obographs to convert mondo.owl to mondo.json.");
            }
        }
        logger.info("obographs jar located at " + pgProperties.getProperty("obographs.jar.path"));



        ChangeListener<? super Object> listener = (obs, oldval, newval) -> activateIfResourcesAvailable();
        optionalHpoResource.ontologyProperty().addListener(listener);
        optionalMondoResource.ontologyProperty().addListener(listener);
        optionalHpoaResource.directAnnotMapProperty().addListener(listener);
        optionalHpoaResource.indirectAnnotMapProperty().addListener(listener);
        logger.info("Done initialization");
        checkAll();
        logger.info("done activate");
    }

    private static final class FileSearch extends SimpleFileVisitor<Path> {
        List<Path> fileList = new ArrayList();

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException exc) throws IOException {
            if (exc instanceof FileSystemException) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            PathMatcher pm = FileSystems.getDefault().getPathMatcher("glob:obographs-cli-*.jar");
            if (pm.matches(file.getFileName())) {
                fileList.add(file);
            }
            return FileVisitResult.CONTINUE;
        }

        public List<Path> getFileList() {
            return fileList;
        }
    }

    @FXML
    public void loadMondoFile(Event e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Local Mondo JSON File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Mondo JSON File", "*.json"));
        Stage stage = MainApp.mainStage;
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            String mondoJsonPath = file.getAbsolutePath();
            loadMondoFile(mondoJsonPath);
        }
    }

    public void loadMondoFile(String mondoPath) {
        HPOParser parser = new HPOParser(mondoPath);
        ont = parser.getHPO();
        if (ont != null) {
            optionalMondoResource.setOntology(ont);
            pgProperties.setProperty("mondo.json.path", mondoPath);
            logger.info("Loaded Ontology {} from file {}", ont.toString(), mondoPath);
            activateOntologyTree();
            publishMessage("Loaded Ontology from file " + mondoPath);
        }
    }

    public void loadHPOFile(Event e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Local HPO JSON File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("HPO JSON File", "*.json"));
        Stage stage = MainApp.mainStage;
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            String hpoJsonPath = file.getAbsolutePath();
            loadHPOFile(new File(hpoJsonPath));
        }
    }

    void loadHPOFile(File file) {
        System.out.println(file.getAbsolutePath());
        final Ontology ontology = OntologyLoader.loadOntology(file);
        optionalHpoResource.setOntology(ontology);
        pgProperties.setProperty(OptionalHpoResource.HP_JSON_PATH_PROPERTY, file.getAbsolutePath());
    }

    public void loadHPOAFile(Event e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Local phenotype.hpoa File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("HPOA File", "*.hpoa"));
        Stage stage = MainApp.mainStage;
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            String hpoaPath = file.getAbsolutePath();
            loadHPOAFile(hpoaPath);
        }
    }

    void loadHPOAFile(String filePath) {
        if (optionalHpoResource.getOntology() != null ) {
            optionalHpoaResource.setAnnotationResources(filePath, optionalHpoResource.getOntology());
            pgProperties.setProperty(OptionalHpoaResource.HPOA_PATH_PROPERTY, filePath);
        } else {
            PopUps.showInfoMessage("Cannot load phenotype.hpoa file. HPO ontology is null.", "Error loading HPOA");
        }
    }

    @FXML
    public void downloadMondoFile(Event e) throws FileDownloadException {
        downloadMondoFile(pgProperties.getProperty("download.path"));
    }

    public void downloadMondoFile(String path) {
        try {
            BioDownloaderBuilder builder = BioDownloader.builder(Path.of(pgProperties.getProperty("download.path")));
            builder.mondoOwl();
            BioDownloader downloader = builder.build();
            downloader.download();
            File owl = new File(path, "mondo.owl");
            String jarPath = pgProperties.getProperty("obographs.jar.path");
            if (jarPath != null && new File(jarPath).isFile()) {
                String[] command = {"java",  "-jar", jarPath, "convert", "-f", "json", owl.getAbsolutePath()};
                logger.info("Converting mondo.owl to readable mondo.json file using obographs.");
                logger.info(String.join(" ", command));
                convertOboToJson(command);
                loadMondoFile(new File(path, "mondo.json").getAbsolutePath());
            } else {
                logger.info("Cannot find obographs-cli jar file to convert mondo.owl to readable mondo.json file.");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void convertOboToJson(String[] args) throws Exception {
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

    @FXML
    public void showMondoStats(ActionEvent actionEvent) {
        if (ont != null) {
            System.out.println("Mondo Stats for Ontology " + ont + ":\n");
            MondoStats mondo = new MondoStats(ont);
            mondo.run();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Cannot show Mondo Stats. No active Ontology.");
            alert.showAndWait();
        }
    }

    @FXML
    public void saveMapOutputFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Map to File");
        File file = fileChooser.showSaveDialog(MainApp.mainStage);
        pretestMap = makeSelectedDiseaseMap(preTestProb);
        if (file != null && pretestMap != null) {
            new MapFileWriter(pretestMap, file.getAbsolutePath());
        }
    }

    @FXML
    public void showMapInterface(ActionEvent event) {
        makeSelectedDiseaseMap(preTestProb);
        if (mapDisplay == null) {
            mapDisplay = new MapDisplayInterface();
        }
        mapDisplay.launchMapInterface();
        mapDisplay.updateTable();
    }


    @FXML
    void showResourcesInterface(ActionEvent event) {
        try {
            ResourcesController controller = new ResourcesController(optionalHpoResource, optionalHpoaResource,
                    optionalMondoResource, pgProperties, executor);
            Parent parent = FXMLLoader.load(Objects.requireNonNull(ResourcesController.class.getResource("/fxml/ResourcesView.fxml")),
                    null, new JavaFXBuilderFactory(), clazz -> controller);
            Stage stage = new Stage();
            stage.setTitle("Initialize L4CI resources");
            stage.initOwner(ontologyTreeView.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(parent));
            stage.showAndWait();
        } catch (IOException e) {
            logger.warn("Unable to display dialog for setting resources", e);
            PopUps.showException("Initialize L4CI resources", "Error: Unable to display dialog for setting resources", e);
        }
    }

    @FXML
    private void close(ActionEvent e) {
        logger.trace("Closing down");
        Platform.exit();
    }

    @FXML
    public void setLiricalDataDirectory(Event e) {
        setLiricalDataDirectory();
    }

    public void setLiricalDataDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Set LIRICAL Data Directory");
        Stage stage = MainApp.mainStage;
        File directory = directoryChooser.showDialog(stage);
        if (directory != null) {
            String liricalDataPath = directory.getAbsolutePath();
            pgProperties.setProperty("lirical.data.path", liricalDataPath);
            logger.info("Set LIRICAL data directory to {}", directory.getAbsolutePath());
        }
    }

    @FXML
    public void setExomiserVariantFile(Event e) {
        setExomiserVariantFile();
    }

    public void setExomiserVariantFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Set Exomiser Variant File");
        Stage stage = MainApp.mainStage;
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            String exomiserVariantPath = file.getAbsolutePath();
            pgProperties.setProperty("exomiser.variant.path", exomiserVariantPath);
            logger.info("Exomiser Variant File path set to {}", file.getAbsolutePath());
        }
    }

    private void activateIfResourcesAvailable() {
        if (optionalMondoResource.getOntology() != null) { // mondo JSON file is missing
            activateOntologyTree();
        } else {
            logger.error("Could not activate resource");
        }
    }

    /**
     * Post information message to the status bar.
     *
     * @param msg String with message to be displayed
     */
    void publishMessage(String msg) {
        publishMessage(msg, MessageType.INFO);
    }

    /**
     * Post the message to the status bar. Color of the text is determined by the message <code>type</code>.
     *
     * @param msg  String with message to be displayed
     * @param type message type
     */
    private void publishMessage(String msg, MessageType type) {
        int MAX_MESSAGES = 1;
        Platform.runLater(() -> {
            if (statusHBox.getChildren().size() == MAX_MESSAGES) {
                statusHBox.getChildren().remove(MAX_MESSAGES - 1);
            }
            Label label = prepareContainer(type);
            label.setText(msg);
            statusHBox.getChildren().add(0, label);
        });
    }

    /**
     * Make label for displaying message in the {@link #statusHBox}. The style of the text depends on given
     * <code>type</code>
     *
     * @param type of the message to be displayed
     * @return {@link Label} styled according to the message type
     */
    private Label prepareContainer(MessageType type) {
        Label label = new Label();
        label.setPrefHeight(30);
        HBox.setHgrow(label, Priority.ALWAYS);
        label.setPadding(new Insets(5));
        switch (type) {
            case WARNING:
                label.setStyle("-fx-text-fill: orange; -fx-font-weight: bolder");
                break;
            case ERROR:
                label.setStyle("-fx-text-fill: red; -fx-font-weight: bolder");
                break;
            case INFO:
            default:
                label.setStyle("-fx-text-fill: black; -fx-font-weight: bolder");
                break;
        }


        return label;
    }

    public static String getVersion() {
        String version = "0.0.0";// default, should be overwritten by the following.
        try {
            Package p = MainController.class.getPackage();
            version = p.getImplementationVersion();
        } catch (Exception e) {
            // do nothing
        }
        if (version == null) version = "0.0.1"; // this works on a maven build but needs to be reassigned in intellij
        return version;
    }

    /**
     * Check availability of tracked resources and publish an appropriate message.
     */
    private void checkAll() {
        if (optionalHpoResource.getOntology() == null) { // hpo obo file is missing
            publishMessage("hpo json file is missing", MessageType.ERROR);
        } else if (optionalHpoaResource.getDirectAnnotMap() == null) {
            publishMessage("phenotype.hpoa file is missing", MessageType.ERROR);
        } else if (optionalMondoResource.getOntology() == null) {
            publishMessage("mondo json file is missing", MessageType.ERROR);
        } else {
            logger.info("All resources loaded");
            publishMessage("Ready to go", MessageType.INFO);
        }
    }

    /**
     * Initialize the ontology browser-tree in the left column of the app.
     *
     * @param ontology Reference to the HPO or Mondo
     * @param addHook  function hook (currently unused)
     */
    private void initTree(Ontology ontology, Consumer<Term> addHook) {
        // populate the TreeView with top-level elements from ontology hierarchy
        System.out.println(ontology);
        if (ontology == null) {
            ontologyTreeView.setRoot(null);
            return;
        }
        TermId rootId = ontology.getRootTermId();
        Term rootTerm = ontology.getTermMap().get(rootId);
        TreeItem<OntologyTermWrapper> root = new OntologyTermTreeItem(new OntologyTermWrapper(rootTerm));
        root.setExpanded(true);
        ontologyTreeView.setShowRoot(false);
        ontologyTreeView.setRoot(root);

        ontologyTreeView.setCellFactory(tv -> new TreeCell<OntologyTermWrapper>() {

            private void updateTreeIcons(OntologyTermWrapper item, ImageView icon1, ImageView icon2) {
                setGraphic(icon1);
                if (mapDataList != null) {
                    for (MapData mapData : mapDataList) {
                        if (mapData.getTermId().equals(item.term.id()) && mapData.getSliderValue() > 1.0) {
                            setGraphic(icon2);
                        }
                    }
                }
            }

            @Override
            protected void updateItem(OntologyTermWrapper item, boolean empty) {
                ImageView omimIcon = new ImageView(redIcon);
                ImageView omimSelectedIcon = new ImageView(redArrowIcon);
                ImageView selectedIcon = new ImageView(blackArrowIcon);
                super.updateItem(item, empty);
                if (!empty || item != null) {
                    setText(item.term.getName());
                    if (!item.term.getXrefs().stream().filter(r -> r.getName().contains("OMIMPS:")).toList().isEmpty()) {
                        updateTreeIcons(item, omimIcon, omimSelectedIcon);
                    } else if (item.term.getXrefs().stream().filter(r -> r.getName().contains("OMIMPS:")).toList().isEmpty()) {
                        updateTreeIcons(item, null, selectedIcon);
                    }
                }
            }

        });

//        ontologyTreeView.
        ontologyTreeView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    OntologyTermWrapper w;
                    if (newValue == null) {
                        // nothing selected so start are the root.
                        w = new OntologyTermWrapper(rootTerm);
                    } else {
                        w = newValue.getValue();
                    }
                    TreeItem<OntologyTermWrapper> item = new OntologyTermTreeItem(w);
                    updateDescription(item);
                    makeSelectedDiseaseMap(preTestProb);
                    if (mapDisplay != null) {
                        mapDisplay.updateTable();
                    }
                });
        // create Map for lookup of the terms in the ontology based on their Name
        ontology.getTermMap().values().forEach(term -> {
            ontologyLabelsAndTermIdMap.put(term.getName(), term.id());
            ontologyLabelsAndTermIdMap.put(term.id().getValue(), term.id());
        });
        WidthAwareTextFields.bindWidthAwareAutoCompletion(autocompleteTextfield, ontologyLabelsAndTermIdMap.keySet());

        // show intro message in the infoWebView
        Platform.runLater(() -> {
            infoWebEngine = infoWebView.getEngine();
            infoWebEngine.loadContent("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>HPO tree browser</title></head>" +
                    "<body><p>Click on Mondo term in the tree browser to display additional information</p></body></html>");
        });
    }

    /** Function is called once all of the resources are found (hp obo, disease annotations, mondo). */
    public void activateOntologyTree() {
        if (optionalMondoResource.getOntology() == null) {
            logger.error("activateOntologyTree: Mondo null");
        } else {
            final Ontology mondo = optionalMondoResource.getOntology();
            Platform.runLater(()->{
                initTree(mondo, k -> System.out.println("Consumed " + k));
                WidthAwareTextFields.bindWidthAwareAutoCompletion(autocompleteTextfield, ontologyLabelsAndTermIdMap.keySet());
                treeLabel.setText(pgProperties.getProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY));
            });
        }
    }

    public Map<TermId, Double> makeSelectedDiseaseMap(double adjProb) {
        Map<TermId, Double> diseaseMap = new HashMap<>();
        Ontology ontology = optionalMondoResource.getOntology();
        for (Term t : ontology.getTerms()) {
            diseaseMap.put(t.id(), 1.0);
        }
        Set<TermId> selectedTerms = new HashSet<>();
        TreeItem<OntologyTermWrapper> selectedItem = ontologyTreeView.getSelectionModel().getSelectedItem();
        mapDataList = new ArrayList<>();
        if (selectedItem != null) {
            Term selectedTerm = selectedItem.getValue().term;
            selectedTerms.add(selectedTerm.id());
            Set<Term> descendents = getTermRelations(selectedTerm, Relation.DESCENDENT);
            descendents.forEach(t -> selectedTerms.add(t.id()));
            Map<TermId, Double> newMap = PretestProbability.of(diseaseMap, selectedTerms, adjProb);
            boolean addNonSelected = true;
            for (Map.Entry<TermId, Double> entry : newMap.entrySet()) {
                if (selectedTerms.contains(entry.getKey())) {
                    String name = ontology.getTermMap().get(entry.getKey()).getName();
                    MapData mapData = new MapData(name, entry.getKey(), entry.getValue(), adjProb);
                    mapDataList.add(mapData);
                } else if (!selectedTerms.contains(entry.getKey()) && addNonSelected) {
                    String name = ontology.getTermMap().get(entry.getKey()).getName();
                    MapData mapData = new MapData(name, entry.getKey(), entry.getValue(), 1.0);
                    mapDataList.add(mapData);
                    addNonSelected = false;
                }
            }
            return newMap;
        } else {
            TermId id = diseaseMap.keySet().iterator().next();
            String name = ontology.getTermMap().get(id).getName();
            mapDataList.add(new MapData(name, id, diseaseMap.get(id), 1.0));
            return diseaseMap;
        }

    }

    /**
     * Update content of the {@link #infoWebView} with currently selected {@link Term}.
     *
     * @param treeItem currently selected {@link TreeItem} containing {@link Term}
     */
    private void updateDescription(TreeItem<OntologyTermWrapper> treeItem) {
        if (treeItem == null)
            return;
        Term term = treeItem.getValue().term;
//        if (optionalHpoaResource.getIndirectAnnotMap() == null) {
//            logger.error("Attempt to get Indirect annotation map but it was null");
//            return;
//        }
        List<HpoDisease> annotatedDiseases =  new ArrayList<>();//optionalHpoaResource.getIndirectAnnotMap().getOrDefault(term.id(), List.of());
        int n_descendents = 42;//getDescendents(model.getHpoOntology(),term.getId()).size();
        //todo--add number of descendents to HTML
        String content = HpoHtmlPageGenerator.getHTML(term, annotatedDiseases);
        //System.out.print(content);
        // infoWebEngine=this.infoWebView.getEngine();
        infoWebEngine.loadContent(content);
        infoWebEngine.getLoadWorker().stateProperty().addListener(// ChangeListener<Worker.State>
                (observableValue, oldState, newState) -> {
                    logger.trace("TOP OF CHANGED  UPDATE DESCRIPTION");
                    if (newState == Worker.State.SUCCEEDED) {
                        org.w3c.dom.events.EventListener listener = // EventListener
                                (event) -> {
                                    String domEventType = event.getType();
                                    // System.err.println("EventType FROM updateHPO: " + domEventType);
                                    if (domEventType.equals(EVENT_TYPE_CLICK)) {
                                        String href = ((Element) event.getTarget()).getAttribute("href");
                                        // System.out.println("HREF "+href);
                                        if (href.equals("http://www.human-phenotype-ontology.org")) {
                                            return; // the external link is taken care of by the Webengine
                                            // therefore, we do not need to do anything special here
                                        }
                                    }
                                };

                        Document doc = infoWebView.getEngine().getDocument();
                        NodeList nodeList = doc.getElementsByTagName("a");
                        for (int i = 0; i < nodeList.getLength(); i++) {
                            ((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
                        }
                    }
                });

    }

    @FXML
    public void goButtonAction() {
        TermId id = ontologyLabelsAndTermIdMap.get(autocompleteTextfield.getText());
        if (id == null) return; // button was clicked while field was hasTermsUniqueToOnlyOneDisease, no need to do anything
        Ontology hpo = optionalMondoResource.getOntology();
        if (hpo == null) {
            logger.error("goButtonAction: hpo is null");
            return;
        }
        Term term = hpo.getTermMap().get(id);
        if (term == null) {
            logger.error("Could not retrieve HPO term from {}", id.getValue());
            return;
        }
        expandUntilTerm(term);
        autocompleteTextfield.clear();
    }

    private void updateLimits(Slider slider, double min, double max) {
        slider.setMin(min);
        slider.setMax(max);
        double range = max - min;
        double incValue = range/4;
        slider.setMajorTickUnit(incValue);
    }

    private void adjustLimits(Slider slider, double value) {
        double curMin = slider.getMin();
        double curMax = slider.getMax();
        if (value > curMax) {
            double newMax = value * 2.0;
            while (newMax < value) {
                newMax *= 2.0;
            }
            updateLimits(slider, curMin, newMax);
            slider.setValue(value);
        } else if (value <= curMax / 4.0) {
            double newMax = curMax / 4.0;
            updateLimits(slider, curMin, newMax);
            slider.setValue(value);
        }
    }

    private void sliderAction() {
        preTestProb = probSlider.getValue();
        sliderTextField.setText(String.format("%.2f", preTestProb));
        makeSelectedDiseaseMap(preTestProb);
        if (mapDisplay != null) {
            mapDisplay.updateTable();
        }
    }

    @FXML
    public void liricalButtonAction(ActionEvent actionEvent) throws LiricalParseException {
        System.out.println("Pretest Probability = " + preTestProb);
        Map<TermId, Double> preTestMap = makeSelectedDiseaseMap(preTestProb);
        AnalysisData analysisData = prepareAnalysisData(lirical, preTestMap);
        AnalysisOptions analysisOptions = AnalysisOptions.of(false, PretestDiseaseProbability.of(preTestMap));
        LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
        AnalysisResults results = analysisRunner.run(analysisData, analysisOptions);
        System.out.println(results);
    }

    protected AnalysisData prepareAnalysisData(Lirical lirical, Map<TermId, Double> pretestMap) throws LiricalParseException {
//        LOGGER.info("Reading phenopacket from {}.", phenopacketPath.toAbsolutePath());
//
//        PhenopacketData data = null;
//        try (InputStream is = Files.newInputStream(phenopacketPath)) {
//            PhenopacketImporter v2 = PhenopacketImporters.v2();
//            data = v2.read(is);
//            LOGGER.info("Success!");
//        } catch (IOException e) {
//            LOGGER.info("Unable to parse as v2 phenopacket, trying v1.");
//        }
//
//        if (data == null) {
//            try (InputStream is = Files.newInputStream(phenopacketPath)) {
//                PhenopacketImporter v1 = PhenopacketImporters.v1();
//                data = v1.read(is);
//            } catch (IOException e) {
//                LOGGER.info("Unable to parser as v1 phenopacket.");
//                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
//            }
//        }
//
//        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
//        List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
//        List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
//
//        // Read VCF file.
//        GenesAndGenotypes genes;
//        // Path to VCF set via CLI has priority.
//        Path vcfPath = this.vcfPath != null
//                ? this.vcfPath
//                : data.getVcfPath().orElse(null);
//        String sampleId = data.getSampleId();
//        if (vcfPath == null) {
//            genes = GenesAndGenotypes.empty();
//        } else {
//            genes = readVariantsFromVcfFile(sampleId, vcfPath, lirical.variantParserFactory());
//        }
//        return AnalysisData.of(sampleId,
//                data.getAge().orElse(null),
//                data.getSex().orElse(null),
//                presentTerms,
//                excludedTerms,
//                genes);
        String sampleId = "1";
        List<TermId> presentTerms = pretestMap.keySet().stream().toList();
        List<TermId> excludedTerms = new ArrayList<>();
        GenesAndGenotypes genes = GenesAndGenotypes.empty();
        return AnalysisData.of(sampleId, null, null, presentTerms, excludedTerms, genes);
    }

    /**
     * Find the path from the root term to given {@link Term}, expand the tree and set the selection model of the
     * TreeView to the term position.
     *
     * @param term {@link Term} to be displayed
     */
    private void expandUntilTerm(Term term) {
        // logger.trace("expand until term " + term.toString());
        Ontology ontology = optionalMondoResource.getOntology();
        if (ontology == null) {
            logger.error("expandUntilTerm not possible because ontology was null");
            return;
        }
        if (existsPathFromRoot(term)) {
            // find root -> term path through the tree
            Stack<Term> termStack = new Stack<>();
            termStack.add(term);
            Set<Term> parents = getTermRelations(term, Relation.PARENT);
            while (parents.size() != 0) {
                Term parent = parents.iterator().next();
                termStack.add(parent);
                parents = getTermRelations(parent, Relation.PARENT);
            }

            // expand tree nodes in top -> down direction
            List<TreeItem<OntologyTermWrapper>> children = ontologyTreeView.getRoot().getChildren();
            termStack.pop(); // get rid of 'All' node which is hidden
            TreeItem<OntologyTermWrapper> target = ontologyTreeView.getRoot();
            while (!termStack.empty()) {
                Term current = termStack.pop();
                for (TreeItem<OntologyTermWrapper> child : children) {
                    if (child.getValue().term.equals(current)) {
                        child.setExpanded(true);
                        target = child;
                        children = child.getChildren();
                        break;
                    }
                }
            }
            ontologyTreeView.getSelectionModel().select(target);
            ontologyTreeView.scrollTo(ontologyTreeView.getSelectionModel().getSelectedIndex());
        } else {
            TermId rootId = ontology.getRootTermId();
            Term rootTerm = ontology.getTermMap().get(rootId);
            logger.warn(String.format("Unable to find the path from %s to %s", rootTerm.toString(), term.getName()));
        }
        selectedTerm = term;
    }

    /**
     * Get the relations of "term"
     *
     * @param term Mondo Term of interest
     * @param relation Relation of interest (ancestor, descendent, child, parent)
     * @return relations of term (not including term itself).
     */
    private Set<Term> getTermRelations(Term term, Relation relation) {
        Ontology ontology = optionalMondoResource.getOntology();
        if (ontology == null) {
            logger.error("Ontology null");
            PopUps.showInfoMessage("Error: Could not initialize Ontology", "ERROR");
            return Set.of();
        }
        TermId termId = term.id();
        Set<TermId> relationIds;
        switch (relation) {
            case ANCESTOR:
                relationIds = getAncestorTerms(ontology, termId, false);
                break;
            case DESCENDENT:
                relationIds = getDescendents(ontology, termId);
                break;
            case CHILD:
                relationIds = getChildTerms(ontology, termId, false);
                break;
            case PARENT:
                relationIds = getParentTerms(ontology, termId, false);
                break;
            default:
                return Set.of();
        }
        Set<Term> relations = new HashSet<>();
        relationIds.forEach(tid -> {
            Term ht = ontology.getTermMap().get(tid);
            relations.add(ht);
        });
        return relations;
    }

    private boolean existsPathFromRoot(Term term) {
        Ontology ontology = optionalMondoResource.getOntology();
        if (ontology == null) {
            logger.error("Ontology null");
            PopUps.showInfoMessage("Error: Could not initialize Ontology", "ERROR");
            return false;
        }
        return existsPath(ontology, term.id(), ontology.getRootTermId());
    }

    /**
     * Inner class that defines a bridge between hierarchy of {@link Term}s and {@link TreeItem}s of the
     * {@link TreeView}.
     */
    class OntologyTermTreeItem extends TreeItem<OntologyTermWrapper> {
        /** List used for caching of the children of this term */
        private ObservableList<TreeItem<OntologyTermWrapper>> childrenList;
        /**
         * Default & only constructor for the TreeItem.
         *
         * @param term {@link Term} that is represented by this TreeItem
         */
        OntologyTermTreeItem(OntologyTermWrapper term) {
            super(term);
        }
        /**
         * Check that the {@link Term} that is represented by this TreeItem is a leaf term as described below.
         * <p>
         * {@inheritDoc}
         */
        @Override
        public boolean isLeaf() {
            return getTermRelations(getValue().term, Relation.CHILD).size() == 0;
        }


        /**
         * Get list of children of the {@link Term} that is represented by this TreeItem.
         * {@inheritDoc}
         */
        @Override
        public ObservableList<TreeItem<OntologyTermWrapper>> getChildren() {
            if (childrenList == null) {
                // logger.debug(String.format("Getting children for term %s", getValue().term.getName()));
                childrenList = FXCollections.observableArrayList();
                Set<Term> children = getTermRelations(getValue().term, Relation.CHILD);
                Comparator<Term> compByChildrenSize = (term1, term2) -> getTermRelations(term2, Relation.CHILD).size() - getTermRelations(term1, Relation.CHILD).size();
                children.stream()
                        .sorted(compByChildrenSize.thenComparing(Term::getName))
                        .map(term -> new OntologyTermTreeItem(new OntologyTermWrapper(term)))
                        .forEach(childrenList::add);
                super.getChildren().setAll(childrenList);
            }
            return super.getChildren();
        }
    }

}

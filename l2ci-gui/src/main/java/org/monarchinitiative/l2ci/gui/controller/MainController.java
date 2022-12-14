package org.monarchinitiative.l2ci.gui.controller;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.monarchinitiative.l2ci.core.io.PretestProbabilityMultiplierIO;
import org.monarchinitiative.l2ci.core.mondo.MondoStats;
import org.monarchinitiative.l2ci.gui.*;
import org.monarchinitiative.l2ci.gui.config.AppProperties;
import org.monarchinitiative.l2ci.gui.model.PretestProbability;
import org.monarchinitiative.l2ci.gui.resources.*;
import org.monarchinitiative.l2ci.gui.model.DiseaseWithSliderValue;
import org.monarchinitiative.l2ci.gui.ui.MondoTreeView;
import org.monarchinitiative.l2ci.gui.ui.OntologyTermWrapper;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

@Component
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    // A pattern for matching either a float with a trailing `.` (e.g. `123.`)
    // or a float with optional whole number part followed by a dot and an arbitrary number of decimal places
    // (e.g. `.1`, `.123`, `1.234`).
    private static final Pattern NONNEGATIVE_FLOAT = Pattern.compile("(\\d+\\.?)|(\\d*\\.\\d+)");

    // Default slider value is 1. and it must match the probability in the `MainView.fxml`.
    private static final double DEFAULT_SLIDER_VALUE = 1.;

    private static final String EVENT_TYPE_CLICK = "click";

    /**
     * Application-specific properties (not the System properties!) defined in the 'application.properties' file that
     * resides in the classpath.
     */
    public final Properties pgProperties;
    private final AppProperties appProperties;
    private final OptionalResources optionalResources;
    private final OptionalServices optionalServices;
    private final ExecutorService executor;
    private final UrlBrowser urlBrowser;
    private final Path dataDirectory;

    @FXML
    private BorderPane contentPane;

    @FXML
    private MenuItem showMondoStats;

    @FXML
    private Label copyrightLabel;
    /**
     * Place at the bottom of the window for showing messages to user
     */
    @FXML
    public HBox statusHBox;
    @FXML
    private AutoCompleteOntologyTextField autocompleteTextField;
    // TODO - get the OMIM file and populate the collections that back the search.
    @FXML
    private AutoCompleteOntologyTextField autocompleteOmimTextField;

    /**
     * WebView for displaying details of the Term that is selected in the {@link #mondoTreeView}.
     */
    @FXML
    private WebView infoWebView;
    /**
     * WebEngine backing up the {@link #infoWebView}.
     */
    private WebEngine infoWebEngine;

    @FXML
    private MondoTreeView mondoTreeView;

    // A flag for syncing slider pretest proba updates.
    private boolean updatingPretestProba = false;
    private final DoubleProperty sliderValue = new SimpleDoubleProperty(DEFAULT_SLIDER_VALUE);
    /**
     * Slider to adjust pretest probability before running LIRICAL
     */
    @FXML
    private Slider pretestProbaSlider;
    @FXML
    private TextField pretestProbaTextField;
    private TextFormatter<Double> pretestProbaFormatter;
    @FXML
    private Button vcfButton;
    @FXML
    private Button liricalButton;
    @FXML
    private Label treeLabel;
    @FXML
    private TextField outputFileTextField;
    @FXML
    private Label outputFileTypeLabel;
    @FXML
    private TextField lrThresholdTextField;
    @FXML
    private Spinner minDiagnosisSpinner;
    @FXML
    private TextField pathogenicityTextField;
    @FXML
    private CheckBox variantsCheckbox;

    @FXML
    private Label phenopacketLabel;
    @FXML
    private Label vcfLabel;


    private enum MessageType {
        INFO, WARNING, ERROR
    }

    public MainController(OptionalResources optionalResources,
                          OptionalServices optionalServices,
                          AppProperties appProperties,
                          @Qualifier("configProperties") Properties properties,
                          ExecutorService executorService,
                          UrlBrowser urlBrowser,
                          Path dataDirectory) {
        this.optionalResources = optionalResources;
        this.optionalServices = optionalServices;
        this.appProperties = appProperties;
        this.pgProperties = properties;
        this.executor = executorService;
        this.urlBrowser = urlBrowser;
        this.dataDirectory = dataDirectory;
    }

    @FXML
    private void initialize() {
        SpinnerValueFactory<Integer> spinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 10);
        minDiagnosisSpinner.setValueFactory(spinnerFactory);
        pathogenicityTextField.setText(pgProperties.getProperty("pathogenicity.threshold"));
        variantsCheckbox.setSelected(false);

        // TODO - address
        outputFileTypeLabel.setText("." + pgProperties.getProperty("output.formats"));

        showMondoStats.disableProperty().bind(optionalServices.mondoProperty().isNull());
        copyrightLabel.setText("L4CI, v. " + appProperties.getVersion() + ", © Monarch Initiative 2022");

        // ---------- Autocompletion fields ----------
        // Mondo disease name autocomplete
        autocompleteTextField.ontologyProperty().bind(optionalServices.mondoProperty());
        // TODO - setup OMIM autocompletion

        // ------------- Slider UI fields ------------
        pretestProbaFormatter = proparePretestProbabilityFormatter(pretestProbaSlider.getMin(), pretestProbaSlider.getMax(), DEFAULT_SLIDER_VALUE);
        pretestProbaTextField.setTextFormatter(pretestProbaFormatter);

        InvalidationListener keepSliderValuesInSync = updateSliderValuesInTheUi();
        pretestProbaFormatter.valueProperty().addListener(keepSliderValuesInSync);
        pretestProbaSlider.valueProperty().addListener(keepSliderValuesInSync);
        sliderValue.addListener(keepSliderValuesInSync);

        // Show path to Mondo file
        treeLabel.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    Path mondoPath = optionalResources.ontologyResources().getMondoPath();
                    return mondoPath == null ? "Unset" : mondoPath.toAbsolutePath().toString();
                    },
                optionalResources.ontologyResources().mondoPathProperty()));

        // Set up the Mondo tree
        mondoTreeView.disableProperty().bind(optionalServices.mondoProperty().isNull());
        mondoTreeView.mondoProperty().bind(optionalServices.mondoProperty());
        // TODO - change mondoTreeView children to descendents and update the map when the data becomes available.
//        mondoTreeView.mondoNDescendentsProperty().bind(optionalServices.mondoOmimResources().mondoNDescendentsProperty());
        mondoTreeView.getSelectionModel().selectedItemProperty()
                .addListener((observable, previousMondoItem, newMondoItem) -> {
                    if (previousMondoItem != null)
                        // We must unbind the previous slider value on the tree item so that we do not update it.
                        previousMondoItem.getValue().sliderValueProperty().unbind();

                    if (newMondoItem != null) {
                        // Next, we update the slider UI elements with the value of the new item and bind it,
                        // to track the user activity.
                        sliderValue.setValue(newMondoItem.getValue().getSliderValue());
                        newMondoItem.getValue().sliderValueProperty().bind(sliderValue);
                    }

                    // Finally, we update the term description in the right panel
                    updateDescription(newMondoItem);
                });

        // TODO - we need both lirical and known disease IDs to run this. Add the corresponding binding.
        liricalButton.disableProperty().bind(optionalServices.liricalProperty().isNull());


        // TODO - remove when Mondo file is fixed. Thus should not be required when the app goes out.
        if (pgProperties.getProperty("obographs.jar.path") == null) {
            String mainDir = new File(".").getAbsolutePath();
            String obographsPath = String.join(File.separator, mainDir.substring(0, mainDir.length()-2), "obographs-cli-0.3.0.jar");
            pgProperties.setProperty("obographs.jar.path", obographsPath);
        }
        LOGGER.info("obographs jar located at " + pgProperties.getProperty("obographs.jar.path"));

        // show intro message in the infoWebView
        // TODO - move into initialize
        Platform.runLater(() -> {
            infoWebEngine = infoWebView.getEngine();
            infoWebEngine.loadContent("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>HPO tree browser</title></head>" +
                    "<body><p>Click on Mondo term in the tree browser to display additional information</p></body></html>");
        });
    }

    private static TextFormatter<Double> proparePretestProbabilityFormatter(double min, double max, double defaultValue) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            Matcher matcher = NONNEGATIVE_FLOAT.matcher(change.getControlNewText());
            if (matcher.matches()) {
                double proba = Double.parseDouble(change.getControlNewText());
                if (min <= proba && proba < max) {
                    return change; // valid input
                }
            }
            change.setText("");
            change.setRange(change.getRangeStart(), change.getRangeEnd());
            return change;
        };
        return new TextFormatter<>(new RoundingDoubleStringConverter(), defaultValue, filter);
    }

    private InvalidationListener updateSliderValuesInTheUi() {
        return obs -> {
            if (updatingPretestProba)
                return;
            try {
                updatingPretestProba = true;
                if (obs.equals(pretestProbaTextField.getTextFormatter().valueProperty())) {
                    // The text field changed, we must update the slider
                    // We're practically sure the value is a valid double since we have a filter on the text formatter
                    pretestProbaSlider.setValue(pretestProbaFormatter.getValue());
                    sliderValue.set(pretestProbaFormatter.getValue());
                } else if (obs.equals(pretestProbaSlider.valueProperty())) {
                    // The slider changed, we must update the text field.
                    pretestProbaFormatter.setValue(pretestProbaSlider.getValue());
                    sliderValue.set(pretestProbaSlider.getValue());
                } else if (obs.equals(sliderValue)) {
                    // The sliderValue changed, we must update both the text field and the slider
                    pretestProbaSlider.setValue(sliderValue.getValue());
                    pretestProbaFormatter.setValue(sliderValue.getValue());
                } else {
                    LOGGER.warn("Unknown observable changed: {}", obs);
                }
            } finally {
                updatingPretestProba = false;
            }
        };
    }

    private Stage progressWindow(Task<?> task, String taskMessage) {
        ProgressIndicator pb = new ProgressIndicator();
        pb.setProgress(0);
        pb.progressProperty().unbind();
        pb.progressProperty().bind(task.progressProperty());
        Stage window = PopUps.setupProgressDialog("Initializing", taskMessage + "...", pb);
        window.show();
        window.toFront();
        window.setAlwaysOnTop(true);
        return window;
    }

    private void updateProgressMessage(Stage window, Task task) {
        Label progressLabel = (Label) window.getScene().getRoot().getChildrenUnmodifiable().get(0);
        progressLabel.setMaxWidth(325);
        progressLabel.setWrapText(true);
        task.messageProperty().addListener((observable, oldValue, newValue) -> {
            progressLabel.setText(newValue);
        });
    }

//    private void showProgress(Task task, String type, String taskMessage) {
//        publishMessage(taskMessage);
//        Stage window = progressWindow(task, taskMessage);
//        switch (type) {
//            case "startup" -> {
//                updateProgressMessage(window, task);
//                task.setOnSucceeded(e -> {
//                    Ontology mondoOnt = optionalMondoResource.getOntology();
//                    Ontology hpoOnt = optionalHpoResource.getOntology();
//                    boolean hpoaEmpty = optionalHpoaResource.getDirectAnnotMap().isEmpty();
//                    if (mondoOnt != null && hpoOnt != null && !hpoaEmpty) {
//                        MapBuildTask mapTask = new MapBuildTask(optionalMondoResource, pgProperties);
//                        Stage mapWindow = progressWindow(mapTask, "Making Map Files");
//                        updateProgressMessage(mapWindow, mapTask);
//                        mapTask.setOnSucceeded(w -> mapWindow.close());
//                        this.executor.submit(mapTask);
//                        logger.info("Activating Ontology Tree.");
//                        activateOntologyTree();
//                        autocompleteTextfield.setOntology(mondoOnt);
//                        autocompleteOmimTextfield.setOmimMap(omimLabelsAndMondoTermIdMap);
//                        publishMessage("Finished " + taskMessage);
//                    } else {
//                        StringBuilder msg = new StringBuilder();
//                        Ontology[] ontologies = {mondoOnt, hpoOnt};
//                        String[] ontTypes = {"Mondo", "HPO"};
//                        for (int i = 0; i < ontologies.length; i++) {
//                            Ontology ontology = ontologies[i];
//                            String ontType = ontTypes[i];
//                            if (ontology == null) {
//                                msg.append("\nNeed to set path to ").append(ontType).append(" file (See File -> Show Resources menu)");
//                            }
//                        }
//                        if (hpoaEmpty) {
//                            msg.append("\nNeed to set path to phenotype.hpoa file (See File -> Show Resources menu)");
//                        }
//                        PopUps.showInfoMessage(msg.toString(), "Error Intializing Ontologies");
//                    }
//                    window.close();
//                });
//            }
//            case "lirical" -> task.setOnSucceeded(e -> {
//                if (lirical != null) {
//                    if (lirical.variantParserFactory().isEmpty()) {
//                        PopUps.showInfoMessage("Path to Exomiser variant file not set (see File -> Show Resources menu). Variants will not be annotated in the current LIRICAL instance. \n\nTo annotate variants, set the path to the Exomiser variant file, and restart the program.", "Missing LIRICAL Resource");
//                    }
//                    liricalAnalysis = new LiricalAnalysis(lirical, pgProperties);
//                    liricalButton.setDisable(false);
//                    publishMessage("Finished " + taskMessage);
//                } else {
//                    System.out.println(task.getException().toString());
//                    publishMessage("Failed " + taskMessage + ". LIRICAL instance is null.", MessageType.ERROR);
//                }
//                window.close();
//            });
//        }
//        task.setOnFailed(e -> {
//            String msg = "Failed " + taskMessage;
//            PopUps.showInfoMessage(msg, "Error Intializing Resources");
//            publishMessage(msg, MessageType.ERROR);
//            window.close();
//        });
//        this.executor.submit(task);
//    }

//    @FXML
//    public void loadMondoFile(Event e) {
//        FileChooser fileChooser = new FileChooser();
//        fileChooser.setTitle("Import Local Mondo JSON File");
//        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Mondo JSON File", "*.json"));
//        File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
//        if (file != null) {
//            String mondoJsonPath = file.getAbsolutePath();
//            loadMondoFile(mondoJsonPath);
//        }
//    }

//    public void loadMondoFile(String mondoPath) {
//        HPOParser parser = new HPOParser(mondoPath);
//        ont = parser.getHPO();
//        if (ont != null) {
////            optionalMondoResource.setOntology(ont);
//            pgProperties.setProperty("mondo.json.path", mondoPath);
//            pgProperties.setProperty(OptionalResources.MONDO_JSON_PATH_PROPERTY, mondoPath);
//            logger.info("Loaded Ontology {} from file {}", ont.toString(), mondoPath);
////            MapBuildTask mapTask = new MapBuildTask(optionalMondoResource, pgProperties);
////            Stage mapWindow = progressWindow(mapTask, "Making Map Files");
////            updateProgressMessage(mapWindow, mapTask);
////            mapTask.setOnSucceeded(w -> mapWindow.close());
////            this.executor.submit(mapTask);
//            publishMessage("Loaded Ontology from file " + mondoPath);
//        }
//    }


    @FXML
    private void showMondoStatsAction(ActionEvent e) {
        MondoStats mondo = new MondoStats(optionalServices.getMondo());
        mondo.run();
        e.consume();
    }

    @FXML
    private void saveMapOutputFile(ActionEvent e) {
        // Ask the user for the output file.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Pretest Probability Adjustments to File");
        File file = fileChooser.showSaveDialog(contentPane.getScene().getWindow());
        if (file == null)
            // The user chose to cancel.
            return;

        // Dump the adjustments to the file.
        try {
            PretestProbabilityMultiplierIO.write(mondoTreeView.sliderValuesProperty(), file.toPath());
        } catch (IOException ex) {
            LOGGER.warn("Unable to write the pretest probability adjustments to {}", file.toPath().toAbsolutePath(), ex);
            PopUps.showException("Save Pretest Probability Adjustments", "Unable to save the data", ex);
        }
        e.consume();
    }

    @FXML
    private void loadMapOutputFile(ActionEvent e) {
        // Ask the user for the input file.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Pretest Probability Adjustments from File");
        File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
        if (file == null)
            // The user chose to cancel.
            return;

        // Reset the Mondo tree and set the loaded adjustments.
        try {
            // TODO - we may need more elaborate code to implement adjustment setting
            Map<TermId, Double> adjustments = PretestProbabilityMultiplierIO.read(file.toPath());
            mondoTreeView.sliderValuesProperty().clear();
            mondoTreeView.sliderValuesProperty().putAll(adjustments);
        } catch (IOException ex) {
            LOGGER.warn("Unable to load the pretest probability adjustments from {}", file.toPath().toAbsolutePath(), ex);
            PopUps.showException("Load Pretest Probability Adjustments", "Unable to load the data", ex);
        }
        e.consume();
    }

    @FXML
    private void showMapInterface(ActionEvent e) {
        ObservableList<DiseaseWithSliderValue> source = mondoTreeView.drainSliderValues()
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        MapDisplay mapDisplay = new MapDisplay();
        Bindings.bindContent(mapDisplay.mondoToOmimProperty(), optionalServices.mondoOmimResources().mondoToOmimProperty());
        Bindings.bindContent(mapDisplay.getItems(), source);
        Stage stage = new Stage();
        stage.setTitle("Probability Map");
        stage.setScene(new Scene(mapDisplay));
        stage.showAndWait();

        Bindings.unbindContent(mapDisplay.getItems(), source);
        Bindings.bindContent(mapDisplay.mondoToOmimProperty(), optionalServices.mondoOmimResources().mondoToOmimProperty());
        e.consume();
    }


    @FXML
    private void showResourcesInterface(ActionEvent e) {
        try {
            ResourcesController controller = new ResourcesController(optionalResources, dataDirectory, executor);
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(ResourcesController.class.getResource("ResourcesView.fxml")));
            loader.setControllerFactory(clz -> controller);
            Stage stage = new Stage();
            stage.setTitle("Initialize L4CI resources");
            stage.initOwner(contentPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait();
        } catch (IOException ex) {
            LOGGER.warn("Unable to display dialog for setting resources", ex);
            PopUps.showException("Initialize L4CI resources", "Error: Unable to display dialog for setting resources", ex);
        }
        e.consume();
    }

    @FXML
    private void close() {
        LOGGER.trace("Closing down");
        Platform.exit();
    }

//    @FXML
//    private void setLiricalResultsDirectory() {
//        String homeDir = System.getProperty("user.home");
//        String initialDir = pgProperties.getProperty("lirical.results.path").equals("unset") ? homeDir : pgProperties.getProperty("lirical.results.path");
//        File dir = PopUps.selectDirectory(contentPane.getScene().getWindow(), new File(initialDir), "Choose Directory to Save Results");
//        if (dir != null) {
//            pgProperties.setProperty("lirical.results.path", dir.getAbsolutePath());
//        }
//    }

//    public void setExomiserVariantFile() {
//        String homeDir = System.getProperty("user.home");
//        String initialDir = pgProperties.getProperty("exomiser.variant.path").equals("unset") ? homeDir :
//                new File(pgProperties.getProperty("exomiser.variant.path")).getParentFile().getAbsolutePath();
//        File file = PopUps.selectFileToOpen(contentPane.getScene().getWindow(), new File(initialDir), "Set Exomiser Variant File");
//        if (file != null) {
//            String exomiserVariantPath = file.getAbsolutePath();
//            pgProperties.setProperty("exomiser.variant.path", exomiserVariantPath);
//            logger.info("Exomiser Variant File path set to {}", file.getAbsolutePath());
//        }
//    }

//    public void setBackgroundFrequencyFile() {
//        String homeDir = System.getProperty("user.home");
//        String initialDir = pgProperties.getProperty("background.frequency.path").equals("unset") ? homeDir :
//                new File(pgProperties.getProperty("background.frequency.path")).getParentFile().getAbsolutePath();
//        File file = PopUps.selectFileToOpen(contentPane.getScene().getWindow(), new File(initialDir), "Set Background Frequency File");
//        if (file != null) {
//            String bkgFreqPath = file.getAbsolutePath();
//            pgProperties.setProperty("background.frequency.path", bkgFreqPath);
//            LOGGER.info("Background Frequency File path set to {}", file.getAbsolutePath());
//        }
//    }


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


//    /**
//     * Check availability of tracked resources and publish an appropriate message.
//     */
//    private void checkAll() {
//        if (optionalHpoResource.getOntology() == null) { // hpo obo file is missing
//            publishMessage("hpo json file is missing", MessageType.ERROR);
//        } else if (optionalHpoaResource.getDirectAnnotMap() == null) {
//            publishMessage("phenotype.hpoa file is missing", MessageType.ERROR);
//        } else if (optionalMondoResource.getOntology() == null) {
//            publishMessage("mondo json file is missing", MessageType.ERROR);
//        } else {
//            logger.info("All resources loaded");
//            publishMessage("Ready to go", MessageType.INFO);
//        }
//    }

//    /**
//     * Initialize the ontology browser-tree in the left column of the app.
//     *
//     */
//    private ChangeListener<? super Ontology> initMondoTree() {
//        return (obs, old, mondo) -> {
//            mondoTreeView.mondoProperty().set(mondo);
////            if (mondo == null) {
////                mondoTreeView.setCellFactory(null);
////            } else {
////                mondoTreeView.setCellFactory(tw -> new MondoTreeCell(mapDataList, mondoNDescendantsMap));
////            }
////
//            // TODO - WE ma?
////        root.getChildren().remove(1, root.getChildren().size());
////        TreeItem<OntologyTermWrapper> diseasesTreeItem = root.getChildren().get(0);
////        diseasesTreeItem.getChildren().remove(1, diseasesTreeItem.getChildren().size());
////        List<TreeItem<OntologyTermWrapper>> mendelianDiseases = diseasesTreeItem.getChildren().get(0).getChildren();
//
//
//
//            // TODO - tweak width, #rows
////        AutoCompletionBinding<String> mondoLabelBinding = TextFields.bindAutoCompletion(autocompleteTextfield, ontologyLabelsAndTermIdMap.keySet());
////        AutoCompletionBinding<TermId> omimBinding = TextFields.bindAutoCompletion(autocompleteOmimTextfield, omimToMondoMap.keySet());
////        omimBinding.prefWidthProperty().bind(autocompleteOmimTextfield.widthProperty());
//
//
//        };
//
//    }
//
//    /** Function is called once all of the resources are found (hp obo, disease annotations, mondo). */
//    public void activateOntologyTree() {
//        if (optionalMondoResource.getOntology() == null) {
//            logger.error("activateOntologyTree: Mondo null");
//        } else {
//            final Ontology mondo = optionalMondoResource.getOntology();
//            Platform.runLater(()->{
//                initMondoTree(mondo);
//                // TODO - tweak width, #rows
////                AutoCompletionBinding<String> mondoLabelBinding = TextFields.bindAutoCompletion(autocompleteTextfield, ontologyLabelsAndTermIdMap.keySet());
////                treeLabel.setText(pgProperties.getProperty(OptionalMondoResource.MONDO_JSON_PATH_PROPERTY));
//            });
//        }
//    }





//    private HashMap<TermId, TermId> makeDescendentsMap(TermId mondoId, Set<TermId> omimIDs) {
//        HashMap<TermId, TermId> selectedTerms = new HashMap<>();
//        for (TermId omimID : omimIDs) {
////            List<TermId> mondoIDs = omimToMondoMap.get(omimID);
////            if (mondoIDs.contains(mondoId)) {
//                Set<Term> descendents = getTermRelations(mondoId, Relation.DESCENDENT);
//                for (Term descendent : descendents) {
//                    for (TermId omimID2 : omimIDs) {
//                        List<TermId> mondoIDs2 = omimToMondoMap.get(omimID2);
//                        if (mondoIDs2.contains(descendent.id())) {
//                            selectedTerms.put(omimID2, descendent.id());
//                            break;
//                        }
//                    }
//                }
////            }
//        }
//        return selectedTerms;
//    }


    // TODO(mabeckwith) - consider removing the method if it proves to have become redundant.
//    private Map<TermId, Double> makeSelectedDiseaseMap() {
//        Map<TermId, Double> omimToPretestProbability = new HashMap<>();
//
//        MondoOmimResources mm = optionalServices.mondoOmimResources();
//        Map<TermId, TermId> mondoToOmim = mm.getMondoToOmim();
//
//        for (TermId omimId : mm.getOmimToMondo().keySet()) {
//            omimToPretestProbability.put(omimId, DEFAULT_SLIDER_VALUE);
//        }
//        mondoTreeView.drainSliderValues()
//                .filter(md -> md.getSliderValue() >= DEFAULT_SLIDER_VALUE)
//                /*
//                  Here we update OMIM -> pretest proba map.
//                  However, the `mondoTreeView` provides, well, Mondo IDs. Hence, we first map
//                  to the corresponding OMIM (if any) and set the pretest probability found on a tree node.
//                 */
//                .forEach(d -> {
//                    TermId omimId = mondoToOmim.get(d.id());
//                    if (omimId != null) {
//                        omimToPretestProbability.compute(omimId,
//                                (OMIM_ID, defaultProba) -> d.getSliderValue()
//                        );
//                    }
//                });
//
//        return omimToPretestProbability;
//    }

//    private void addToMapData(Ontology ontology, TermId mondoID, TermId omimID, Double probValue, Double sliderValue, boolean isFixed) {
//        String name = "";
//        if (mondoID != null & ontology != null) {
//            name = ontology.getTermMap().get(mondoID).getName();
//        } else if (ontology == null) {
//            name = "other diseases"; //optionalHpoaResource.getId2diseaseModelMap().get(omimID).diseaseName();
//        }
//        List<TermId> mapMondoIds = new ArrayList<>();
//        for (MapData mapData : mapDataList) {
//            mapMondoIds.add(mapData.getMondoId());
//        }
//        if (!mapMondoIds.contains(mondoID)) {
//            mapDataList.add(new MapData(name, mondoID, omimID, probValue, sliderValue, isFixed));
//        }

    /**
     * Update content of the {@link #infoWebView} with currently selected {@link Term}.
     *
     * @param treeItem currently selected {@link TreeItem} containing {@link Term}
     */
    private void updateDescription(TreeItem<OntologyTermWrapper> treeItem) {
        if (treeItem == null)
            // TODO - handle properly. Null means no display
            return;

        Term term = treeItem.getValue().term();

        List<HpoDisease> annotatedDiseases = List.of();
//        List<HpoDisease> annotatedDiseases =  optionalHpoaResource.getIndirectAnnotMap().getOrDefault(term.id(), List.of());
        int n_descendents = 42;//getDescendents(model.getHpoOntology(),term.getId()).size();
        //todo--add number of descendents to HTML
        Double pretestProba = treeItem.getValue().getSliderValue();
        String content = HpoHtmlPageGenerator.getHTML(term, annotatedDiseases, pretestProba);
        //System.out.print(content);
        infoWebEngine=this.infoWebView.getEngine();
        infoWebEngine.loadContent(content);
        infoWebEngine.getLoadWorker().stateProperty().addListener(// ChangeListener<Worker.State>
                (observableValue, oldState, newState) -> {
                    LOGGER.trace("TOP OF CHANGED  UPDATE DESCRIPTION");
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
    private void goButtonAction() {
        Optional<TermId> opt = autocompleteTextField.getSelectedId();
        if (opt.isPresent()) {
            TermId id = opt.get();
            goToTerm(id);
        } else {
            LOGGER.warn("Unable to get term id: ");
        }
        autocompleteTextField.clear();
    }

    @FXML
    private void omimButtonAction() {
        Optional<TermId> opt = autocompleteOmimTextField.getSelectedId();
        if (opt.isPresent()) {
            TermId id = opt.get();
            goToTerm(id);
        } else {
            LOGGER.warn("Unable to get term id: ");
        }
        autocompleteOmimTextField.clear();
    }

    private void goToTerm(TermId id) {
        if (id == null) return; // button was clicked while field was hasTermsUniqueToOnlyOneDisease, no need to do anything
        Ontology mondo = optionalServices.getMondo();

        Term term = mondo.getTermMap().get(id);
        if (term == null) {
            LOGGER.error("Could not retrieve Mondo term from {}", id.getValue());
            return;
        }
        mondoTreeView.expandUntilTerm(term);
    }

    @FXML
    private void phenopacketButtonAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Phenopacket File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Phenopacket JSON File", "*.json"));
        File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
        if (file != null) {
            phenopacketLabel.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void vcfButtonAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Background VCF File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Background VCF File", "*.vcf"));
        File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
        if (file != null) {
            vcfLabel.setText(file.getAbsolutePath());
        }
    }

    private void updateLimits(Slider slider, double min, double max) {
        slider.setMin(min);
        slider.setMax(max);
        double range = max - min;
        double incValue = range/4;
        slider.setMajorTickUnit(incValue);
    }

//    private void adjustLimits(Slider slider, double value) {
//        // TODO(mabeckwith) - Please use the function if we really want to change the slider values.
//        //  My gut feeling is that being able to set value to >10 should not be super important.
//        double curMin = slider.getMin();
//        double curMax = slider.getMax();
//        if (value > curMax) {
//            double newMax = value * 2.0;
//            while (newMax < value) {
//                newMax *= 2.0;
//            }
//            updateLimits(slider, curMin, newMax);
//            slider.setValue(value);
//        } else if (value <= curMax / 4.0) {
//            double newMax = curMax / 4.0;
//            updateLimits(slider, curMin, newMax);
//            slider.setValue(value);
//        }
//    }

    @FXML
    private void liricalButtonAction(ActionEvent event) throws Exception {
        Map<TermId, Double> diseaseIdToPretestProba = PretestProbability.of(mondoTreeView, optionalServices.mondoOmimResources(), optionalServices.getLirical().phenotypeService().diseases().diseaseIds(), DEFAULT_SLIDER_VALUE);
        Path phenopacketFile = Path.of(phenopacketLabel.getText());
        String vcfFile = vcfLabel.getText();
        if (!Files.isRegularFile(phenopacketFile)) {
            PopUps.showInfoMessage("Unable to run analysis: no phenopacket present.", "ERROR");
            LOGGER.info("Unable to run analysis: no phenopacket present.");
        }

        Lirical lirical = optionalServices.getLirical();

        AnalysisData analysisData = null; // TODO - implement
        AnalysisOptions analysisOptions = null; // TODO - implement
        AnalysisResultsMetadata metadata = null; // TODO - implement

        LiricalAnalysisRunner runner = lirical.analysisRunner();

        AnalysisResults results = runner.run(analysisData, analysisOptions);

        AnalysisResultsWriter writer = lirical.analysisResultsWriterFactory()
                .getWriter(analysisData, results, metadata);

        OutputOptions outputOptions = createOutputOptions();
        writer.process(outputOptions);
        Path outFile = outputOptions.outputDirectory().resolve(outputOptions.prefix() + ".html");

        if (Files.isRegularFile(outFile)) {
            try {
                URL document = new URL(outFile.toAbsolutePath().toString());
                urlBrowser.showDocument(document);
            } catch (MalformedURLException e) {
                LOGGER.error("Unable to create URL from {}: ", outFile.toAbsolutePath());
            }
        }

        event.consume();
    }

    protected OutputOptions createOutputOptions() throws LiricalParseException {
        double lrThresholdValue = Double.parseDouble(lrThresholdTextField.getText());
        int minDiagnosisValue = Integer.parseInt(minDiagnosisSpinner.getValue().toString());
        String outputFormatsString = pgProperties.getProperty("output.formats");
        float pathogenicityThreshold = Float.parseFloat(pathogenicityTextField.getText());
        boolean displayAllVariants = variantsCheckbox.isSelected();
        if (lrThresholdValue < 0 || lrThresholdValue > 1) {
            PopUps.showInfoMessage("Error: LR Threshold must be between 0 and 1.", "ERROR");
            throw new LiricalParseException("LR Threshold not between 0 and 1.");
        }
        Path outdir = Path.of(pgProperties.getProperty("lirical.results.path"));
        String outfileText = outputFileTextField.getText();
        String outfilePrefix = outfileText == null ? "lirical_results" : outfileText;
        LrThreshold lrThreshold = LrThreshold.setToUserDefinedThreshold(lrThresholdValue);
        MinDiagnosisCount minDiagnosisCount = MinDiagnosisCount.setToUserDefinedMinCount(minDiagnosisValue);
        List<OutputFormat> outputFormats = parseOutputFormats(outputFormatsString);
        return new OutputOptions(lrThreshold,
                minDiagnosisCount,
                pathogenicityThreshold,
                displayAllVariants,
                outdir,
                outfilePrefix,
                outputFormats);
    }

    private List<OutputFormat> parseOutputFormats(String outputFormats) {
        return Arrays.stream(outputFormats.split(","))
                .map(String::trim)
                .map(toOutputFormat())
                .flatMap(Optional::stream)
                .toList();
    }

    private static Function<String, Optional<OutputFormat>> toOutputFormat() {
        return payload -> switch (payload.toUpperCase()) {
            case "HTML" -> Optional.of(OutputFormat.HTML);
            case "TSV" -> Optional.of(OutputFormat.TSV);
            default -> {
                LOGGER.warn("Unknown output format {}", payload);
                yield Optional.empty();
            }
        };
    }

}

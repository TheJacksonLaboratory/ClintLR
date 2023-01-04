package org.monarchinitiative.l2ci.gui.controller;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.monarchinitiative.l2ci.core.io.PretestProbaAdjustmentIO;
import org.monarchinitiative.l2ci.core.mondo.MondoStats;
import org.monarchinitiative.l2ci.gui.*;
import org.monarchinitiative.l2ci.gui.config.AppProperties;
import org.monarchinitiative.l2ci.gui.model.PretestProbability;
import org.monarchinitiative.l2ci.gui.resources.*;
import org.monarchinitiative.l2ci.gui.model.DiseaseWithMultiplier;
import org.monarchinitiative.l2ci.gui.tasks.LiricalRunTask;
import org.monarchinitiative.l2ci.gui.ui.mondotree.MondoTreeView;
import org.monarchinitiative.l2ci.gui.ui.summary.DiseaseSummaryView;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    // A pattern for matching either a float with a trailing `.` (e.g. `123.`)
    // or a float with optional whole number part followed by a dot and an arbitrary number of decimal places
    // (e.g. `.1`, `.123`, `1.234`).
    private static final Pattern NONNEGATIVE_FLOAT = Pattern.compile("(\\d+\\.?)|(\\d*\\.\\d+)");

    // Default multiplier value is 1. and it must match the multiplier in the `MainView.fxml`.
    private static final double DEFAULT_MULTIPLIER_VALUE = 1.;

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

    @FXML
    private AutoCompleteOntologyTextField autocompleteOmimTextField;

    @FXML
    private Button resetMultipliersButton;

    /**
     * A UI component for displaying details of the Term that is selected in the {@link #mondoTreeView}.
     */
    @FXML
    private DiseaseSummaryView diseaseSummaryView;

    @FXML
    private MondoTreeView mondoTreeView;

    // A flag for syncing pretest proba multiplier updates.
    private boolean updatingMultiplier = false;
    private final DoubleProperty multiplier = new SimpleDoubleProperty(DEFAULT_MULTIPLIER_VALUE);
    /**
     * Slider to adjust pretest probability multiplier before running LIRICAL
     */
    @FXML
    private Slider multiplierSlider;
    @FXML
    private TextField multiplierTextField;
    private TextFormatter<Double> multiplierFormatter;
    @FXML
    private Button liricalButton;
    @FXML
    private Label treeLabel;
    @FXML
    private TextField outputFileTextField;
    @FXML
    private TextField lrThresholdTextField;
    @FXML
    private Spinner<Integer> minDiagnosisSpinner;
    @FXML
    private TextField pathogenicityTextField;
    @FXML
    private CheckBox variantsCheckbox;

    @FXML
    private Label phenopacketLabel;
    private final ObjectProperty<Path> phenopacketPath = new SimpleObjectProperty<>();
    @FXML
    private Label vcfLabel;
    private final ObjectProperty<Path> vcfPath = new SimpleObjectProperty<>();


    private enum MessageType {
        INFO, WARNING, ERROR
    }

    public MainController(OptionalResources optionalResources,
                          OptionalServices optionalServices,
                          AppProperties appProperties,
                          ExecutorService executorService,
                          UrlBrowser urlBrowser,
                          Path dataDirectory) {
        this.optionalResources = optionalResources;
        this.optionalServices = optionalServices;
        this.appProperties = appProperties;
        this.executor = executorService;
        this.urlBrowser = urlBrowser;
        this.dataDirectory = dataDirectory;
    }

    @FXML
    private void initialize() {
        minDiagnosisSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 10));
        pathogenicityTextField.setText(String.valueOf(optionalResources.liricalResources().getPathogenicityThreshold()));
        variantsCheckbox.setSelected(false);

        showMondoStats.disableProperty().bind(optionalServices.mondoProperty().isNull());
        copyrightLabel.setText("L4CI, v. " + appProperties.version() + ", © Monarch Initiative 2022");

        // ---------- Autocompletion fields ----------
        // Mondo disease name autocomplete
        autocompleteTextField.ontologyProperty().bind(optionalServices.mondoProperty());
        autocompleteOmimTextField.omim2MondoProperty().bind(optionalServices.mondoOmimResources().omimToMondoProperty());

        // ------------- Slider UI fields ------------
        multiplierFormatter = preparePretestProbabilityFormatter(multiplierSlider.getMin(), multiplierSlider.getMax(), DEFAULT_MULTIPLIER_VALUE);
        multiplierTextField.setTextFormatter(multiplierFormatter);

        InvalidationListener keepMultiplierValuesInSync = updateMultiplierValuesInTheUi();
        multiplierFormatter.valueProperty().addListener(keepMultiplierValuesInSync);
        multiplierSlider.valueProperty().addListener(keepMultiplierValuesInSync);
        multiplier.addListener(keepMultiplierValuesInSync);

        // -------- Inputs - Phenopacket & VCF --------
        phenopacketLabel.textProperty().bind(showAbsolutePathIfPresent(phenopacketPath));
        vcfLabel.textProperty().bind(showAbsolutePathIfPresent(vcfPath));

        // Show path to Mondo file
        treeLabel.textProperty().bind(showAbsolutePathIfPresent(optionalResources.ontologyResources().mondoPathProperty()));

        // Set up the Mondo tree
        mondoTreeView.disableProperty().bind(optionalServices.mondoProperty().isNull());
        mondoTreeView.mondoProperty().bind(optionalServices.mondoProperty());
        mondoTreeView.nDescendentsProperty().bind(optionalServices.mondoOmimResources().mondoNDescendentsProperty());
        mondoTreeView.getSelectionModel().selectedItemProperty()
                .addListener((observable, previousMondoItem, newMondoItem) -> {
                    if (previousMondoItem != null)
                        // We must unbind the previous slider value on the tree item so that we do not update it.
                        previousMondoItem.getValue().multiplierProperty().unbind();

                    if (newMondoItem != null) {
                        // Next, we update the slider UI elements with the value of the new item and bind it,
                        // to track the user activity.
                        multiplier.setValue(newMondoItem.getValue().getMultiplier());
                        newMondoItem.getValue().multiplierProperty().bind(multiplier);
                    }
                });
        // Finally, we update the term description in the right panel
        mondoTreeView.getSelectionModel().selectedItemProperty()
                .addListener((observable, previousMondoItem, newMondoItem) -> {
                    diseaseSummaryView.dataProperty().set(newMondoItem.getValue());
                });

        resetMultipliersButton.disableProperty().bind(mondoTreeView.multiplierValuesProperty().emptyProperty());

        // TODO - we need both LIRICAL and known disease IDs to run this. Add the corresponding binding.
        liricalButton.disableProperty().bind(
                optionalServices.liricalProperty().isNull()
                        .or(phenopacketPath.isNull())
                        .or(optionalResources.liricalResultsProperty().isNull()));

    }

    private static TextFormatter<Double> preparePretestProbabilityFormatter(double min, double max, double defaultValue) {
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

    private InvalidationListener updateMultiplierValuesInTheUi() {
        return obs -> {
            if (updatingMultiplier)
                return;
            try {
                // Note: this is not synchronized since JavaFX UI is updated exclusively on the UI thread.
                updatingMultiplier = true;
                if (obs.equals(multiplierTextField.getTextFormatter().valueProperty())) {
                    // The text field changed, we must update the slider
                    // We're practically sure the value is a valid double since we have a filter on the text formatter
                    multiplierSlider.setValue(multiplierFormatter.getValue());
                    multiplier.set(multiplierFormatter.getValue());
                } else if (obs.equals(multiplierSlider.valueProperty())) {
                    // The slider changed, we must update the text field.
                    multiplierFormatter.setValue(multiplierSlider.getValue());
                    multiplier.set(multiplierSlider.getValue());
                } else if (obs.equals(multiplier)) {
                    // The sliderValue changed, we must update both the text field and the slider
                    multiplierSlider.setValue(multiplier.getValue());
                    multiplierFormatter.setValue(multiplier.getValue());
                } else {
                    LOGGER.warn("Unknown observable changed: {}", obs);
                }
            } finally {
                updatingMultiplier = false;
            }
        };
    }

    private static StringBinding showAbsolutePathIfPresent(ObjectProperty<Path> pathProperty) {
        return Bindings.createStringBinding(
                () -> {
                    Path path = pathProperty.get();
                    return path == null ? "Unset" : path.toAbsolutePath().toString();
                },
                pathProperty);
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
            PretestProbaAdjustmentIO.write(mondoTreeView.multiplierValuesProperty(), file.toPath());
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
            Map<TermId, Double> adjustments = PretestProbaAdjustmentIO.read(file.toPath());
            mondoTreeView.multiplierValuesProperty().clear();
            mondoTreeView.multiplierValuesProperty().putAll(adjustments);
        } catch (IOException ex) {
            LOGGER.warn("Unable to load the pretest probability adjustments from {}", file.toPath().toAbsolutePath(), ex);
            PopUps.showException("Load Pretest Probability Adjustments", "Unable to load the data", ex);
        }
        e.consume();
    }

    @FXML
    private void showMapInterface(ActionEvent e) {
        ObservableList<DiseaseWithMultiplier> source = mondoTreeView.drainMultiplierValues()
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
            ResourcesController controller = new ResourcesController(optionalResources, appProperties, dataDirectory, executor);
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
        // TODO - have user choose a Mondo term from the list of Mondo terms in the Omim-to-Mondo map.
        Optional<List<TermId>> opt = autocompleteOmimTextField.getSelectedIds();
        if (opt.isPresent()) {
            TermId id = opt.get().get(0);
            goToTerm(id);
        } else {
            LOGGER.warn("Unable to get term id: ");
        }
        autocompleteOmimTextField.clear();
    }

    @FXML
    private void resetMultipliersAction(ActionEvent e) {
        LOGGER.debug("Resetting pretest probability multiplier values");
        mondoTreeView.clearMultipliers();
        e.consume();
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
            phenopacketPath.set(file.toPath());
        }
    }

    @FXML
    private void vcfButtonAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Background VCF File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Background VCF File", "*.vcf"));
        File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
        if (file != null) {
            vcfPath.set(file.toPath());
        }
    }

    private void updateLimits(Slider slider, double min, double max) {
        slider.setMin(min);
        slider.setMax(max);
        double range = max - min;
        double incValue = range/4;
        slider.setMajorTickUnit(incValue);
    }

    @FXML
    private void liricalButtonAction(ActionEvent event) throws Exception {
        LOGGER.debug("Running LIRICAL");
        Lirical lirical = optionalServices.getLirical();
        AnalysisOptions analysisOptions = prepareAnalysisOptions();

        LiricalRunTask liricalTask = new LiricalRunTask(lirical,
                optionalResources.liricalResources(),
                phenopacketPath.get(),
                vcfPath.get(), // nullable
                analysisOptions,
                createOutputOptions());

        liricalTask.setOnSucceeded(e -> {
            Path report = liricalTask.getValue();
            if (Files.isRegularFile(report)) {
                URI uri = report.toUri();
                try {
                    URL document = uri.toURL();
                    urlBrowser.showDocument(document);
                } catch (MalformedURLException ex) {
                    LOGGER.error("Unable to create URL from {}: ", uri, ex);
                }
            }
        });
        liricalTask.setOnFailed(e -> PopUps.showException(
                "Run LIRICAL",
                e.getSource().getException().getMessage(),
                e.getSource().getException())
            );
        executor.submit(liricalTask);
        event.consume();
    }

    private AnalysisOptions prepareAnalysisOptions() {
        Map<TermId, Double> diseaseIdToPretestProba = PretestProbability.of(mondoTreeView.multiplierValuesProperty(), optionalServices.mondoOmimResources(), optionalServices.getLirical().phenotypeService().diseases().diseaseIds(), DEFAULT_MULTIPLIER_VALUE);
        PretestDiseaseProbability pretestProba = PretestDiseaseProbability.of(diseaseIdToPretestProba);
        LiricalResources liricalResources = optionalResources.liricalResources();
        return AnalysisOptions.builder()
                .genomeBuild(liricalResources.getGenomeBuild())
                .transcriptDatabase(liricalResources.getTranscriptDatabase())
                .setDiseaseDatabases(List.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER))
                .variantDeleteriousnessThreshold(liricalResources.getPathogenicityThreshold())
                .defaultVariantBackgroundFrequency(liricalResources.getDefaultVariantBackgroundFrequency())
                .useStrictPenalties(liricalResources.isStrict())
//                .useGlobal(true) // TODO - evaluate
                .pretestProbability(pretestProba)
                .disregardDiseaseWithNoDeleteriousVariants(false) // TODO - evaluate
                .build();
    }

    private OutputOptions createOutputOptions() throws LiricalParseException {
        double lrThresholdValue = Double.parseDouble(lrThresholdTextField.getText());
        if (lrThresholdValue < 0 || lrThresholdValue > 1) {
            // TODO(mabeckwith) - can you use TextFormatter to make it impossible to submit out of range value and remove this check?
            PopUps.showInfoMessage("Error: LR Threshold must be between 0 and 1.", "ERROR");
            throw new LiricalParseException("LR Threshold not between 0 and 1.");
        }
        LrThreshold lrThreshold = LrThreshold.setToUserDefinedThreshold(lrThresholdValue);

        MinDiagnosisCount minDiagnosisCount = MinDiagnosisCount.setToUserDefinedMinCount(minDiagnosisSpinner.getValue());
        float pathogenicityThreshold = Float.parseFloat(pathogenicityTextField.getText());
        boolean displayAllVariants = variantsCheckbox.isSelected();

        Path resultsDir = optionalResources.getLiricalResults();
        String outfileText = outputFileTextField.getText();
        String outfilePrefix = outfileText == null ? "lirical_results" : outfileText;

        return new OutputOptions(lrThreshold,
                minDiagnosisCount,
                pathogenicityThreshold,
                displayAllVariants,
                resultsDir,
                outfilePrefix);
    }

}

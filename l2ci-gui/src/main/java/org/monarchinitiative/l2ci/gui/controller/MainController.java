package org.monarchinitiative.l2ci.gui.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.core.io.MapFileWriter;
import org.monarchinitiative.l2ci.core.mondo.MondoStats;
import org.monarchinitiative.l2ci.core.pretestprob.PretestProbability;
import org.monarchinitiative.l2ci.gui.MainApp;
import org.monarchinitiative.l2ci.gui.PopUps;
import org.monarchinitiative.l2ci.gui.StartupTask;
import org.monarchinitiative.l2ci.gui.WidthAwareTextFields;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoResource;
import org.monarchinitiative.l2ci.gui.resources.OptionalHpoaResource;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

@Component
public class MainController {

    private static MainController mainController;
    private Ontology ont;

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String EVENT_TYPE_CLICK = "click";

    /**
     * Application-specific properties (not the System properties!) defined in the 'application.properties' file that
     * resides in the classpath.
     */
    private final Properties pgProperties;

    private final ExecutorService executor;

    private final OptionalHpoResource optionalHpoResource;

    private final OptionalHpoaResource optionalHpoaResource;

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

    private double preTestProb;

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

    @Autowired
    public MainController(OptionalHpoResource optionalHpoResource,
                          OptionalHpoaResource optionalHpoaResource,
                          @Qualifier("configProperties") Properties properties,
                          @Qualifier("appHomeDir") File l4ciDir,
                          ExecutorService executorService) {
        this.optionalHpoResource = optionalHpoResource;
        this.optionalHpoaResource = optionalHpoaResource;
        this.pgProperties = properties;
        this.l4ciDir = l4ciDir;
        this.executor = executorService;
    }

    @FXML
    private void initialize() {
        mainController = this;
        logger.info("Initializing main controller");
        StartupTask task = new StartupTask(optionalHpoResource, optionalHpoaResource, pgProperties);
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
        probSlider.setMin(0);
        probSlider.setMax(1);
        probSlider.setValue(0.5);
        probSlider.setMajorTickUnit(0.25);
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

        ChangeListener<? super Object> listener = (obs, oldval, newval) -> activateIfResourcesAvailable();
        optionalHpoResource.ontologyProperty().addListener(listener);
        optionalHpoaResource.directAnnotMapProperty().addListener(listener);
        optionalHpoaResource.indirectAnnotMapProperty().addListener(listener);
        logger.info("Done initialization");
        checkAll();
        logger.info("done activate");
    }

    @FXML
    public void loadFile(Event e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Local Mondo JSON File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Mondo JSON File", "*.json"));
        Stage stage = MainApp.mainStage;
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            String mondoJsonPath = file.getAbsolutePath();
            HPOParser parser = new HPOParser(mondoJsonPath);
            ont = parser.getHPO();
            if (ont != null) {
                optionalHpoResource.setOntology(ont);
                pgProperties.setProperty("hpo.json.path", mondoJsonPath);
                logger.info("Loaded Ontology {} from file {}", ont.toString(), file.getAbsolutePath());
                activateOntologyTree();
            }
        }
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
        Map<TermId, Double> pretestMap = PretestProbability.getAdjustedDiseaseToPretestMap();
        if (file != null) {
            new MapFileWriter(pretestMap, file.getAbsolutePath());
        }
    }

    @FXML
    private void close(ActionEvent e) {
        logger.trace("Closing down");
        Platform.exit();
    }

    private void activateIfResourcesAvailable() {
        if (optionalHpoResource.getOntology() != null) { // hpo obo file is missing
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
        } else {
            logger.info("All three resources loaded");
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
        if (optionalHpoResource.getOntology() == null) {
            logger.error("activateOntologyTree: HPO null");
        } else {
            final Ontology hpo = optionalHpoResource.getOntology();
            Platform.runLater(()->{
                initTree(hpo, k -> System.out.println("Consumed " + k));
                WidthAwareTextFields.bindWidthAwareAutoCompletion(autocompleteTextfield, ontologyLabelsAndTermIdMap.keySet());
            });
        }
    }

    private void makeSelectedDiseaseMap(double adjProb) {
        Map<TermId, Double> selectedDiseaseMap = new HashMap<>();
        Term selectedTerm = ontologyTreeView.getSelectionModel().getSelectedItem().getValue().term;
        selectedDiseaseMap.put(selectedTerm.id(), 0.0);
        Set<Term> descendents = getTermRelations(selectedTerm, Relation.DESCENDENT);
        for (Term desc : descendents) {
            selectedDiseaseMap.put(desc.id(), 0.0);
        }
        PretestProbability pretestProbability = new PretestProbability(selectedDiseaseMap, selectedDiseaseMap.keySet(), adjProb);
        Map<TermId, Double> newMap = pretestProbability.getAdjustedDiseaseToPretestMap();
        System.out.println(newMap);
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
        Ontology hpo = optionalHpoResource.getOntology();
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
        sliderTextField.setText(String.valueOf(preTestProb));
    }

    @FXML
    public void liricalButtonAction(ActionEvent actionEvent) {
        System.out.println("Pretest Probability = " + preTestProb);
        makeSelectedDiseaseMap(preTestProb);
    }

    /**
     * Find the path from the root term to given {@link Term}, expand the tree and set the selection model of the
     * TreeView to the term position.
     *
     * @param term {@link Term} to be displayed
     */
    private void expandUntilTerm(Term term) {
        // logger.trace("expand until term " + term.toString());
        Ontology ontology = optionalHpoResource.getOntology();
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
     * Get the children of "term"
     *
     * @param term HPO Term of interest
     * @return children of term (not including term itself).
     */
    private Set<Term> getTermChildren(Term term) {
        Ontology  ontology = optionalHpoResource.getOntology();
        if (ontology == null) {
            logger.error("Ontology null");
            PopUps.showInfoMessage("Error: Could not initialize Ontology", "ERROR");
            return Set.of();
        }
        TermId parentTermId = term.id();
        Set<TermId> childrenIds = getChildTerms(ontology, parentTermId, false);
        Set<Term> kids = new HashSet<>();
        childrenIds.forEach(tid -> {
            Term ht = ontology.getTermMap().get(tid);
            kids.add(ht);
        });
        return kids;
    }

    /**
     * Get the relations of "term"
     *
     * @param term HPO Term of interest
     * @param relation Relation of interest (ancestor, descendent, child, parent)
     * @return relations of term (not including term itself).
     */
    private Set<Term> getTermRelations(Term term, Relation relation) {
        Ontology ontology = optionalHpoResource.getOntology();
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
        Ontology ontology = optionalHpoResource.getOntology();
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
            return getTermChildren(getValue().term).size() == 0;
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
                Set<Term> children = getTermChildren(getValue().term);
                children.stream()
                        .sorted(Comparator.comparing(Term::getName))
                        .map(term -> new OntologyTermTreeItem(new OntologyTermWrapper(term)))
                        .forEach(childrenList::add);
                super.getChildren().setAll(childrenList);
            }
            return super.getChildren();
        }
    }

}

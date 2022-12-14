package org.monarchinitiative.l2ci.gui.ui;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.monarchinitiative.l2ci.core.Relation;
import org.monarchinitiative.l2ci.gui.model.DiseaseWithSliderValue;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

public class MondoTreeView extends TreeView<OntologyTermWrapper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MondoTreeView.class);
    private final MapProperty<TermId, Double> sliderValues = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private final ObjectProperty<Ontology> mondo = new SimpleObjectProperty<>();
    private final MapProperty<TermId, Integer> nChildren = new SimpleMapProperty<>(FXCollections.observableHashMap());

    public MondoTreeView() {
        super();
        setShowRoot(false);
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        setCellFactory(tw -> new MondoTreeCell(nChildren));
        mondo.addListener(handleOntologyUpdate());
    }

    public ObjectProperty<Ontology> mondoProperty() {
        return mondo;
    }

    public MapProperty<TermId, Double> sliderValuesProperty() {
        return sliderValues;
    }

    public MapProperty<TermId, Integer> nChildrenProperty() {
        return nChildren;
    }

    private ChangeListener<Ontology> handleOntologyUpdate() {
        return (obs, old, mondo) -> {
            if (mondo == null) {
                setRoot(null);
            } else {
                TermId rootId = mondo.getRootTermId();
                Set<Term> children = Relation.getTermRelations(mondo, rootId, Relation.CHILD);
                List<Term> containsDisease = children.stream().filter(r -> r.getName().contains("disease or disorder")).toList();
                if (!containsDisease.isEmpty()) {
                    rootId = containsDisease.get(0).id();
                }
                Term rootTerm = mondo.getTermMap().get(rootId);
                // TODO(ielis) - replace 1.0 with DEFAULT_SLIDER_VALUE variable
                TreeItem<OntologyTermWrapper> root = new MondoTreeItem(OntologyTermWrapper.createOmimXref(rootTerm, 1), mondo, nChildren, sliderValues);
                root.setExpanded(true);
                setRoot(root);
            }
        };
    }


    /**
     * Find the path from the root term to given {@link Term}, expand the tree and set the selection model of the
     * TreeView to the term position.
     *
     * @param term {@link Term} to be displayed
     */
    public void expandUntilTerm(Identified term) {
        Ontology mondo = this.mondo.get();
        if (existsPathFromRoot(term)) {
            // find root -> term path through the tree
            Stack<Identified> termStack = new Stack<>();
            termStack.add(term);
            Optional<? extends Identified> parents = Relation.getTermRelationsStream(mondo, term.id(), Relation.PARENT).findFirst();
            while (parents.isPresent()) {
                Identified parent = parents.get();
                termStack.add(parent);
                parents = Relation.getTermRelationsStream(mondo, parent.id(), Relation.PARENT).findFirst();
            }

            // expand tree nodes in top -> down direction
            List<TreeItem<OntologyTermWrapper>> children = getRoot().getChildren();
            termStack.pop(); // get rid of 'All' node which is hidden
            TreeItem<OntologyTermWrapper> target = getRoot();
            while (!termStack.empty()) {
                Identified current = termStack.pop();
                for (TreeItem<OntologyTermWrapper> child : children) {
                    if (child.getValue().term().equals(current)) {
                        child.setExpanded(true);
                        target = child;
                        children = child.getChildren();
                        break;
                    }
                }
            }
            getSelectionModel().select(target);
            scrollTo(getSelectionModel().getSelectedIndex());
        } else {
            TermId rootId = mondo.getRootTermId();
            Term rootTerm = mondo.getTermMap().get(rootId);
            LOGGER.warn(String.format("Unable to find the path from %s to %s", rootTerm.id().getValue(), term.id().getValue()));
        }
    }

    private boolean existsPathFromRoot(Identified term) {
        Ontology mondo = this.mondo.get();
        return existsPath(mondo, term.id(), mondo.getRootTermId());
    }

    /**
     * Run a breadth-first traversal of the tree nodes to yield a stream of {@link DiseaseWithSliderValue} items.
     * <p>
     * Note, due to the fact that we use tree to display a graph, the {@code Stream} will contain non-unique elements
     * (the items with multiple parents).
     *
     * @deprecated Use {@link #drainSliderValues()} to get IDs and slider values for the values that have been changed.
     * @return a {@code Stream} with disease probabilities.
     */
    @Deprecated(forRemoval = true)
    public Stream<DiseaseWithSliderValue> drainDiseaseProbabilities() {
        // A tad of recursion never killed anybody..
        Stream.Builder<DiseaseWithSliderValue> builder = Stream.builder();
        getMapData(builder, getRoot());
        return builder.build();
    }

    public Stream<DiseaseWithSliderValue> drainSliderValues() {
        if (mondo.get() == null) {
            LOGGER.warn("Tried to get slider values with unset Mondo");
            return Stream.empty();
        }

        return sliderValues.entrySet().stream()
                .map(entry -> DiseaseWithSliderValue.of(
                        entry.getKey(),
                        mondo.get().getTermMap().get(entry.getKey()).getName(),
                        entry.getValue()));
    }

    private static void getMapData(Stream.Builder<DiseaseWithSliderValue> builder, TreeItem<OntologyTermWrapper> item) {
        if (item == null)
            return;

        builder.add(item.getValue());

        for (TreeItem<OntologyTermWrapper> child : item.getChildren()) {
            getMapData(builder, child);
        }
    }

}

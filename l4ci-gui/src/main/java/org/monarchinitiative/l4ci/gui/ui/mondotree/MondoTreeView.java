package org.monarchinitiative.l4ci.gui.ui.mondotree;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.monarchinitiative.l4ci.core.Relation;
import org.monarchinitiative.l4ci.gui.model.DiseaseWithMultiplier;
import org.monarchinitiative.l4ci.gui.ui.OntologyTermWrapper;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

public class MondoTreeView extends TreeView<OntologyTermWrapper> {

    // The default multiplier is 1.0, a no-op.
    static final double DEFAULT_MULTIPLIER_VALUE = 1.;
    private static final Logger LOGGER = LoggerFactory.getLogger(MondoTreeView.class);
    private final ObjectProperty<Ontology> mondo = new SimpleObjectProperty<>();
    private final MapProperty<TermId, Double> multiplierValues = new SimpleMapProperty<>(FXCollections.observableHashMap());
    private final MapProperty<TermId, Integer> nDescendents = new SimpleMapProperty<>(FXCollections.observableHashMap());

    public MondoTreeView() {
        super();
        setShowRoot(false);
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        setCellFactory(tw -> new MondoTreeCell(nDescendents));
        mondo.addListener(handleOntologyUpdate());
    }

    public ObjectProperty<Ontology> mondoProperty() {
        return mondo;
    }

    public MapProperty<TermId, Double> multiplierValuesProperty() {
        return multiplierValues;
    }

    public MapProperty<TermId, Integer> nDescendentsProperty() {
        return nDescendents;
    }

    /**
     * Reset all multiplier values by setting the multipliers of the expanded elements to {@code 1.0} and clearing the
     * {@link #multiplierValuesProperty()}.
     */
    public void clearMultipliers() {
        // Two steps. First, set multiplier of the expanded items to the default value.
        // This propagates to the children thanks to the listener in `MondoTreeItem`.
        // Next, clear the slider values.
        getRoot().getValue().multiplierProperty().setValue(DEFAULT_MULTIPLIER_VALUE);
        multiplierValues.clear();
    }

    private ChangeListener<Ontology> handleOntologyUpdate() {
        return (obs, old, mondo) -> {
            if (mondo == null) {
                setRoot(null);
            } else {
                TermId rootId = mondo.getRootTermId();
                Set<Term> children = Relation.getTermRelations(mondo, rootId, Relation.CHILD);
                List<Term> containsDisease = children.stream()
                        .filter(r -> r.getName().contains("disease or disorder"))
                        .toList();
                if (!containsDisease.isEmpty()) {
                    rootId = containsDisease.get(0).id();
                }
                Term rootTerm = mondo.getTermMap().get(rootId);
                TreeItem<OntologyTermWrapper> root = new MondoTreeItem(
                        OntologyTermWrapper.createOmimXref(rootTerm, DEFAULT_MULTIPLIER_VALUE),
                        mondo,
                        nDescendents,
                        multiplierValues);
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
     * Run a breadth-first traversal of the tree nodes to yield a stream of {@link DiseaseWithMultiplier} items.
     * <p>
     * Note, due to the fact that we use tree to display a graph, the {@code Stream} will contain non-unique elements
     * (the items with multiple parents).
     *
     * @return a {@code Stream} with disease probabilities.
     * @deprecated Use {@link #drainMultiplierValues()} to get IDs and slider values for the values that have been changed.
     */
    @Deprecated(forRemoval = true)
    public Stream<DiseaseWithMultiplier> drainDiseaseProbabilities() {
        // A tad of recursion never killed anybody..
        Stream.Builder<DiseaseWithMultiplier> builder = Stream.builder();
        getMapData(builder, getRoot());
        return builder.build();
    }

    /**
     * Get the stream of disease ids with the multiplier values that have been updated by the user.
     */
    public Stream<DiseaseWithMultiplier> drainMultiplierValues() {
        if (mondo.get() == null) {
            LOGGER.warn("Tried to get slider values with unset Mondo");
            return Stream.empty();
        }

        return multiplierValues.entrySet().stream()
                // The user can increase the probability multiplier
                .filter(e -> e.getValue() > DEFAULT_MULTIPLIER_VALUE)
                .map(entry -> DiseaseWithMultiplier.of(
                        entry.getKey(),
                        mondo.get().getTermMap().get(entry.getKey()).getName(),
                        entry.getValue()));
    }

    private static void getMapData(Stream.Builder<DiseaseWithMultiplier> builder, TreeItem<OntologyTermWrapper> item) {
        if (item == null)
            return;

        builder.add(item.getValue());

        for (TreeItem<OntologyTermWrapper> child : item.getChildren()) {
            getMapData(builder, child);
        }
    }

}

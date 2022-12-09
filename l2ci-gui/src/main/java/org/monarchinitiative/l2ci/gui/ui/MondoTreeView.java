package org.monarchinitiative.l2ci.gui.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.monarchinitiative.l2ci.core.Relation;
import org.monarchinitiative.l2ci.gui.model.DiseaseWithProbability;
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

    private final ObjectProperty<Ontology> mondo = new SimpleObjectProperty<>();
    private final ObjectProperty<Map<TermId, Integer>> mondoNDescendents = new SimpleObjectProperty<>(Map.of());

    public MondoTreeView() {
        super();
        setShowRoot(false);
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        setCellFactory(tw -> {
            MondoTreeCell cell = new MondoTreeCell();
            cell.mondoNDescendantsMapProperty().bind(mondoNDescendents);
            return cell;
        });
        mondo.addListener(handleOntologyUpdate());
    }

    public ObjectProperty<Ontology> mondoProperty() {
        return mondo;
    }

    public ObjectProperty<Map<TermId, Integer>> mondoNDescendentsProperty() {
        return mondoNDescendents;
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
                TreeItem<OntologyTermWrapper> root = new OntologyTermTreeItem(new OntologyTermWrapper(rootTerm), mondo);
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
            Set<? extends Identified> parents = Relation.getTermRelations(mondo, term.id(), Relation.PARENT);
            while (parents.size() != 0) {
                Identified parent = parents.iterator().next();
                termStack.add(parent);
                parents = Relation.getTermRelations(mondo, parent.id(), Relation.PARENT);
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

    public Stream<DiseaseWithProbability> drainDiseaseProbabilities() {
        // A tad of recursion never killed anybody..
        Stream.Builder<DiseaseWithProbability> builder = Stream.builder();
        getMapData(builder, getRoot());
        return builder.build();
    }

    private static void getMapData(Stream.Builder<DiseaseWithProbability> builder, TreeItem<OntologyTermWrapper> item) {
        if (item == null)
            return;

        builder.add(item.getValue());

        for (TreeItem<OntologyTermWrapper> child : item.getChildren()) {
            getMapData(builder, child);
        }
    }

}

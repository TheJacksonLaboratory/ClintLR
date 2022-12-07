package org.monarchinitiative.l2ci.gui.ui;

import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.monarchinitiative.l2ci.core.Relation;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/**
 * A class that defines a bridge between hierarchy of {@link Term}s and {@link TreeItem}s of the
 * {@link TreeView}.
 */
public class OntologyTermTreeItem extends TreeItem<OntologyTermWrapper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyTermTreeItem.class);

    private final Ontology mondo;

    /**
     * List used for caching of the children of this term
     */
    private ObservableList<TreeItem<OntologyTermWrapper>> childrenList;

    /**
     * Default & only constructor for the TreeItem.
     *
     * @param term  {@link Term} that is represented by this TreeItem
     * @param mondo Mondo {@link Ontology}
     */
    public OntologyTermTreeItem(OntologyTermWrapper term, Ontology mondo) {
        super(term);
        this.mondo = mondo;
        this.getValue().sliderValueProperty()
                .addListener((obs, oldProba, novelProba) -> {
                    LOGGER.trace("Setting pretest probability of {} to {}", getValue().term().id(), novelProba);
                    if (!isLeaf()) {
                        // Update the children anytime a value is set. Will end up setting the probability to all children recursively
                        for (TreeItem<OntologyTermWrapper> child : getChildren()) {
                            child.getValue().sliderValueProperty().set(novelProba.doubleValue());
                        }
                    }
                });
    }

    /**
     * Check that the {@link Term} that is represented by this TreeItem is a leaf term as described below.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean isLeaf() {
        return Relation.getTermRelations(mondo, getValue().term().id(), Relation.CHILD).isEmpty();
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
            Set<Term> children = Relation.getTermRelations(mondo, getValue().term().id(), Relation.CHILD);
            Comparator<Term> compByChildrenSize = (term1, term2) ->
                    Relation.getTermRelations(mondo, term2.id(), Relation.CHILD).size()
                            - Relation.getTermRelations(mondo, term1.id(), Relation.CHILD).size();
            children.stream()
                    .filter(Objects::nonNull) // TODO - why do we have to do this?
                    .sorted(compByChildrenSize.thenComparing(Term::getName))
                    .map(term -> new OntologyTermTreeItem(new OntologyTermWrapper(term), mondo))
                    .forEach(childrenList::add);
            super.getChildren().setAll(childrenList);
        }
        return super.getChildren();
    }

    /**
     * @return pretest probability property in the underlying {@link OntologyTermWrapper}.
     */
    public DoubleProperty pretestProbabilityProperty() {
        return getValue().sliderValueProperty();
    }

}

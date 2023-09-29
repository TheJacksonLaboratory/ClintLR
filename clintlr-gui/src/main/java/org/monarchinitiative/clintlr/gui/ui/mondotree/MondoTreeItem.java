package org.monarchinitiative.clintlr.gui.ui.mondotree;

import javafx.beans.property.MapProperty;
import javafx.scene.control.TreeItem;
import org.monarchinitiative.clintlr.core.Relation;
import org.monarchinitiative.clintlr.gui.ui.OntologyTermWrapper;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A class that defines a bridge between hierarchy of {@link Term}s and {@link TreeItem}s of the
 * {@link MondoTreeView}.
 * <p>
 * {@link MondoTreeItem} knows about Mondo {@link Ontology} to be able to prepare child tree items. It
 */
class MondoTreeItem extends TreeItem<OntologyTermWrapper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MondoTreeItem.class);

    private final Ontology mondo;
    private Boolean isLeaf = null;

    /**
     * Default & only constructor for the TreeItem.
     *
     * @param term             {@link Term} that is represented by this {@linkplain MondoTreeItem}.
     * @param mondo            Mondo {@link Ontology}
     * @param nChildren        A map for caching number of children of a Mondo term.
     * @param multipliers      A mapping from Mondo ID to multiplier value
     */
    MondoTreeItem(OntologyTermWrapper term,
                  Ontology mondo,
                  MapProperty<TermId, Integer> nChildren,
                  Map<TermId, Double> multipliers) {
        super(term);
        this.mondo = mondo;
        this.getValue().multiplierProperty()
                .addListener((obs, oldProba, novelProba) -> {
                    LOGGER.trace("Setting pretest probability adjustment of {} to {}", getValue().term().id(), novelProba);
                    if (!isLeaf()) {
                        // Update the children anytime a value is set. Will end up setting the adjustment to all children recursively
                        Relation.getTermRelationsStream(mondo, getValue().id(), Relation.DESCENDENT)
                                .forEach(descendent -> multipliers.put(descendent.id(), novelProba.doubleValue()));
                    }
                });

        Comparator<MondoTreeItem> comparator = Comparator.<MondoTreeItem>comparingInt(t -> nChildren.getOrDefault(t.getValue().id(), 0))
                .reversed()
                .thenComparing(treeItem -> treeItem.getValue().term().getName());
//        Comparator<MondoTreeItem> comparator = Comparator.comparing(treeItem -> treeItem.getValue().term().getName());

        expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            /*
              The user or code is expanding or collapsing the tree item.
              We need to get the children and set with the slider values.
              We do not cache the children, we cache just the slider values in the `multipliers` map.
             */
            if (isExpanded) {
                /*
                 We create a child tree item on the fly. We set the child adjustment values to the previous value or
                 null if no previous value was set. Then, we set up a listener to keep the map updated on changes.
                 As a side product, we also cache the number of children.
                */
                List<MondoTreeItem> children = Relation.getTermRelationsStream(mondo, getValue().term().id(), Relation.CHILD)
                        .map(t -> {
                            OntologyTermWrapper wrapper = OntologyTermWrapper.createOmimXref(t, MondoTreeView.DEFAULT_MULTIPLIER_VALUE);
                            Double previousMultiplier = multipliers.get(t.id());
                            if (previousMultiplier != null)
                                wrapper.setMultiplier(previousMultiplier);
                            wrapper.multiplierProperty().addListener((o, old, novel) -> multipliers.put(wrapper.id(), novel.doubleValue()));
                            return new MondoTreeItem(wrapper, mondo, nChildren, multipliers);
                        })
                        .sorted(comparator)
                        .toList();
                nChildren.put(getValue().term().id(), children.size());
                getChildren().setAll(children);
            } else
                // The item is being collapsed. We clear the children to free up some memory. Note, we keep the adjustment values in the `multipliers` map!
                getChildren().clear();
        });
    }

    /**
     * Check that the {@link Term} that is represented by this TreeItem is a leaf term as described below.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean isLeaf() {
        // We cache the results to improve UI performance.
        // The code is not prone to race conditions because the method is run solely by JavaFX thread.
        if (isLeaf == null)
            isLeaf = Relation.getTermRelationsStream(mondo, getValue().term().id(), Relation.CHILD).findAny().isEmpty();
        return isLeaf;
    }

}

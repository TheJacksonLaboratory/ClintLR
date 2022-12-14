package org.monarchinitiative.l2ci.gui.ui;

import javafx.beans.property.MapProperty;
import javafx.scene.control.TreeItem;
import org.monarchinitiative.l2ci.core.Relation;
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

    /**
     * Default & only constructor for the TreeItem.
     *
     * @param term         {@link Term} that is represented by this {@linkplain MondoTreeItem}.
     * @param mondo        Mondo {@link Ontology}
     * @param nChildren    A map for caching number of children of a Mondo term.
     * @param sliderValues A mapping from Mondo ID to slider
     */
    MondoTreeItem(OntologyTermWrapper term,
                  Ontology mondo,
                  MapProperty<TermId, Integer> nChildren,
                  MapProperty<TermId, Double> sliderValues) {
        super(term);
        this.mondo = mondo;
        this.getValue().sliderValueProperty()
                .addListener((obs, oldProba, novelProba) -> {
                    LOGGER.trace("Setting pretest probability of {} to {}", getValue().term().id(), novelProba);
                    if (!isLeaf()) {
                        // Update the children anytime a value is set. Will end up setting the probability to all children recursively
                        Relation.getTermRelationsStream(mondo, getValue().id(), Relation.DESCENDENT)
                                .forEach(descendent -> sliderValues.put(descendent.id(), novelProba.doubleValue()));
                    }
                });
        // TODO - sort tree item children number of descendants.
//        Comparator<MondoTreeItem> comparator = Comparator.<MondoTreeItem>comparingInt(t -> nChildren.getOrDefault(t.getValue().id(), 0))
        Comparator<MondoTreeItem> comparator = Comparator.comparing(treeItem -> treeItem.getValue().term().getName());

        expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            /*
              The user or code is expanding or collapsing the tree item.
              We need to get the children and set with the slider values.
              We do not cache the children, we cache just the slider values in the `sliderValues` map.
             */
            if (isExpanded) {
                /*
                 We create a child tree item on the fly. We set the child slider values to the previous value or
                 null if no previous value was set. Then, we set up a listener to keep the map updated on changes.
                 As a side product, we also cache the number of children.
                */
                List<MondoTreeItem> children = Relation.getTermRelationsStream(mondo, getValue().term().id(), Relation.CHILD)
                        .map(t -> {
                            // TODO(ielis) - replace 1.0 with DEFAULT_SLIDER_VALUE variable
                            OntologyTermWrapper wrapper = OntologyTermWrapper.createOmimXref(t, 1.0);
                            Double previousSliderValue = sliderValues.get(wrapper.id());
                            wrapper.sliderValueProperty().setValue(previousSliderValue);
                            wrapper.sliderValueProperty().addListener((o, old, novel) -> sliderValues.put(wrapper.id(), novel.doubleValue()));
                            return new MondoTreeItem(wrapper, mondo, nChildren, sliderValues);
                        })
                        .sorted(comparator)
                        .toList();
                nChildren.put(getValue().term().id(), children.size());
                getChildren().setAll(children);
            } else
                // The item is being collapsed. We clear the children to free up some memory. Note, we keep the slider values in the `sliderValues` map!
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
        // TODO - Getting children is relatively expensive operation. Perhaps we can cache this?
        return Relation.getTermRelationsStream(mondo, getValue().term().id(), Relation.CHILD).findAny().isEmpty();
    }

}

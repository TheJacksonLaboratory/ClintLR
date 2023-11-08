package org.monarchinitiative.clintlr.gui.ui.mondotree;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.MapProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.monarchinitiative.clintlr.gui.ui.OntologyTermWrapper;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

class MondoTreeCell extends TreeCell<OntologyTermWrapper> {

    // TODO(mabeckwith) - a better image for no OMIM disease or PS, or no image at all?
    private static final Image EMPTY_SQUARE = loadBundledImage("/icons/empty16x16.png");
    private static final Image RED_CIRCLE = loadBundledImage("/icons/red_circle.png");
    private static final Image RED_CIRCLE_UP_ARROW = loadBundledImage("/icons/red_circle_up_arrow.png");
    private static final Image BLACK_CIRCLE = loadBundledImage("/icons/black_circle.png");
    private static final Image BLACK_CIRCLE_UP_ARROW = loadBundledImage("/icons/black_circle_up_arrow.png");

    private final MapProperty<TermId, Integer> nDescendents;

    MondoTreeCell(MapProperty<TermId, Integer> nDescendents) {
        this.nDescendents = nDescendents;
    }

    @Override
    protected void updateItem(OntologyTermWrapper item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty && item != null) {
            // Text
            StringBinding text = Bindings.createStringBinding(
                    () -> "(%d) %s".formatted(nDescendents.getOrDefault(item.id(), 0), item.term().getName()),
                    Bindings.valueAt(nDescendents, item.id()));
            textProperty().bind(text);
            // Graphics
            setGraphic(chooseAnImage(item, item.getMultiplier()));
            item.multiplierProperty()
                    .addListener((obs, old, multiplierValue) -> chooseAnImage(item, multiplierValue));
        } else {
            // Text
            textProperty().unbind();
            setText(null);
            // Graphics
            setGraphic(null);
        }
    }

    private static Node chooseAnImage(OntologyTermWrapper item, Number multiplierValue) {
        if (item != null) {
            if (multiplierValue.doubleValue() > MondoTreeView.DEFAULT_MULTIPLIER_VALUE) {
                if (item.hasOmimXref()) {
                    return new ImageView(BLACK_CIRCLE_UP_ARROW);
                } else if (item.hasOmimPSXref()) {
                    return new ImageView(RED_CIRCLE_UP_ARROW);
                }
            } else {
                if (item.hasOmimXref()) {
                    return new ImageView(BLACK_CIRCLE);
                } else if (item.hasOmimPSXref()) {
                    return new ImageView(RED_CIRCLE);
                }
            }
        }
        return new ImageView(EMPTY_SQUARE);
    }

    private static Image loadBundledImage(String location) {
        try (InputStream is = MondoTreeCell.class.getResourceAsStream(location)) {
            return new Image(Objects.requireNonNull(is));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the image from %s. Please contact the developers".formatted(location), e);
        }
    }

}

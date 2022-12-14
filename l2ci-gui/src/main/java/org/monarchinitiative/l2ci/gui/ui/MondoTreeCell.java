package org.monarchinitiative.l2ci.gui.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.MapProperty;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.monarchinitiative.l2ci.gui.controller.MainController;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

class MondoTreeCell extends TreeCell<OntologyTermWrapper> {

    private static final Image RED_CIRCLE = loadBundledImage("/icons/red_circle.png");
    private static final Image RED_CIRCLE_UP_ARROW = loadBundledImage("/icons/red_circle_up_arrow.png");
    private static final Image BLACK_CIRCLE = loadBundledImage("/icons/black_circle.png");
    private static final Image BLACK_CIRCLE_UP_ARROW = loadBundledImage("/icons/black_circle_up_arrow.png");

    private final MapProperty<TermId, Integer> nDescendents;

    MondoTreeCell(MapProperty<TermId, Integer> nDescendents) {
        this.nDescendents = nDescendents;
    }

    private static Image loadBundledImage(String location) {
        try (InputStream is = MainController.class.getResourceAsStream(location)) {
            return new Image(Objects.requireNonNull(is));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the image from %s. Please contact the developers".formatted(location), e);
        }
    }

    @Override
    protected void updateItem(OntologyTermWrapper item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty || item != null) {
            // Text
            StringBinding text = Bindings.createStringBinding(
                    () -> "(%d) %s".formatted(nDescendents.getOrDefault(item.id(), 0), item.term().getName()),
                    Bindings.valueAt(nDescendents, item.id()));
            textProperty().bind(text);

            // Graphics
            // TODO - update icons of descendants as well when change the slider
            item.sliderValueProperty().greaterThan(MondoTreeView.DEFAULT_PROBABILITY_ADJUSTMENT).addListener((obs, old, sliderAboveDefault) -> {
                if (sliderAboveDefault) {
                    if (item.hasOmimXref()) {
                        ImageView omimChanged = new ImageView(BLACK_CIRCLE_UP_ARROW);
                        setGraphic(omimChanged);
                    } else if (item.hasOmimPSXref()) {
                        ImageView omimPSChanged = new ImageView(RED_CIRCLE_UP_ARROW);
                        setGraphic(omimPSChanged);
                    } else {
                        setGraphic(null);
                    }
                } else {
                    if (item.hasOmimXref()) {
                        ImageView omimDefault = new ImageView(BLACK_CIRCLE);
                        setGraphic(omimDefault);
                    } else if (item.hasOmimPSXref()) {
                        ImageView omimPSDefault = new ImageView(RED_CIRCLE);
                        setGraphic(omimPSDefault);
                    } else {
                        setGraphic(null);
                    }
                }
            });
        } else {
            textProperty().unbind();
            setText(null);
            setGraphic(null);
        }
    }

}

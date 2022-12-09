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

    private void updateTreeIcons(OntologyTermWrapper item, ImageView icon1, ImageView icon2) {
        setGraphic(icon1);
        // TODO(mabeckwith) - please select the appropriate icon
//        if (mapDataList != null) {
//            for (MapData mapData : mapDataList) {
//                TermId mapMondoId = mapData.getMondoId();
//                TermId treeMondoId = item.term().id();
//                if (mapMondoId != null && mapMondoId.equals(treeMondoId) && mapData.getSliderValue() > 1.0) {
//                    setGraphic(icon2);
//                }
//            }
//        }
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
            // TODO - perhaps we can optimize the conditions to prevent two streams?
            if (item.term().getXrefs().stream().anyMatch(r -> r.getName().contains("OMIM:"))) {
                ImageView omimIcon = new ImageView(BLACK_CIRCLE);
                ImageView selectedIcon = new ImageView(BLACK_CIRCLE_UP_ARROW);
                updateTreeIcons(item, omimIcon, selectedIcon);
            } else if (item.term().getXrefs().stream().noneMatch(r -> r.getName().contains("OMIM:"))) {
                if (item.term().getXrefs().stream().anyMatch(r -> r.getName().contains("OMIMPS:"))) {
                    ImageView omimPSIcon = new ImageView(RED_CIRCLE);
                    ImageView omimPSSelectedIcon = new ImageView(RED_CIRCLE_UP_ARROW);
                    updateTreeIcons(item, omimPSIcon, omimPSSelectedIcon);
                } else {
                    updateTreeIcons(item, null, null);
                }
            }
        } else {
            textProperty().unbind();
            setText(null);
            setGraphic(null);
        }
    }

}

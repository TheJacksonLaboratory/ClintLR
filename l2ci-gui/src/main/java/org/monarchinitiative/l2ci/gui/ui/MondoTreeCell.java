package org.monarchinitiative.l2ci.gui.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.monarchinitiative.l2ci.gui.controller.MainController;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class MondoTreeCell extends TreeCell<OntologyTermWrapper> {

    private static final Image RED_CIRCLE = loadBundledImage("/icons/red_circle.png");
    private static final Image RED_CIRCLE_UP_ARROW = loadBundledImage("/icons/red_circle_up_arrow.png");
    private static final Image BLACK_CIRCLE = loadBundledImage("/icons/black_circle.png");
    private static final Image BLACK_CIRCLE_UP_ARROW = loadBundledImage("/icons/black_circle_up_arrow.png");

    private final ObjectProperty<Map<TermId, Integer>> mondoNDescendantsMap = new SimpleObjectProperty<>(Map.of());

    private static Image loadBundledImage(String location) {
        try (InputStream is = MainController.class.getResourceAsStream(location)) {
            return new Image(Objects.requireNonNull(is));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the image from %s. Please contact the developers".formatted(location), e);
        }
    }

    public ObjectProperty<Map<TermId, Integer>> mondoNDescendantsMapProperty() {
        return mondoNDescendantsMap;
    }

    private void updateTreeIcons(OntologyTermWrapper item, ImageView icon1, ImageView icon2) {
        setGraphic(icon1);
        // TODO - implement
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
        ImageView omimPSIcon = new ImageView(RED_CIRCLE);
        ImageView omimPSSelectedIcon = new ImageView(RED_CIRCLE_UP_ARROW);
        ImageView omimIcon = new ImageView(BLACK_CIRCLE);
        ImageView selectedIcon = new ImageView(BLACK_CIRCLE_UP_ARROW);
        super.updateItem(item, empty);
        if (!empty || item != null) {

            setText(item.term().getName());
            if (mondoNDescendantsMap.get() != null) {
                Integer nDescendants = mondoNDescendantsMap.get().get(item.term().id());
                if (nDescendants != null) {
                    if (nDescendants > 0) {
                        setText("(" + nDescendants + ") " + item.term().getName());
                    }
                }

            }

            // TODO - perhaps we can optimize the conditions to prevent two streams?
            if (item.term().getXrefs().stream().anyMatch(r -> r.getName().contains("OMIMPS:"))) {
                updateTreeIcons(item, omimPSIcon, omimPSSelectedIcon);
            } else if (item.term().getXrefs().stream().noneMatch(r -> r.getName().contains("OMIMPS:"))) {
                if (item.term().getXrefs().stream().anyMatch(r -> r.getName().contains("OMIM:"))) {
                    updateTreeIcons(item, omimIcon, selectedIcon);
                } else {
                    updateTreeIcons(item, null, null);
                }
            }
        } else {
            setText(null);
            setGraphic(null);
        }
    }

}

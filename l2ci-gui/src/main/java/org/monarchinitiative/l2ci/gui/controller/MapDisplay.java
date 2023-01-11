package org.monarchinitiative.l2ci.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.monarchinitiative.l2ci.gui.model.DiseaseWithMultiplier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;

public class MapDisplay extends VBox {

    private final MapProperty<TermId, TermId> mondoToOmim = new SimpleMapProperty<>(FXCollections.observableHashMap());

    @FXML
    private TableView<DiseaseWithMultiplier> tableView;

    @FXML
    private TableColumn<DiseaseWithMultiplier, String> diseaseName;
    @FXML
    private TableColumn<DiseaseWithMultiplier, String> mondoId;
    @FXML
    private TableColumn<DiseaseWithMultiplier, TermId> omimId;
//    @FXML
//    private TableColumn<DiseaseWithProbability, Double> diseaseProb;
    @FXML
    private TableColumn<DiseaseWithMultiplier, Double> multiplierValue;
//    @FXML
//    private TableColumn<DiseaseWithProbability, Boolean> isFixed;

    public MapDisplay() {
        FXMLLoader loader = new FXMLLoader(MapDisplay.class.getResource("MapDisplay.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        diseaseName.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getName()));
        mondoId.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().id().getValue()));
        omimId.setCellValueFactory(cdf -> Bindings.valueAt(mondoToOmim, cdf.getValue().id()));
        omimId.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(TermId value, boolean empty) {
                super.updateItem(value, empty);
                if (value == null || empty) {
                    setText(null);
                } else {
                    setText(value.getValue());
                }
            }
        });

        multiplierValue.setCellValueFactory(cdf -> cdf.getValue().multiplierProperty().asObject());
        multiplierValue.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (value == null || empty) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", value));
                }
            }
        });

//        isFixed.setCellFactory(CheckBoxTableCell.forTableColumn(isFixed));
    }

    public MapProperty<TermId, TermId> mondoToOmimProperty() {
        return mondoToOmim;
    }

    public ObservableList<DiseaseWithMultiplier> getItems() {
        return tableView.getItems();
    }

}

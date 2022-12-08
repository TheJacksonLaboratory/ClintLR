package org.monarchinitiative.l2ci.gui.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.monarchinitiative.l2ci.gui.model.DiseaseWithSliderValue;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;

public class MapDisplay extends VBox {

    @FXML
    private TableView<DiseaseWithSliderValue> tableView;

    @FXML
    private TableColumn<DiseaseWithSliderValue, String> diseaseName;
    @FXML
    private TableColumn<DiseaseWithSliderValue, String> mondoId;
    @FXML
    private TableColumn<DiseaseWithSliderValue, String> omimId;
//    @FXML
//    private TableColumn<DiseaseWithProbability, Double> diseaseProb;
    @FXML
    private TableColumn<DiseaseWithSliderValue, Double> sliderValue;
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
        omimId.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(getOmimNames(cdf.getValue().id())));

        sliderValue.setCellValueFactory(cdf -> cdf.getValue().sliderValueProperty().asObject());
        sliderValue.setCellFactory(tc -> new TableCell<>() {
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

    private String getOmimNames(TermId mondoId) {
        return null;
    }

    public ObservableList<DiseaseWithSliderValue> getItems() {
        return tableView.getItems();
    }

}

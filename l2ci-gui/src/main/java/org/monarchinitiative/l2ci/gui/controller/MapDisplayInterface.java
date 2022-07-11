package org.monarchinitiative.l2ci.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.monarchinitiative.l2ci.gui.MapData;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class MapDisplayInterface {

    GridPane display = new GridPane();
    Stage mapStage = new Stage();

    TableView tableView = new TableView();

    public void initMapInterface() {
        display.getChildren().clear();
        display.add(tableView, 0, 1);

        tableView.prefHeightProperty().bind(display.heightProperty());
        tableView.prefWidthProperty().bind(display.widthProperty());

        mapStage.setScene(new Scene(display, 600, 300));
        mapStage.initOwner(MainController.getController().statusHBox.getScene().getWindow());
        mapStage.initModality(Modality.NONE);
        mapStage.setResizable(true);
        mapStage.setTitle("Probability Map");
    }

    public void show() {
        mapStage.show();
    }

    public void updateTable() {
        MainController.getController().makeSelectedDiseaseMap(MainController.preTestProb);
        List<MapData> mapDataList = MainController.mapDataList;
        ObservableList<MapData> data = FXCollections.observableArrayList();
        for (MapData mapData : mapDataList) {
            data.addAll(mapData);
        }
        tableView.itemsProperty().setValue(data);

        TableColumn<TermId, String> mondoIdColumn = new TableColumn("MondoId");
        TableColumn<TermId, String> omimIdColumn = new TableColumn("OmimId");
        TableColumn<String, String> nameColumn = new TableColumn("Name");
        TableColumn<Double, Double> probColumn = new TableColumn("Probability");
        TableColumn<Double, Double> sliderColumn = new TableColumn<>("SliderValue");

        mondoIdColumn.setCellValueFactory(new PropertyValueFactory<>("MondoId"));
        omimIdColumn.setCellValueFactory(new PropertyValueFactory<>("OmimId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        probColumn.setCellValueFactory(new PropertyValueFactory<>("Probability"));
        sliderColumn.setCellValueFactory(new PropertyValueFactory<>("SliderValue"));

        probColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (!empty) {
                    setText(String.format("%6.2e", value));
                } else {
                    setText(null);
                }
            }
        });

        sliderColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (!empty) {
                    setText(String.format("%.2f", value));
                } else {
                    setText(null);
                }
            }
        });

        tableView.getColumns().clear();
        tableView.getColumns().addAll(mondoIdColumn, omimIdColumn, nameColumn, sliderColumn, probColumn);
    }

}

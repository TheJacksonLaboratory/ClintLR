package org.monarchinitiative.l2ci.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.monarchinitiative.l2ci.gui.MapData;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;

public class MapDisplayInterface {

    GridPane display = new GridPane();
    Stage mapStage = new Stage();
    List<MapData> mapDataList = new ArrayList<>();

    TableView tableView = new TableView();

    public void launchMapInterface() {
        display.add(tableView, 0, 1);

        tableView.prefHeightProperty().bind(display.heightProperty());
        tableView.prefWidthProperty().bind(display.widthProperty());

        mapStage.setScene(new Scene(display, 600, 300));
        mapStage.setResizable(true);
        mapStage.setTitle("Probability Map");
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

        TableColumn<TermId, String> idColumn = new TableColumn("TermId");
        TableColumn<String, String> nameColumn = new TableColumn("Name");
        TableColumn<Double, Double> probColumn = new TableColumn("Probability");
        TableColumn<Double, Double> sliderColumn = new TableColumn<>("SliderValue");

        idColumn.setCellValueFactory(new PropertyValueFactory<>("TermId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        probColumn.setCellValueFactory(new PropertyValueFactory<>("Probability"));
        sliderColumn.setCellValueFactory(new PropertyValueFactory<>("SliderValue"));

        probColumn.setCellFactory(tc -> new TableCell<Double, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty) ;
                if (!empty) {
                    setText(String.format("%.4f", value));
                } else {
                    setText(null);
                }
            }
        });

        sliderColumn.setCellFactory(tc -> new TableCell<Double, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty) ;
                if (!empty) {
                    setText(String.format("%.2f", value));
                } else {
                    setText(null);
                }
            }
        });

        tableView.getColumns().clear();
        tableView.getColumns().addAll(idColumn, nameColumn, sliderColumn, probColumn);
    }

}

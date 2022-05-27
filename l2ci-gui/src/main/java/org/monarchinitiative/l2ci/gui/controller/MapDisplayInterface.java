package org.monarchinitiative.l2ci.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    Button updateButton = new Button();

    public void launchMapInterface() {
        display.add(updateButton, 0, 1);
        display.add(tableView, 0, 2);

        updateButton.setOnAction(event -> updateTable());
        updateButton.setText("Update Map Table");
        tableView.prefHeightProperty().bind(display.heightProperty());
        tableView.prefWidthProperty().bind(display.widthProperty());

        mapStage.setScene(new Scene(display, 900, 600));
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
        TableColumn<TermId, String> nameColumn = new TableColumn("Name");
        TableColumn<TermId, String> probColumn = new TableColumn("Probability");

        idColumn.setCellValueFactory(new PropertyValueFactory<>("TermId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
        probColumn.setCellValueFactory(new PropertyValueFactory<>("Probability"));

        tableView.getColumns().clear();
        tableView.getColumns().addAll(idColumn, nameColumn, probColumn);
    }

}

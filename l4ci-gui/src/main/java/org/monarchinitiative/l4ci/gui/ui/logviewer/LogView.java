package org.monarchinitiative.l4ci.gui.ui.logviewer;

/*
 * #%L
 * PhenoteFX
 * %%
 * Copyright (C) 2017 - 2018 Peter Robinson
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Duration;

public class LogView extends ListView<LogRecord> {

    private static final int MAX_ENTRIES = 10_000;

    private final static PseudoClass debug = PseudoClass.getPseudoClass("debug");
    private final static PseudoClass info = PseudoClass.getPseudoClass("info");
    private final static PseudoClass warn = PseudoClass.getPseudoClass("warn");
    private final static PseudoClass error = PseudoClass.getPseudoClass("error");
    private final static PseudoClass trace = PseudoClass.getPseudoClass("trace");
    private final static PseudoClass fatal = PseudoClass.getPseudoClass("fatal");


    private final BooleanProperty showTimestamp = new SimpleBooleanProperty(false);
    private final ObjectProperty<Level> filterLevel = new SimpleObjectProperty<>(null);
    private final BooleanProperty tail = new SimpleBooleanProperty(false);
    /** True if we want to show the class/line location in the viewer. */
    private final BooleanProperty showLocation = new SimpleBooleanProperty(false);
    private final DoubleProperty refreshRate = new SimpleDoubleProperty(60);

    private final ObservableList<LogRecord> logItems = FXCollections.observableArrayList();

    BooleanProperty showTimeStampProperty() {
        return showTimestamp;
    }

    ObjectProperty<Level> filterLevelProperty() {
        return filterLevel;
    }

    BooleanProperty tailProperty() {
        return tail;
    }

    BooleanProperty showLocationProperty() {
        return showLocation;
    }

    DoubleProperty refreshRateProperty() {
        return refreshRate;
    }

    LogView(MyLogger mylogger) {
        getStyleClass().add("log-view");
        Timeline logTransfer = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> {
                            mylogger.getLog().drainTo(logItems);

                            if (logItems.size() > MAX_ENTRIES) {
                                logItems.remove(0, logItems.size() - MAX_ENTRIES);
                            }

                            if (tail.get()) {
                                scrollTo(logItems.size());
                            }
                        }
                )
        );
        logTransfer.setCycleCount(Timeline.INDEFINITE);
        logTransfer.rateProperty().bind(refreshRateProperty());

        this.showLocationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && logTransfer.getStatus() == Animation.Status.RUNNING) {
                logTransfer.pause();
            }

            if (!newValue && logTransfer.getStatus() == Animation.Status.PAUSED && getParent() != null) {
                logTransfer.play();
            }
        });

        this.parentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                logTransfer.pause();
            } else {
                if (!showLocation.get()) {
                    logTransfer.play();
                }
            }
        });

        filterLevel.addListener((observable, oldValue, newValue) -> setItems(
                new FilteredList<>(
                        logItems,
                        logRecord ->
                                logRecord.getLevel().ordinal() >=
                                        this.filterLevel.get().ordinal()
                )
        ));
        filterLevel.set(Level.TRACE);

        setCellFactory(param -> new ListCell<>() {
            {
                showTimestamp.addListener(observable -> updateItem(this.getItem(), this.isEmpty()));
                showLocation.addListener(observable -> updateItem(this.getItem(), this.isEmpty()));
            }

            @Override
            protected void updateItem(LogRecord item, boolean empty) {
                super.updateItem(item, empty);

                pseudoClassStateChanged(debug, false);
                pseudoClassStateChanged(info, false);
                pseudoClassStateChanged(warn, false);
                pseudoClassStateChanged(error, false);
                pseudoClassStateChanged(trace, false);
                pseudoClassStateChanged(fatal, false);

                if (item == null || empty) {
                    setText(null);
                    return;
                }

                String context =
                        (item.getContext() == null)
                                ? ""
                                : item.getContext() + " ";

                String timestamp = showTimestamp.get() ? item.getTimestamp() + " " : "";

                String location = showLocation.get() ? context + " " : "";

                setText(timestamp + location + item.getMessage());

                switch (item.getLevel()) {
                    case DEBUG -> pseudoClassStateChanged(debug, true);
                    case TRACE -> pseudoClassStateChanged(trace, true);
                    case FATAL -> pseudoClassStateChanged(fatal, true);
                    case INFO -> pseudoClassStateChanged(info, true);
                    case WARN -> pseudoClassStateChanged(warn, true);
                    case ERROR -> pseudoClassStateChanged(error, true);
                }
            }
        });

    }

}

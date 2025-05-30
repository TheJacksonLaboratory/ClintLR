package org.monarchinitiative.clintlr.gui.controller;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * This class is a TextField which implements an "autocomplete" functionality, based on a supplied list of entries.
 * @author Caleb Brinkman
 */
public class AutoCompleteOntologyTextField extends TextField {


    private final ObjectProperty<Ontology> ontology = new SimpleObjectProperty<>();

    private final ObjectProperty<Map<TermId, TermId>> omim2Mondo = new SimpleObjectProperty<>();
    /** The maximum number of autocomplete entries to show (default 10). */
    private final int maxEntries;
    /**
     * The existing autocomplete entries (ontology term labels and synonyms).
     * We sort the entries in a case-insensitive way.
     */
    private final SortedSet<String> ontologyLabels = new TreeSet<>(Comparator.comparing(String::toLowerCase));
    /**
     * The existing autocomplete entries (ontology term labels and synonyms).
     * We sort the entries in a case-insensitive way.
     */
    private final SortedSet<String> omimLabels = new TreeSet<>(Comparator.comparing(String::toLowerCase));
    /** Key: an ontology term label or synonym label; value: the corresponding termid. This is used
     * to return the TermId of the selected item rather than a String*/
    private final Map<String, TermId> labelToTermMap = new HashMap<>();
    /** Key: an OMIM ID label; value: the corresponding Mondo termid. This is used
     * to return the TermId of the selected item rather than a String*/
    private final Map<String, TermId> omimLabelToMondoTermMap = new HashMap<>();
    /** The popup used to select an entry. */
    private final ContextMenu entriesPopup;


    public AutoCompleteOntologyTextField() {
        this(10);
    }

    /** Construct a new AutoCompleteTextField. */
    public AutoCompleteOntologyTextField(int maximumEntriesToShow) {
        super();
        this.maxEntries = maximumEntriesToShow;
        this.entriesPopup = new ContextMenu();
        textProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                if (getText().length() == 0) {
                    entriesPopup.hide();
                } else {
                    if (ontologyLabels.size() > 0) {
                        List<String> searchResult = List.copyOf(ontologyLabels.subSet(getText(), getText() + Character.MAX_VALUE));
                        populatePopup(searchResult);
                        if (!entriesPopup.isShowing()) {
                            entriesPopup.show(AutoCompleteOntologyTextField.this, Side.BOTTOM, 0, 0);
                        }
                    } else if (omimLabels.size() > 0) {
                        List<String> omimSearchResult = List.copyOf(omimLabels.subSet(getText(), getText() + Character.MAX_VALUE));
                        populatePopup(omimSearchResult);
                        if (!entriesPopup.isShowing()) {
                            entriesPopup.show(AutoCompleteOntologyTextField.this, Side.BOTTOM, 0, 0);
                        }
                    } else {
                        entriesPopup.hide();
                    }
                }
            }
        });

        focusedProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                entriesPopup.hide();
            }
        });

        ontology.addListener((obs, old, novel) -> {
            if (novel == null) {
                labelToTermMap.clear();
                ontologyLabels.clear();
            } else {
                for (var tid : novel.getNonObsoleteTermIds()) {
                    Term term = novel.getTermMap().get(tid);
                    labelToTermMap.put(term.getName(), tid);
                    term.getSynonyms()
                            .forEach(synonym -> labelToTermMap.put(synonym.getValue(), tid));
                }
                ontologyLabels.addAll(labelToTermMap.keySet());
            }
        });

        omim2Mondo.addListener((obs, old, novel) -> {
            if (novel == null) {
                omimLabelToMondoTermMap.clear();
                omimLabels.clear();
            } else {
                for (var entry : novel.entrySet()) {
                    TermId omimId = entry.getKey();
                    TermId mondoId = entry.getValue();
                    omimLabelToMondoTermMap.put(omimId.getValue(), mondoId);
                }
                omimLabels.addAll(omimLabelToMondoTermMap.keySet());
            }
        });
    }

    public Optional<TermId> getSelectedId() {
        String selected = getText();
        return Optional.ofNullable(labelToTermMap.get(selected));
    }

    public Optional<TermId> getSelectedMondoId() {
        String selected = getText();
        return Optional.ofNullable(omimLabelToMondoTermMap.get(selected));
    }

    public ObjectProperty<Ontology> ontologyProperty() {
        return ontology;
    }

    public ObjectProperty<Map<TermId, TermId>> omim2MondoProperty() {return omim2Mondo; }

    /**
     * Populate the entry set with the given search results.  Display is limited to 10 entries, for performance.
     * @param searchResult The set of matching strings.
     */
    private void populatePopup(List<String> searchResult) {
        int count = Math.min(searchResult.size(), maxEntries);
        List<CustomMenuItem> menuItems = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            final String result = searchResult.get(i);
            Label entryLabel = new Label(result);
            CustomMenuItem item = new CustomMenuItem(entryLabel, true);
            item.setOnAction(new EventHandler<>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    setText(result);
                    entriesPopup.hide();
                }
            });
            menuItems.add(item);
        }
        entriesPopup.getItems().setAll(menuItems);
    }



}

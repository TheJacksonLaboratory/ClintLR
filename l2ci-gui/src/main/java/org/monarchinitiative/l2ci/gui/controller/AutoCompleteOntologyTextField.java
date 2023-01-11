package org.monarchinitiative.l2ci.gui.controller;

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

    /** The maximum mnumber of autocomplete entries to show (default 10). */
    private final int maxEntries;
    /** We pass this to the constructor of {@link #ontologyLabels} to make sure it sorts in a case-insensitive way.*/
    final Comparator<String> caseInsensitiveComparator = Comparator.comparing(String::toLowerCase);
    /** The existing autocomplete entries (ontology term labels and synonyms). */
    private final SortedSet<String> ontologyLabels;
    /** Key: an ontology term label or synonym label; value: the corresponding termid. This is used
     * to return the TermId of the selected item rather than a String*/
    private final Map<String, TermId> labelToTermMap;
    /** The popup used to select an entry. */
    private final ContextMenu entriesPopup;


    public AutoCompleteOntologyTextField() {
        this(10);
    }

    /** Construct a new AutoCompleteTextField. */
    public AutoCompleteOntologyTextField(int maximumEntriesToShow) {
        super();
        this.maxEntries = maximumEntriesToShow;
        this.labelToTermMap = new HashMap<>();
        ontologyLabels = new TreeSet<>(caseInsensitiveComparator);
        entriesPopup = new ContextMenu();
        textProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                if (getText().length() == 0) {
                    entriesPopup.hide();
                } else {
                    LinkedList<String> searchResult = new LinkedList<>(ontologyLabels.subSet(getText(), getText() + Character.MAX_VALUE));
                    if (ontologyLabels.size() > 0) {
                        populatePopup(searchResult);
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
    }

    public Optional<TermId> getSelectedId() {
        String selected = getText();
        return Optional.ofNullable(this.labelToTermMap.get(selected));
    }

    /**
     * Update this autocomplete functionality with the non-obsolete terms (labels, synonyms) of an ontology
     * @param ontology A phenol ontology object
     */
    public void setOntology(Ontology ontology) {
        this.labelToTermMap.clear();
        for (var tid : ontology.getNonObsoleteTermIds()) {
            Term term = ontology.getTermMap().get(tid);
            this.labelToTermMap.put(term.getName(), tid);
            term.getSynonyms().forEach(s -> labelToTermMap.put(s.getValue(), tid));
        }
        this.ontologyLabels.addAll(labelToTermMap.keySet());
    }

    /**
     * Update this autocomplete functionality with the OMIM term labels
     * @param omimMap Map of OMIM labels and Mondo IDs
     */
    public void setOmimMap(Map<String, TermId> omimMap) {
        this.labelToTermMap.clear();
        for (Map.Entry entry : omimMap.entrySet()) {
            String omimLabel = entry.getKey().toString();
            TermId mondoID = (TermId) entry.getValue();
            this.labelToTermMap.put(omimLabel, mondoID);
        }
        this.ontologyLabels.addAll(labelToTermMap.keySet());
    }

    /**
     * Populate the entry set with the given search results.  Display is limited to 10 entries, for performance.
     * @param searchResult The set of matching strings.
     */
    private void populatePopup(List<String> searchResult) {
        List<CustomMenuItem> menuItems = new LinkedList<>();
        int count = Math.min(searchResult.size(), maxEntries);
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
        entriesPopup.getItems().clear();
        entriesPopup.getItems().addAll(menuItems);
    }



}

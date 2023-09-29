package org.monarchinitiative.clintlr.gui.controller;

import javafx.util.StringConverter;

import java.text.NumberFormat;

class RoundingDoubleStringConverter extends StringConverter<Double> {

    private static final NumberFormat FMT = NumberFormat.getNumberInstance();

    static {
        FMT.setMaximumFractionDigits(2);
    }

    @Override
    public String toString(Double aDouble) {
        return aDouble == null
                ? "NaN"
                : FMT.format(aDouble);
    }

    @Override
    public Double fromString(String s) {
        return Double.parseDouble(s);
    }
}

package org.monarchinitiative.l4ci.gui.controller;

import java.text.DecimalFormat;
import java.text.ParseException;

class ThresholdStringConverter {

    javafx.util.StringConverter<Double> converter = new javafx.util.StringConverter<>() {
        final DecimalFormat decimalFormat = new DecimalFormat();

        @Override
        public String toString(Double object) {
            return object == null ? "" : decimalFormat.format(object);
        }

        @Override
        public Double fromString(String string) {
            if (string == null || string.isEmpty())
                return 0.0;
            else {
                try {
                    double value = decimalFormat.parse(string).doubleValue();
                    if (value < 0.0) {
                        return 0.0;
                    } else {
                        return Math.min(value, 1.0);
                    }
                } catch (ParseException e) {
                    return 0.0 ;
                }
            }
        }
    };

    public javafx.util.StringConverter<Double> getConverter() {
        return converter;
    }
}

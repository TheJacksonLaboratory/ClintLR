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

/**
 * log4j levels,which are
 * TRACE &lt; DEBUG &lt; INFO &lt; WARN &lt; ERROR &lt; FATAL
 */
public enum Level {
    TRACE(1), DEBUG(2), INFO(3), WARN(4), ERROR(5),FATAL(6);

    final int val;

    Level(int l){val=l;}

    public int getVal() { return val; }


    /**
     * Convert from String to enum constant. We should never reach the
     * default case, but if we do just return TRACE
     * @param s A string representing the log level
     * @return corresponding enum constant.
     */
    public static Level string2level(String s) {
        return switch (s) {
            case "TRACE" -> TRACE;
            case "DEBUG" -> DEBUG;
            case "INFO" -> INFO;
            case "WARN" -> WARN;
            case "ERROR" -> ERROR;
            case "FATAL" -> FATAL;
            default -> TRACE;
        };
    }
}

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

import java.util.Optional;

/**
 * This class represents information in the logfile.
 * Note: It depends on the following line in logback.xml:
 * <pre><pattern>[%level] %d{HH:mm:ss.SSS} - %class{0} - %msg%n</pattern></pre>
 */
class LogRecord {
    private final String timestamp;
    private final Level  level;
    private final String context;
    private final String message;

    LogRecord(Level level, String date, String context, String message) {
        this.timestamp = date;
        this.level     = level;
        this.context   = context;
        this.message   = message;
    }

   String getTimestamp() {
        return timestamp;
    }

    Level getLevel() {
        return level;
    }

    public String getContext() {
        return context;
    }

    public String getMessage() {
        return message;
    }




    /**
     * @param line a log line such as [INFO] [2023-07-09T11:04:13] - PhenoteFxApplication - Setting version to 0.8.32
     * @return LogRecord object
     */
    public static Optional<LogRecord> fromLine(String line)  {
        // get level, e.g., from [INFO]
        int i = line.indexOf("]");
        if (i<0) {/* should never happen, each line starts with [INFO], [ERROR], etc. */
            return Optional.empty();
        }
        String level = line.substring(1,i);
        Level lvl = Level.string2level(level);
        // get date, e.g., from [2023-07-09T11:04:13]
        i = line.indexOf("[",i);
        int j =line.indexOf("]",i);
        if (i<0 || j<0)
            return Optional.empty(); /* should never happen -- data is in square brackets */
        String date = line.substring(i+1,j);
        // Get context, e.g., - PhenoteFxApplication -
        i = line.indexOf("-", j);
        j = line.indexOf("-", i+1);
        if (i<0|| j<0 ) return Optional.empty(); /* should never happen -- class/line is in square brackets */
        String context=line.substring(i+1, j).trim();
        String message=line.substring(j+2).trim();
        LogRecord record = new LogRecord(lvl,date,context,message);
        return Optional.of(record);
    }
}


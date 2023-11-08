package org.monarchinitiative.clintlr.gui.ui.logviewer;

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

public class MyLogger {

    private final Log log;
    private final String context;

    MyLogger(Log log, String context) {
        this.log = log;
        this.context = context;
    }

    public void log(LogRecord record) {
        log.offer(record);
    }

    public void debug(String msg,String date) {
        log(new LogRecord(Level.DEBUG, date,context, msg));
    }

    public void trace(String msg,String date) {
        log(new LogRecord(Level.TRACE,date, context, msg));
    }


    public void info(String msg,String date) {
        log(new LogRecord(Level.INFO,date, context, msg));
    }

    public void warn(String msg,String date) {
        log(new LogRecord(Level.WARN,date, context, msg));
    }

    public void error(String msg,String date) {
        log(new LogRecord(Level.ERROR, date,context, msg));
    }

    public Log getLog() {
        return log;
    }
}

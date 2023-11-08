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


import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

class Log {
    private static final int MAX_LOG_ENTRIES = 1_000_000;

    private final BlockingDeque<LogRecord> log = new LinkedBlockingDeque<>(MAX_LOG_ENTRIES);

    void drainTo(Collection<? super LogRecord> collection) {
        log.drainTo(collection);
    }

    void offer(LogRecord record) {
        log.offer(record);
    }
}


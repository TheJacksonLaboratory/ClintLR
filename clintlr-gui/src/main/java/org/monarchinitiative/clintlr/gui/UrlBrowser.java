package org.monarchinitiative.clintlr.gui;

import java.net.URL;

@FunctionalInterface
public interface UrlBrowser {

    /**
     * Show <em>url</em> in a system browser.
     * @param url document URL
     */
    void showDocument(URL url);

}

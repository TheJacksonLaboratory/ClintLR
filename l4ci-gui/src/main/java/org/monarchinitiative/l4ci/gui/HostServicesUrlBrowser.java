package org.monarchinitiative.l4ci.gui;

import javafx.application.HostServices;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
public class HostServicesUrlBrowser implements UrlBrowser {

    private HostServices hostServices;

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @Override
    public void showDocument(URL url) {
        if (hostServices != null) {
            hostServices.showDocument(url.toExternalForm());
        }
    }
}

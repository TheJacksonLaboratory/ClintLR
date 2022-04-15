package org.monarchinitiative.l2ci.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

@Component
public class StageInitializer implements ApplicationListener<MainApp.StageReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageInitializer.class);

    private final String applicationTitle;

    private final ApplicationContext applicationContext;


    public StageInitializer(@Value("${spring.application.ui.title}") String applicationTitle, ApplicationContext context) {
        this.applicationTitle = applicationTitle;
        this.applicationContext = context;
    }


    @Override
    public void onApplicationEvent(MainApp.StageReadyEvent event) {
        try {
            ClassPathResource l4ciResource = new ClassPathResource("fxml/MainController.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(l4ciResource.getURL());
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent parent = fxmlLoader.load();
            Stage stage = event.getStage();
            MainApp.mainStage = stage;
            stage.setScene(new Scene(parent, 1200, 900));
            stage.setResizable(true);
            stage.setTitle(applicationTitle);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

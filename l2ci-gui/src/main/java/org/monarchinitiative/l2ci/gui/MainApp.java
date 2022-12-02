package org.monarchinitiative.l2ci.gui;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

@SpringBootApplication
public class MainApp  extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainApp.class);

    private ConfigurableApplicationContext applicationContext;

    static public final String L4CI_NAME_KEY = "l4ci.name";

    static public final String L4CI_VERSION_PROP_KEY = "l4ci.version";

    static public Stage mainStage;

    public static HostServices host;


    @Override
    public void start(Stage stage) {
        applicationContext.publishEvent(new StageReadyEvent(stage));
        host = getHostServices();
    }

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(String[]::new);
        applicationContext = new SpringApplicationBuilder(MainApp.class)
                .headless(false)
                .run(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        final Properties pgProperties = applicationContext.getBean("configProperties", Properties.class);
        final File configFile = applicationContext.getBean("configFilePath", File.class);
        try (OutputStream os = Files.newOutputStream(configFile.toPath())) {
            pgProperties.store(os, "L4CI properties");
        }
        LOGGER.debug("Shutting down...");
        applicationContext.close();
        LOGGER.info("Bye!");
    }




    static class StageReadyEvent extends ApplicationEvent {
        public StageReadyEvent(Stage stage) {
            super(stage);
        }

        public Stage getStage() {
            return ((Stage) getSource());
        }
    }

    static void loadSplashScreen()  {
        Stage splashStage = new Stage();
        ClassPathResource splashResource = new ClassPathResource("fxml/splashScreen.fxml");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(splashResource.getURL());
            Parent splashRoot = fxmlLoader.load();
            Scene splashScene = new Scene(splashRoot);
            splashStage.setScene(splashScene);
            splashStage.initStyle(StageStyle.UNDECORATED);
            splashStage.show();

            setFadeInOut(splashRoot, splashStage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void setFadeInOut(Parent splashScene, Stage splashStage) {
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(3), splashScene);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), splashScene);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setCycleCount(1);
        fadeIn.play();

        fadeIn.setOnFinished((e) -> fadeOut.play());
        fadeOut.setOnFinished((e) -> splashStage.close());
    }

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}

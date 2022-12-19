package org.monarchinitiative.l2ci.gui;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.monarchinitiative.l2ci.gui.config.AppProperties;
import org.monarchinitiative.l2ci.gui.controller.MainController;
import org.monarchinitiative.l2ci.gui.resources.LiricalResources;
import org.monarchinitiative.l2ci.gui.resources.OntologyResources;
import org.monarchinitiative.l2ci.gui.resources.OptionalResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Properties;

@SpringBootApplication
public class MainApp  extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainApp.class);

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(String[]::new);
        context = new SpringApplicationBuilder(MainApp.class)
                .headless(false)
                .run(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainController.class.getResource("MainController.fxml"));
        loader.setControllerFactory(context::getBean);

        context.getBean(HostServicesUrlBrowser.class).setHostServices(getHostServices());
        AppProperties properties = context.getBean(AppProperties.class);

        Scene scene = new Scene(loader.load(), 1200, 900);
        stage.setTitle(properties.getApplicationUiTitle().concat(" :: ").concat(properties.getVersion()));
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        serializeResourceState(
                context.getBean(OptionalResources.class),
                context.getBean("configFilePath", File.class));

        LOGGER.debug("Shutting down...");
        context.close();
        LOGGER.info("Bye!");
    }

    private static void serializeResourceState(OptionalResources optionalResources,
                                               File target) throws IOException {
        Properties properties = new Properties();
        optionalResources.storeResources(properties);

        try (Writer writer = Files.newBufferedWriter(target.toPath())) {
            properties.store(writer, "L4CI properties");
        }

        LOGGER.debug("Properties saved to `{}`", target.getAbsolutePath());
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

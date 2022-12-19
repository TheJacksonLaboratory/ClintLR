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
        context.getBean(HostServicesUrlBrowser.class).setHostServices(getHostServices());

        AppProperties properties = context.getBean(AppProperties.class);
        stage.setTitle(properties.getTitle().concat(" :: v").concat(properties.getVersion()));

        Scene scene = new Scene(loadParent(), 1200, 900);
        stage.setScene(scene);
//        stage.setOnCloseRequest(); // TODO(mabeckwith) - should we implement this?
        stage.show();
    }

    private Parent loadParent() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainController.class.getResource("MainController.fxml"));
        loader.setControllerFactory(context::getBean);
        return loader.load();
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
        Properties resourceProperties = new Properties();
        // Serialize LIRICAL resources
        LiricalResources liricalResources = optionalResources.liricalResources();
        if (liricalResources.getDataDirectory() != null)
            resourceProperties.setProperty(LiricalResources.LIRICAL_DATA_PROPERTY, liricalResources.getDataDirectory().toAbsolutePath().toString());
        // TODO create new utility class (private constructor) that combines initialization and serialization
        if (liricalResources.getExomiserVariantDbFile() != null)
            resourceProperties.setProperty(LiricalResources.EXOMISER_VARIANT_PROPERTY, liricalResources.getExomiserVariantDbFile().toAbsolutePath().toString());
        if (liricalResources.getBackgroundVariantFrequencyFile() != null)
            resourceProperties.setProperty(LiricalResources.BACKGROUND_FREQUENCY_PROPERTY, liricalResources.getBackgroundVariantFrequencyFile().toAbsolutePath().toString());
        resourceProperties.setProperty(LiricalResources.PATHOGENICITY_PROPERTY, String.valueOf(liricalResources.getPathogenicityThreshold()));
        resourceProperties.setProperty(LiricalResources.DEFAULT_VARIANT_BACKGROUND_FREQUENCY_PROPERTY, String.valueOf(liricalResources.getDefaultVariantBackgroundFrequency()));
        resourceProperties.setProperty(LiricalResources.STRICT_PROPERTY, String.valueOf(liricalResources.isStrict()));
        resourceProperties.setProperty(LiricalResources.DEFAULT_ALLELE_PROPERTY, String.valueOf(liricalResources.getDefaultAlleleFrequency()));
        resourceProperties.setProperty(LiricalResources.GENOME_BUILD_PROPERTY, String.valueOf(liricalResources.getGenomeBuild()));
        resourceProperties.setProperty(LiricalResources.TRANSCRIPT_DATABASE_PROPERTY, String.valueOf(liricalResources.getTranscriptDatabase()));

        // MONDO path
        OntologyResources ontologyResources = optionalResources.ontologyResources();
        if (ontologyResources.getMondoPath() != null)
            resourceProperties.setProperty(OntologyResources.MONDO_JSON_PATH_PROPERTY, ontologyResources.getMondoPath().toAbsolutePath().toString());



        try (OutputStream os = Files.newOutputStream(target.toPath())) {
            resourceProperties.store(os, "L4CI properties");
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

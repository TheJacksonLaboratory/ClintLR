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
import org.monarchinitiative.l2ci.gui.config.LiricalProperties;
import org.monarchinitiative.l2ci.gui.controller.MainController;
import org.monarchinitiative.l2ci.gui.resources.OptionalResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Properties;

@SpringBootApplication@EnableConfigurationProperties({
        AppProperties.class,
        LiricalProperties.class
})
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
        stage.setTitle(properties.title().concat(" :: v").concat(properties.version()));

        Scene scene = new Scene(loadParent(), 1200, 900);
        stage.setScene(scene);
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
        Properties properties = new Properties();
        optionalResources.storeResources(properties);

        try (Writer writer = Files.newBufferedWriter(target.toPath())) {
            properties.store(writer, "L4CI properties");
        }

        LOGGER.debug("Properties saved to `{}`", target.getAbsolutePath());
    }


    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}

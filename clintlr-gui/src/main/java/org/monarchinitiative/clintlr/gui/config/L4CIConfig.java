package org.monarchinitiative.clintlr.gui.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class L4CIConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(L4CIConfig.class);

    /**
     * The name of property file we use to persist paths to local resources such as location of {@code hp.json}.
     */
    private static final String CONFIG_FILE_BASENAME = "l4ci.properties";

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create path to directory where we will store the resources such as {@code hp.json}.
     * The directory is created if it does not exist.
     *
     * @return path to data directory
     * @throws IOException if unable to create the data directory
     */
    @Bean
    public Path dataDirectory(File appHomeDir) throws IOException {
        Path dataDirectory = appHomeDir.toPath().resolve("data");
        if (!Files.isDirectory(dataDirectory)) {
            LOGGER.debug("Creating a new data directory at {}", dataDirectory.toAbsolutePath());
            if (!dataDirectory.toFile().mkdirs())
                throw new IOException("Unable to create data directory at %s".formatted(dataDirectory.toAbsolutePath().toString()));
        } else {
            LOGGER.debug("Using data directory at {}", dataDirectory.toAbsolutePath());
        }

        return dataDirectory;
    }

    /**
     * Properties meant to store user configuration within the app's directory
     *
     * @param configFilePath path where the properties file is supposed to be present (it's ok if the file itself doesn't exist).
     * @return {@link Properties} with user configuration
     */
    @Bean(value="configProperties")
    @Primary
    public Properties pgProperties(@Qualifier("configFilePath") File configFilePath) {
        Properties properties = new Properties();
        if (configFilePath.isFile()) {
            try (InputStream is = Files.newInputStream(configFilePath.toPath())) {
                properties.load(is);
            } catch (IOException e) {
                LOGGER.warn("Error during reading `{}`", configFilePath, e);
            }
        }
        return properties;
    }

    @Bean("configFilePath")
    public File configFilePath(@Qualifier("appHomeDir") File appHomeDir) {
        return new File(appHomeDir, CONFIG_FILE_BASENAME);
    }


    @Bean("appHomeDir")
    public File appHomeDir() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        File appHomeDir;
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) { // Unix
            appHomeDir = new File(System.getProperty("user.home") + File.separator + ".l4ci");
        } else if (osName.contains("win")) { // Windows
            appHomeDir = new File(System.getProperty("user.home") + File.separator + "l4ci");
        } else if (osName.contains("mac")) { // OsX
            appHomeDir = new File(System.getProperty("user.home") + File.separator + ".l4ci");
        } else { // unknown platform
            appHomeDir = new File(System.getProperty("user.home") + File.separator + "l4ci");
        }

        if (!appHomeDir.exists()) {
            LOGGER.debug("App home directory does not exist at {}", appHomeDir.getAbsolutePath());
            if (!appHomeDir.getParentFile().exists() && !appHomeDir.getParentFile().mkdirs()) {
                LOGGER.warn("Unable to create parent directory for app home at {}",
                        appHomeDir.getParentFile().getAbsolutePath());
                throw new IOException("Unable to create parent directory for app home at " +
                        appHomeDir.getParentFile().getAbsolutePath());
            } else {
                if (!appHomeDir.mkdir()) {
                    LOGGER.warn("Unable to create app home directory at {}", appHomeDir.getAbsolutePath());
                    throw new IOException("Unable to create app home directory at " + appHomeDir.getAbsolutePath());
                } else {
                    LOGGER.info("Created app home directory at {}", appHomeDir.getAbsolutePath());
                }
            }
        }
        return appHomeDir;
    }

}

package org.monarchinitiative.l4ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.biodownload.BioDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * A task that downloads Mondo JSON file from provided URL and stores the JSON file as {@code mondo.json}
 * in given {@code dataDirectory}.
 * <p>
 * The task optionally decompresses JSON file if it seems to be gzipped.
 */
public class DownloadMondoTask extends Task<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadMondoTask.class);

    private final URL mondoJsonUrl;
    private final Path dataDirectory;

    public DownloadMondoTask(URL mondoJsonUrl, Path dataDirectory) {
        this.mondoJsonUrl = mondoJsonUrl;
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected Path call() throws Exception {
        // Download Mondo JSON and complain in case of any errors.
        boolean looksCompressed = mondoJsonUrl.getFile().endsWith(".gz");
        String mondoJsonName = looksCompressed ? "mondo.json.gz" : "mondo.json";
        BioDownloader downloader = BioDownloader.builder(dataDirectory)
                .custom(mondoJsonName, mondoJsonUrl)
                .build();

        downloader.download();

        // This is where the Mondo file will end up.
        Path target = dataDirectory.resolve("mondo.json");
        if (looksCompressed) {
            LOGGER.debug("Decompressing Mondo JSON to {}", target.toAbsolutePath());
            // We downloaded gzipped Mondo to data directory
            Path localGzippedMondoJson = dataDirectory.resolve(mondoJsonName);
            try (InputStream is = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(localGzippedMondoJson)))) {
                Files.copy(is, target);
            }
            LOGGER.debug("Deleting temporary Mondo JSON file at {}", localGzippedMondoJson.toAbsolutePath());
            Files.deleteIfExists(localGzippedMondoJson);
        }

        return target;
    }
}

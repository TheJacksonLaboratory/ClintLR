package org.monarchinitiative.l2ci.core.io;

import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.BioDownloaderBuilder;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

public class Download {

    private static final Logger logger = LoggerFactory.getLogger(Download.class);

    String path;

    boolean overwrite;

    public Download(String path, boolean overwrite) {
        this.path = path;
        this.overwrite = overwrite;
    }

    void downloadFile(String path, String filename, String type) {
        String filePath = String.join(File.separator, path, filename);
        try {
            File target = new File(filePath);
            if (target.isFile()) {
                if (!overwrite) {
                    logger.info(filePath + " was not overwritten.");
                    return;
                }
            }
            BioDownloaderBuilder builder = BioDownloader.builder(Path.of(path));
            switch (type) {
                case "HPO" -> builder.hpoJson();
                case "HPOA" -> builder.hpDiseaseAnnotations();
                case "MONDO" -> builder.mondoJson();
            }
            BioDownloader downloader = builder.build();
            downloader.download();
        } catch (FileDownloadException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() throws FileDownloadException {
        downloadFile(path, "HP.json", "HPO");
        downloadFile(path, "PHENOTYPE.hpoa", "HPOA");
    }
}

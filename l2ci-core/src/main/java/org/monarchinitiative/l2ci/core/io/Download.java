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

    BioDownloaderBuilder builder;

    public Download(String path, boolean overwrite) {
        this.path = path;
        this.overwrite = overwrite;
        this.builder = BioDownloader.builder(Path.of(path));
    }

    void addToBuilder(String path, String filename, String type) {
        String filePath = String.join(File.separator, path, filename);
        File target = new File(filePath);
        if (target.isFile()) {
            if (!overwrite) {
                logger.info(filePath + " was not overwritten.");
                return;
            }
        }
        switch (type) {
            case "HPO" -> builder.hpoJson();
            case "HPOA" -> builder.hpDiseaseAnnotations();
            case "MONDO" -> builder.mondoJson();
        }
    }

    public void run() throws FileDownloadException {
        addToBuilder(path, "HP.json", "HPO");
        addToBuilder(path, "PHENOTYPE.hpoa", "HPOA");
        if (overwrite) {
            BioDownloader downloader = builder.build();
            downloader.download();
        }
    }
}

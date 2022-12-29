package org.monarchinitiative.l2ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.l2ci.gui.config.LiricalProperties;
import org.monarchinitiative.l2ci.gui.exception.L4CIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * A task for downloading LIRICAL resource files into a specific folder.
 */
public class DownloadLiricalData extends Task<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadLiricalData.class);

    private final LiricalProperties liricalProperties;
    private final Path destinationFolder;
    private final boolean overwrite;

    public DownloadLiricalData(LiricalProperties liricalProperties,
                               Path destinationFolder,
                               boolean overwrite) {
        this.liricalProperties = liricalProperties;
        this.destinationFolder = destinationFolder;
        this.overwrite = overwrite;
    }

    @Override
    protected Void call() throws Exception {
        LOGGER.info("Downloading LIRICAL data files to {}", destinationFolder.toAbsolutePath());
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                // Jannovar v0.35 transcript databases
                .custom("hg19_ucsc.ser", createUrlOrExplode(liricalProperties.jannovarHg19UcscUrl()))
                .custom("hg19_refseq.ser", createUrlOrExplode(liricalProperties.jannovarHg19RefseqUrl()))
                .custom("hg38_ucsc.ser", createUrlOrExplode(liricalProperties.jannovarHg38UcscUrl()))
                .custom("hg38_refseq.ser", createUrlOrExplode(liricalProperties.jannovarHg38RefseqUrl()))
                .build();
        downloader.download();
        return null;
    }


    private static URL createUrlOrExplode(String url) throws L4CIException {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new L4CIException(e);
        }
    }
}

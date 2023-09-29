package org.monarchinitiative.clintlr.core.io;

import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.BioDownloaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

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

    private static URL createUrlOrExplode(String url) throws Exception {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new Exception(e);
        }
    }

    public void run() throws Exception {
        URL mondoJsonUrl = new URL("https://storage.googleapis.com/ielis/l4ci/mondo.2022-12-01.json.gz");
        String hg19UcscURL = "https://storage.googleapis.com/ielis/jannovar/v0.35/hg19_ucsc.ser";
        String hg19RefSeqURL = "https://storage.googleapis.com/ielis/jannovar/v0.35/hg19_refseq.ser";
        String hg38UcscURL = "https://storage.googleapis.com/ielis/jannovar/v0.35/hg38_ucsc.ser";
        String hg38RefSeqURL = "https://storage.googleapis.com/ielis/jannovar/v0.35/hg38_refseq.ser";

        System.out.println("Downloading mondo.json and LIRICAL data files to {}" + Path.of(path).toAbsolutePath());
        // Download Mondo JSON and Lirical data files and complain in case of any errors.
        boolean looksCompressed = mondoJsonUrl.getFile().endsWith(".gz");
        String mondoJsonName = looksCompressed ? "mondo.json.gz" : "mondo.json";
        // TODO add Homo_sapiens file to builder
        BioDownloader downloader = BioDownloader.builder(Path.of(path))
               // .custom(mondoJsonName, mondoJsonUrl)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                .mondoJson()
                .geneInfoHuman()
                // Jannovar v0.35 transcript databases
                .custom("hg19_ucsc.ser", createUrlOrExplode(hg19UcscURL))
                .custom("hg19_refseq.ser", createUrlOrExplode(hg19RefSeqURL))
                .custom("hg38_ucsc.ser", createUrlOrExplode(hg38UcscURL))
                .custom("hg38_refseq.ser", createUrlOrExplode(hg38RefSeqURL))
                .build();

        downloader.download();

        // This is where the Mondo file will end up.
        Path target = Path.of(path).resolve("mondo.json");
        if (looksCompressed) {
            // We downloaded gzipped Mondo to data directory
            Path localGzippedMondoJson = Path.of(path).resolve(mondoJsonName);
            try (InputStream is = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(localGzippedMondoJson)))) {
                if (overwrite) {
                    Files.deleteIfExists(target);
                }
                Files.copy(is, target);
            }
            Files.deleteIfExists(localGzippedMondoJson);
        }
    }
}

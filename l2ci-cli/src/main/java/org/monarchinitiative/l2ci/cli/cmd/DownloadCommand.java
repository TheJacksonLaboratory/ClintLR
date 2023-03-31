package org.monarchinitiative.l2ci.cli.cmd;

import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.l2ci.core.io.Download;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Download a number of files needed for the analysis. We download by default to a subdirectory called
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "download", aliases = {"D"},
        mixinStandardHelpOptions = true,
        description = "Download files for L4CI")
public class DownloadCommand implements Callable<Integer>{
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);
    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    public String datadir="data";

    @CommandLine.Option(names={"-w","--overwrite"}, description = "overwrite previously downloaded files (default: ${DEFAULT-VALUE})")
    public boolean overwrite;

    @Override
    public Integer call() throws Exception {
        String homeDir = new File(".").getAbsolutePath();
        String path = String.join(File.separator, homeDir.substring(0, homeDir.length() - 2), datadir);
        logger.info(String.format("Download resource files to %s", path));
        Download download = new Download(path, overwrite);
        download.run();
        return 0;
    }

}

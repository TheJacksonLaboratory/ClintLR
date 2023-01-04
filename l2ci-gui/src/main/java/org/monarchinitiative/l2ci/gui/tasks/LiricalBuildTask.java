package org.monarchinitiative.l2ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.gui.resources.LiricalResources;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;


/**
 * Initialization of the Lirical build is being done here.
 *
 * @author <a href="mailto:daniel.danis@jax.org">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 2.0.0
 * @since 0.0
 */
public class LiricalBuildTask extends Task<Lirical> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalBuildTask.class);

    private final LiricalResources liricalResources;


    public LiricalBuildTask(LiricalResources liricalResources) {
        this.liricalResources = liricalResources;
    }

    @Override
    protected Lirical call() throws Exception {
        LOGGER.debug("Building LIRICAL from {}", liricalResources.getDataDirectory());
        LiricalBuilder builder = LiricalBuilder.builder(liricalResources.getDataDirectory());
        checkAndSetExomiserVariantDbPath(builder::exomiserVariantDbPath,
                GenomeBuild.HG19,
                liricalResources.getExomiserHg19VariantDbFile());
        checkAndSetExomiserVariantDbPath(builder::exomiserVariantDbPath,
                GenomeBuild.HG38,
                liricalResources.getExomiserHg38VariantDbFile());
        return builder.build();
    }

    private static void checkAndSetExomiserVariantDbPath(BiConsumer<GenomeBuild, Path> consumer,
                                                         GenomeBuild genomeBuild,
                                                         Path path) {
        if (path == null) {
            LOGGER.debug("Exomiser variant database path for {} is unset", genomeBuild);
        } else if (!Files.isRegularFile(path)) {
            LOGGER.warn("Ignoring Exomiser variant database path that is not a file: {}", path.toAbsolutePath());
        } else {
            LOGGER.debug("Using Exomiser variant database for {}: {}", genomeBuild, path.toAbsolutePath());
            consumer.accept(genomeBuild, path);
        }
    }

}

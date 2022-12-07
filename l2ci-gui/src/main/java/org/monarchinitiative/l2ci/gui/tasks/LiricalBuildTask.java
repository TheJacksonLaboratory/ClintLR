package org.monarchinitiative.l2ci.gui.tasks;

import javafx.concurrent.Task;
import org.monarchinitiative.l2ci.gui.resources.LiricalResources;
import org.monarchinitiative.lirical.configuration.GenotypeLrProperties;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.Lirical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

        LOGGER.debug("Using Exomiser file at {}", liricalResources.getExomiserVariantDbFile().toAbsolutePath());
        builder.exomiserVariantDatabase(liricalResources.getExomiserVariantDbFile());

        LOGGER.debug("Using GenomeBuild {}", liricalResources.getGenomeBuild());
        builder.genomeBuild(liricalResources.getGenomeBuild());

        if (liricalResources.getBackgroundVariantFrequencyFile() != null) {
            LOGGER.debug("Using background variant frequency file at {}", liricalResources.getBackgroundVariantFrequencyFile().toAbsolutePath());
            builder.backgroundVariantFrequency(liricalResources.getBackgroundVariantFrequencyFile());
        } else {
            LOGGER.debug("Using bundled background variant frequency file");
        }

        LOGGER.debug("Using pathogenicityThreshold of {}", liricalResources.getPathogenicityThreshold());
        LOGGER.debug("Using default variant background frequency of {}", liricalResources.getDefaultVariantBackgroundFrequency());
        LOGGER.debug("Using strict mode: {}", liricalResources.isStrict());
        GenotypeLrProperties gtLrProperties = new GenotypeLrProperties(liricalResources.getPathogenicityThreshold(),
                liricalResources.getDefaultVariantBackgroundFrequency(),
                liricalResources.isStrict());
        builder.genotypeLrProperties(gtLrProperties);

        LOGGER.debug("Using default allele frequency of {}", liricalResources.getDefaultAlleleFrequency());
        builder.defaultVariantAlleleFrequency(liricalResources.getDefaultAlleleFrequency());

        LOGGER.debug("Using {} transcripts", liricalResources.getTranscriptDatabase());
        builder.transcriptDatabase(liricalResources.getTranscriptDatabase());

        return builder.build();
    }

}

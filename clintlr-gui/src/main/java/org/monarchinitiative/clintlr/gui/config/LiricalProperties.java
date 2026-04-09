package org.monarchinitiative.clintlr.gui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clintlr.lirical")
public class LiricalProperties {

    private String jannovarHg19EnsemblUrl;
    private String jannovarHg19RefseqUrl;
    private String jannovarHg19RefseqCuratedUrl;
    private String jannovarHg38EnsemblUrl;
    private String jannovarHg38RefseqUrl;
    private String jannovarHg38RefseqCuratedUrl;

    public String jannovarHg19EnsemblUrl() {
        return jannovarHg19EnsemblUrl;
    }

    public void setJannovarHg19EnsemblUrl(String jannovarHg19EnsemblUrl) {
        this.jannovarHg19EnsemblUrl = jannovarHg19EnsemblUrl;
    }

    public String jannovarHg19RefseqUrl() {
        return jannovarHg19RefseqUrl;
    }

    public void setJannovarHg19RefseqUrl(String jannovarHg19RefseqUrl) {
        this.jannovarHg19RefseqUrl = jannovarHg19RefseqUrl;
    }

    public String jannovarHg19RefseqCuratedUrl() {
        return jannovarHg19RefseqCuratedUrl;
    }

    public void setJannovarHg19RefseqCuratedUrl(String jannovarHg19RefseqCuratedUrl) {
        this.jannovarHg19RefseqCuratedUrl = jannovarHg19RefseqCuratedUrl;
    }

    public String jannovarHg38EnsemblUrl() {
        return jannovarHg38EnsemblUrl;
    }

    public void setJannovarHg38EnsemblUrl(String jannovarHg38EnsemblUrl) {
        this.jannovarHg38EnsemblUrl = jannovarHg38EnsemblUrl;
    }

    public String jannovarHg38RefseqUrl() {
        return jannovarHg38RefseqUrl;
    }

    public void setJannovarHg38RefseqUrl(String jannovarHg38RefseqUrl) {
        this.jannovarHg38RefseqUrl = jannovarHg38RefseqUrl;
    }

    public String jannovarHg38RefseqCuratedUrl() {
        return jannovarHg38RefseqCuratedUrl;
    }

    public void setJannovarHg38RefseqCuratedUrl(String jannovarHg38RefseqCuratedUrl) {
        this.jannovarHg38RefseqCuratedUrl = jannovarHg38RefseqCuratedUrl;
    }
}

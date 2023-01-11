package org.monarchinitiative.l2ci.gui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "l4ci.lirical")
public class LiricalProperties {

    private String jannovarHg19UcscUrl;
    private String jannovarHg19RefseqUrl;
    private String jannovarHg38UcscUrl;
    private String jannovarHg38RefseqUrl;

    public String jannovarHg19UcscUrl() {
        return jannovarHg19UcscUrl;
    }

    public void setJannovarHg19UcscUrl(String jannovarHg19UcscUrl) {
        this.jannovarHg19UcscUrl = jannovarHg19UcscUrl;
    }

    public String jannovarHg19RefseqUrl() {
        return jannovarHg19RefseqUrl;
    }

    public void setJannovarHg19RefseqUrl(String jannovarHg19RefseqUrl) {
        this.jannovarHg19RefseqUrl = jannovarHg19RefseqUrl;
    }

    public String jannovarHg38UcscUrl() {
        return jannovarHg38UcscUrl;
    }

    public void setJannovarHg38UcscUrl(String jannovarHg38UcscUrl) {
        this.jannovarHg38UcscUrl = jannovarHg38UcscUrl;
    }

    public String jannovarHg38RefseqUrl() {
        return jannovarHg38RefseqUrl;
    }

    public void setJannovarHg38RefseqUrl(String jannovarHg38RefseqUrl) {
        this.jannovarHg38RefseqUrl = jannovarHg38RefseqUrl;
    }
}

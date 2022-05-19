package org.monarchinitiative.l2ci.cli.cmd;


import org.monarchinitiative.l2ci.core.io.HPOParser;
import org.monarchinitiative.l2ci.core.mondo.MondoStats;
import org.monarchinitiative.l2ci.core.mondo.PhenotypicSeries;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "phenotype", aliases = {"P"},
        mixinStandardHelpOptions = true,
        description = "Dump Phenotypic Series to shell")
public class PhenotypicSeriesCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-m", "--mondo"},
            required = true,
            description = "path to mondo.json file")
    private String mondoJsonPath;



    @Override
    public Integer call() throws Exception {
        HPOParser parser = new HPOParser(mondoJsonPath);
        Ontology ont = parser.getHPO();
        if (ont != null) {
            PhenotypicSeries ps = new PhenotypicSeries(ont);
            ps.run();
        }
        return null;
    }
}

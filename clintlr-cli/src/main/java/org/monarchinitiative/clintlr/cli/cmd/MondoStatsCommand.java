package org.monarchinitiative.clintlr.cli.cmd;


import org.monarchinitiative.clintlr.core.io.HPOParser;
import org.monarchinitiative.clintlr.core.mondo.MondoStats;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "stats", aliases = {"S"},
        mixinStandardHelpOptions = true,
        description = "Dump Mondo stats to shell")
public class  MondoStatsCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-m", "--mondo"},
            required = true,
            description = "path to mondo.json file")
    private String mondoJsonPath;



    @Override
    public Integer call() throws Exception {
        HPOParser parser = new HPOParser(mondoJsonPath);
        Ontology ont = parser.getHPO();
        if (ont != null) {
            MondoStats mondo = new MondoStats(ont);
            mondo.run();
        }
        return null;
    }
}

package org.monarchinitiative.l2ci.cli.cmd;


import org.monarchinitative.l2ci.mondo.MondoStats;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "stats", aliases = {"S"},
        mixinStandardHelpOptions = true,
        description = "Dump Mondo stats to shell")
public class MondoStatsCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-m", "--mondo"},
            required = true,
            description = "path to mondo.json file")
    private String mondoJsonPath;



    @Override
    public Integer call() throws Exception {
        // import Mondo file and create ontology
       // MondoStats stats = new MondoStats();
        // dump to shell.

        return null;
    }
}

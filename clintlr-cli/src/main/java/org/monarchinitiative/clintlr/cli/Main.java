package org.monarchinitiative.clintlr.cli;


import org.monarchinitiative.clintlr.cli.cmd.*;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "clintlr",
        mixinStandardHelpOptions = true,
        version = "0.3.0",
        description = "LIRICAl for clinical intuition")
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }
        CommandLine cline = new CommandLine(new Main())
                .addSubcommand("download", new DownloadCommand())
                .addSubcommand("stats", new MondoStatsCommand())
                .addSubcommand("phenotype", new PhenotypicSeriesCommand())
                .addSubcommand("batch", new BatchAnalysisCommand())
                .addSubcommand("benchmark", new BenchmarkCommand())
                .addSubcommand("genes", new GeneAnalysisCommand())
                .addSubcommand("ranges", new IntuitionRangesCommand());
        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // work done in subcommands
        return 0;
    }

}

package org.monarchinitiative.l2ci.cli;


import org.monarchinitiative.l2ci.cli.cmd.BenchmarkCommand;
import org.monarchinitiative.l2ci.cli.cmd.DownloadCommand;
import org.monarchinitiative.l2ci.cli.cmd.MondoStatsCommand;
import org.monarchinitiative.l2ci.cli.cmd.PhenotypicSeriesCommand;
import org.monarchinitiative.l2ci.core.mondo.PhenotypicSeries;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "l2ci",
        mixinStandardHelpOptions = true,
        version = "0.0.1",
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
                .addSubcommand("benchmark", new BenchmarkCommand());
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

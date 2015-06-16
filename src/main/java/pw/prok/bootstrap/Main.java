package pw.prok.bootstrap;

import org.apache.commons.cli.*;
import pw.prok.bootstrap.tasks.*;

import java.util.Arrays;

public class Main {
    public final Options options;

    public final Option serverDir;
    public final Option jvmArgs;

    public final Option installKCauldron;
    public final Option runKCauldron;
    public final Option installServer;
    public final Option libraries;

    public final CommandLineParser parser;
    public final HelpFormatter helpFormatter;

    public CommandLine cli;
    private boolean wasExecuted = false;

    public Main() {
        options = new Options();

        serverDir = new Option("d", "serverDir", true, "Server root directory");
        serverDir.setArgName("dir");
        options.addOption(serverDir);

        jvmArgs = new Option("j", "jvmArg", true, "Server's JVM arguments");
        jvmArgs.setArgName("args");
        options.addOption(jvmArgs);

        installKCauldron = new Option("k", "installKCauldron", true, "Install specified or latest KCauldron");
        installKCauldron.setArgName("version");
        installKCauldron.setOptionalArg(true);
        options.addOption(installKCauldron);

        runKCauldron = new Option("r", "runKCauldron", true, "Install & run specified or latest KCauldron");
        runKCauldron.setArgName("version");
        runKCauldron.setOptionalArg(true);
        options.addOption(runKCauldron);

        installServer = new Option("i", "installServer", true, "Install custom server");
        installServer.setArgName("server file or url");
        options.addOption(installServer);

        libraries = new Option("l", "libraries", true, "Install specified libraries into server dir");
        libraries.setArgName("libraries");
        libraries.setValueSeparator(';');
        options.addOption(libraries);

        parser = new DefaultParser();
        helpFormatter = new HelpFormatter();
    }

    public static void main(String[] args) {
        new Main().start(args);
    }

    private void start(String[] args) {
        try {
            cli = parser.parse(options, args, true);
            if (cli.hasOption(libraries.getOpt())) {
                run(new Libraries());
            }
            if (cli.hasOption(installKCauldron.getOpt())) {
                run(new InstallKCauldron());
            }
            if (cli.hasOption(runKCauldron.getOpt())) {
                run(new RunKCauldron());
            }
            if (cli.hasOption(installServer.getOpt())) {
                run(new InstallServer());
            }
            if (!wasExecuted) {
                printHelp();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            printHelp();
        }
    }

    public void run(DefaultTask task) {
        task.setMain(this);
        try {
            task.make();
        } catch (Exception e) {
            e.printStackTrace();
        }
        wasExecuted = true;
    }

    private void printHelp() {
        helpFormatter.printHelp("kbootstrap", options, true);
    }
}

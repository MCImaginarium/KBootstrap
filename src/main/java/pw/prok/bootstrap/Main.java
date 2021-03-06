package pw.prok.bootstrap;

import org.apache.commons.cli.*;
import pw.prok.bootstrap.tasks.*;

import java.io.File;
import java.util.Arrays;

public class Main {
    public final Options options;

    public final Option serverDir;
    public final Option binDir;
    public final Option jvmArgs;
    public final Option serverSymlinks;
    public final Option pidFile;
    public final Option warmRoast;

    public final Option installKCauldron;
    public final Option runKCauldron;
    public final Option installServer;
    public final Option runServer;
    public final Option libraries;

    public final CommandLineParser parser;
    public final HelpFormatter helpFormatter;

    public CommandLine cli;
    private boolean wasExecuted = false;

    public static Main instance;

    public Main() {
        options = new Options();

        serverDir = new Option("d", "serverDir", true, "Server root directory");
        serverDir.setArgName("dir");
        options.addOption(serverDir);

        binDir = new Option("b", "binDir", true, "Server bin directory");
        binDir.setArgName("dir");
        options.addOption(binDir);

        jvmArgs = new Option("j", "jvmArg", true, "Server's JVM arguments");
        jvmArgs.setArgName("args");
        options.addOption(jvmArgs);

        serverSymlinks = new Option("s", "serverSymlinks", true, "Server's symlinks");
        serverSymlinks.setArgName("paths");
        serverSymlinks.setValueSeparator(File.pathSeparatorChar);
        options.addOption(serverSymlinks);

        pidFile = new Option("p", "pidFile", true, "PID file for server");
        pidFile.setArgName("file");
        options.addOption(pidFile);

        warmRoast = new Option("w", "warmRoast", false, "Run warmroast for this launch (useful with -r and -c)");
        options.addOption(warmRoast);

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

        runServer = new Option("c", "runServer", true, "Install & run custom server");
        runServer.setArgName("server file or url");
        options.addOption(runServer);

        libraries = new Option("l", "libraries", true, "Install specified libraries into server dir");
        libraries.setArgName("libraries");
        libraries.setValueSeparator(File.pathSeparatorChar);
        options.addOption(libraries);

        parser = new DefaultParser();
        helpFormatter = new HelpFormatter();
    }

    public static void main(String[] args) {
        (instance = new Main()).start(args);
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
            if (cli.hasOption(runServer.getOpt())) {
                run(new RunServer());
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

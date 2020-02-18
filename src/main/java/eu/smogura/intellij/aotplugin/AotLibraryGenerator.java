package eu.smogura.intellij.aotplugin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.ExecutionModes;
import com.intellij.execution.OutputListener;
import com.intellij.execution.RunManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.terminal.TerminalExecutionConsole;
import com.intellij.ui.MessageException;
import eu.smogura.intellij.aotplugin.AotConfiguration.AotLibraryInfo;
import java.awt.EventQueue;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AotLibraryGenerator {
  private static final Logger logger = Logger.getInstance(AotLibraryGenerator.class);

  private final static ExecutorService oneThreadExecutor = Executors.newSingleThreadExecutor();

  private final Module module;
  private final RunConfiguration runConfiguration;
  private final AotApplicationConfigurationAdapter runConfigurationAdapter;

  /** Set during execution to output library path. */
  private String libraryPath;

  public AotLibraryGenerator(RunConfiguration runConfiguration) {
    this.runConfiguration = runConfiguration;

    if (this.runConfiguration instanceof ApplicationConfiguration) {
      ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) this.runConfiguration;
      this.module = applicationConfiguration.getConfigurationModule().getModule();
      this.runConfigurationAdapter = new AotApplicationConfigurationAdapter(applicationConfiguration);
    } else {
      throw new IllegalStateException("Configuration "
          + runConfiguration.getName() + " is not supported");
    }
  }

  public void generateAotLibrary() {
    final var commandLine = new GeneralCommandLine(this.findJaotc());
    final var classPath = this.buildClassPath();

    // Add classpath as -J to pass to underlying VM
    // Without this JAOTC will not be able to resolve classes referenced
    // from other classes
    commandLine.addParameters("-J-cp", "-J" + classPath);

    // Add a classpath to allow scanning JARs
    commandLine.addParameters("--jar", classPath);
    commandLine.addParameters("--module", jdkModulesList(":"));
    commandLine.addParameters("-J--add-modules", "-J" + jdkModulesList(","));

    // Prepare file with compile commands
    final var compilesCommandsPath = this.prepareCompileCommands();
    commandLine.addParameters("--compile-commands", compilesCommandsPath.getAbsolutePath());

    // Add VM parameters from run configuration wrapped with -J flag
    commandLine.addParameters(this.createCompileCommandsFromConfiguration());
    commandLine.addParameters("--compile-for-tiered");
    commandLine.addParameters("--ignore-errors");
    commandLine.addParameters("--info");
    commandLine.addParameters("--verbose");

    commandLine.addParameters("-J-Dgraal.ProfileSimpleMethods=false");
    var outputLib = new File(
      this.outputFolder(),
      this.libraryName());

    this.libraryPath = outputLib.getAbsolutePath();

    commandLine.addParameters("--output", this.libraryPath);

    var cmdString = commandLine.getCommandLineString();

    final var ptyCommandLine = new PtyCommandLine(commandLine);
    ptyCommandLine.withConsoleMode(true);

    oneThreadExecutor.submit(() -> {
      try {
        executeUsingExecutionHelper(ptyCommandLine);
      } catch (ExecutionException e) {
        Messages.showErrorDialog(e.getMessage(), "AOT Error");
      }
    });
  }

  protected void executeUsingConsole(final PtyCommandLine ptyCommandLine) throws ExecutionException {
    final var processHandler = new ColoredProcessHandler(ptyCommandLine);
    final var console = new TerminalExecutionConsole(this.module.getProject(), processHandler);
    console.attachToProcess(processHandler);
  }

  protected void executeUsingExecutionHelper(final PtyCommandLine ptyCommandLine) throws ExecutionException {
    final var stdCombined = new StringBuilder(2048);
    final var processHandler = new ColoredProcessHandler(ptyCommandLine);
    final var outputCollector = new OutputListener(stdCombined, stdCombined);

    processHandler.addProcessListener(outputCollector);
    processHandler.startNotify();

    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        final var exitCode = event.getExitCode();
        if (exitCode != 0) {
          EventQueue.invokeLater(() -> {
            Messages.showErrorDialog(
              "JAOTC Exited with status code " + exitCode,
              "AOT - Library build failure"
            );
          });

          logger.error("JAOTC Exited with status code " + exitCode
            + ": " + outputCollector.getOutput().getStdout().toString());
        } else {
          updateConfigurationWithAotResults("TEST-DIGEST", libraryPath);
        }
      }
    });
    ExecutionHelper.executeExternalProcess(
      this.module.getProject(),
      processHandler,
      new ExecutionModes.BackGroundMode(true, "AOT"),
      ptyCommandLine
    );

    processHandler.waitFor();
  }

  protected void executeUsingRunProfile() throws ExecutionException {
    ExecutionEnvironmentBuilder.createOrNull(
      this.module.getProject(),
      DefaultRunExecutor.getRunExecutorInstance(),
      new AotRunProfile(this.module)
    ).buildAndExecute();
  }

  protected String libraryName() {
    var name = this.runConfiguration.getName();

    return name
      .replaceAll("\\s", "-")
      .replaceAll("/", "-")
      .replaceAll("\\\\", "-")
      + "__"
      + AotConfigDigester.digestConfiguration(this.runConfigurationAdapter);
  }

  /**
   * Updates run configuration storing specific information about generated AOT library.
   */
  protected void updateConfigurationWithAotResults(String configDigest, String libraryPath) {
    final var runManager = RunManager.getInstance(this.module.getProject());
    final var runConfiguration = runManager.findConfigurationByName(this.runConfiguration.getName());
    if (runConfiguration != null) {
      // TODO Here configuration could change, as it's async process - add digesting check again
      final var configuration = runConfiguration.getConfiguration();
      if (configuration instanceof RunConfigurationBase<?>) {
        final var configurationBase = (RunConfigurationBase<?>) configuration;
        final var aotConfig = AotIntellijHelpers.getOrCreateAotConfiguration(configurationBase);

        aotConfig.setLibraryInfo(AotLibraryInfo.builder()
            .configDigest(configDigest)
            .libraryPath(libraryPath)
            .build()
        );

        runManager.makeStable(runConfiguration);
      }
    }
  }

  protected String findJaotc() {
    String binPath = this.runConfigurationAdapter.getJdkBinPath();
    var jaotcPath = new File(binPath, "jaotc");

    if (jaotcPath.canExecute()) {
      return jaotcPath.getAbsolutePath();
    } else {
      throw new MessageException(jaotcPath + " is not executable");
    }
  }

  protected String buildClassPath() {
    return this.runConfigurationAdapter.getClassPath()
        .stream()
        .filter(cp -> cp.endsWith(".jar"))
        // Those packages causes problems with JDK 11
        // We assume those packages has been added by Maven or Gradle, so it will
        // contain this name
        .filter(cp -> !cp.contains("org.graalvm."))
        .collect(Collectors.joining(":"));
  }

  protected File prepareCompileCommands() {
    try {
      var compileCommands = FileUtilRt.createTempFile("jaotc-plugin", "compileCommands");
      try (FileWriter out = new FileWriter(compileCommands)) {
        appendCompileCommandsForProfile(out, "java-base");
        appendCompileCommandsForProfile(out, "micronaut");
        appendCompileCommandsForProfile(out, "io.micronaut");
        appendCompileCommandsForProfile(out, "micronaut-small-profile");
        appendCompileCommandsForProfile(out, "spring-5");
      }

      return compileCommands;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  protected void appendCompileCommandsForProfile(FileWriter out, String profileName)
      throws IOException {
    try (InputStream resourceAsStream = this.compileCommandsResource(profileName);
        InputStreamReader reader = new InputStreamReader(resourceAsStream)) {
      IOUtils.copy(reader, out);
      out.write("\n");
    }
  }

  private InputStream compileCommandsResource(String profileName) {
    final var resourceName = "/eu/smogura/intellij/aotplugin/" + profileName + ".aot.txt";

    return this.getClass().getResourceAsStream(resourceName);
  }

  protected File outputFolder() {
    final var compilerProjectExtension =
        CompilerProjectExtension.getInstance(this.module.getProject());
    final var projectOutputDir = compilerProjectExtension.getCompilerOutput().getPath();
    final var aotFileDir = new File(projectOutputDir, ".aot/libs");

    FileUtil.createDirectory(aotFileDir);

    return aotFileDir;
  }

  protected List<String> createCompileCommandsFromConfiguration() {
    final var vmArgs = this.runConfigurationAdapter.getVMArguments();
    final var aotOptions = new ArrayList<String>(vmArgs.size());
    for (final var arg : vmArgs) {
      aotOptions.add("-J" + arg);
    }

    return aotOptions;
  }

  /** List of JDK modules to use during AOT. */
  protected String jdkModulesList(String separator) {
    return String.join(separator, "java.base", "java.xml");
  }

  public static boolean isConfigurationApplicable(RunConfiguration runConfiguration) {
    return (runConfiguration instanceof ApplicationConfiguration);
  }
}

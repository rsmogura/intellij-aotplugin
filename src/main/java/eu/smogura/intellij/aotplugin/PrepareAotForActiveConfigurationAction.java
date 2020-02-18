package eu.smogura.intellij.aotplugin;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class PrepareAotForActiveConfigurationAction extends AnAction {
  public static final String CANT_RUN_INFO = ""
      + "No active configuration, or configuration not supported.\n"
      + "Please check if your configuration setting if AOT is allowed";

  @Override
  public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
    for (var project : ProjectManager.getInstance().getOpenProjects()) {
      final var runManager = RunManager.getInstance(anActionEvent.getProject());
      Optional.ofNullable(runManager.getSelectedConfiguration())
          .map(RunnerAndConfigurationSettings::getConfiguration)
          .filter(AotLibraryGenerator::isConfigurationApplicable)
          .ifPresentOrElse(
              config -> {
                new AotLibraryGenerator(config).generateAotLibrary();
              },
              () -> {
                Messages.showInfoMessage(CANT_RUN_INFO, "AOT");
              });

//      Can be used for generate AOT for all. So just keep this code commented out
//      runManager.getAllConfigurationsList().stream()
//        .filter(c -> c instanceof ApplicationConfiguration)
//        .map(c -> (ApplicationConfiguration) c)
//        .forEach(c -> {
//          new AotLibraryBuilderService(c.getConfigurationModule().getModule(), c).generateAotLibrary();
//        });
    }
  }
}

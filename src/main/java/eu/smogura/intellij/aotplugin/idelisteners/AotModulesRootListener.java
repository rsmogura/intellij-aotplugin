package eu.smogura.intellij.aotplugin.idelisteners;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import org.jetbrains.annotations.NotNull;

public class AotModulesRootListener implements ModuleRootListener {

  public AotModulesRootListener() {
  }

  @Override
  public void rootsChanged(@NotNull ModuleRootEvent event) {
    Project project = (Project) event.getSource();
    ProjectRootManager.getInstance(project).orderEntries().forEachModule(
      m -> {
        var configs = RunManager.getInstance(project).getAllConfigurationsList();
        return true;
      }
    );
  }
}

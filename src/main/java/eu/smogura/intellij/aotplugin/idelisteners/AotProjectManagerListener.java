package eu.smogura.intellij.aotplugin.idelisteners;

import com.intellij.ProjectTopics;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Listener to listen for projects open and close. Used to register listeners for dependencies changes.
 */
public class AotProjectManagerListener implements ProjectManagerListener {
  private final Logger logger = Logger.getInstance(AotAppInitListener.class);

  @Override
  public void projectOpened(@NotNull Project project) {
    logger.info("Project opened, registering roots listener");
    project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new AotModulesRootListener());
  }
}

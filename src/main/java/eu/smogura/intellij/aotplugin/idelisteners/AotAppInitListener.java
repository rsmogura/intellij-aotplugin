package eu.smogura.intellij.aotplugin.idelisteners;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.messages.MessageBusConnection;

public class AotAppInitListener implements ApplicationInitializedListener {
  private Logger logger = Logger.getInstance(AotAppInitListener.class);

  @Override
  public void componentsInitialized() {
    logger.info("Initializing plugin " + AotAppInitListener.class.getName());

    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();

    logger.info("Registering projects listeners");
    connection.subscribe(ProjectManager.TOPIC, new AotProjectManagerListener());
  }
}

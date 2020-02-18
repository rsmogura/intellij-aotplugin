package eu.smogura.intellij.aotplugin;

import com.intellij.execution.configurations.RunConfigurationBase;

public class AotIntellijHelpers {

  /**
   * Searches for AOT configuration in Run Configuration. If AOT config does not exist
   * creates default one and adds to Run Configuration
   */
  public static AotConfiguration getOrCreateAotConfiguration(final RunConfigurationBase<?> config) {
    var aotConfig = config.getUserData(AotConfiguration.KEY);
    if (aotConfig == null) {
      aotConfig = new AotConfiguration();
      config.putUserData(AotConfiguration.KEY, aotConfig);
    }

    return aotConfig;
  }
}

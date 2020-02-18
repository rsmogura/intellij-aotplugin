package eu.smogura.intellij.aotplugin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Location;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.options.SettingsEditor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AotRunConfigurationExtension extends RunConfigurationExtension {
  @Nullable
  @Override
  protected <P extends RunConfigurationBase<?>> SettingsEditor<P> createEditor(@NotNull P configuration) {
    if (configuration instanceof ApplicationConfiguration) {
      ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) configuration;
      return (SettingsEditor<P>) new AotConfigurationEditor(
        applicationConfiguration.getProject(),
        applicationConfiguration.getConfigurationModule().getModule());
    }

    return null;
  }

  @Nullable
  @Override
  protected String getEditorTitle() {
    return "AOT";
  }

  @Override
  public <T extends RunConfigurationBase> void updateJavaParameters(@NotNull T t, @NotNull JavaParameters javaParameters, RunnerSettings runnerSettings) throws ExecutionException {
    if (t instanceof ApplicationConfiguration) {
      ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) t;
      final var aotConfig = AotIntellijHelpers.getOrCreateAotConfiguration(applicationConfiguration);
      if (aotConfig.isEnabled()) {
        updateParametersWithAotLibraryInfo(javaParameters, aotConfig);
      }
    }
  }

  @Override
  public boolean isApplicableFor(@NotNull RunConfigurationBase<?> runConfigurationBase) {
    return runConfigurationBase instanceof ApplicationConfiguration;
  }

  @Override
  protected void extendCreatedConfiguration(@NotNull RunConfigurationBase<?> configuration, @NotNull Location location) {
    super.extendCreatedConfiguration(configuration, location);
  }

  @Override
  protected void readExternal(@NotNull RunConfigurationBase<?> runConfiguration, @NotNull Element element) {
    AotIntellijHelpers.getOrCreateAotConfiguration(runConfiguration).readExternal(element);
  }

  @Override
  protected void writeExternal(@NotNull RunConfigurationBase<?> runConfiguration, @NotNull Element element) {
    AotIntellijHelpers.getOrCreateAotConfiguration(runConfiguration).writeExternal(element);
  }

  private void updateParametersWithAotLibraryInfo(
      @NotNull JavaParameters javaParameters,
      AotConfiguration aotConfig) {
    final var aotLibraryInfo = aotConfig.getLibraryInfo();
    if (aotLibraryInfo != null && aotLibraryInfo.getLibraryPath() != null) {
      javaParameters
          .getVMParametersList()
          .add("-XX:AOTLibrary=" + aotLibraryInfo.getLibraryPath());
    }
  }
}

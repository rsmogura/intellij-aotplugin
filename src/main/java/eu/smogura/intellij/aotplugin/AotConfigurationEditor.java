package eu.smogura.intellij.aotplugin;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AotConfigurationEditor extends SettingsEditor<RunConfigurationBase<?>> {
  private final Project project;
  private final Module module;

  private JCheckBox enableAheadOfTimeCheckBox;
  private JPanel rootPanel;
  private JTextField pathTextField;

  public AotConfigurationEditor(Project project, Module module) {
    //TODO Clean constructor
    this.project = project;
    this.module = module;
  }

  @Override
  protected void resetEditorFrom(@NotNull RunConfigurationBase<?> s) {
    var aotConfiguration = AotIntellijHelpers.getOrCreateAotConfiguration(s);
    this.enableAheadOfTimeCheckBox.setSelected(aotConfiguration.isEnabled());

    final var libraryInfo = aotConfiguration.getLibraryInfo();
    if (libraryInfo != null) {
      this.pathTextField.setText("" + libraryInfo.getLibraryPath());
    } else {
      this.pathTextField.setText("(AOT Library not generated, yet)");
    }
  }

  @Override
  protected void applyEditorTo(@NotNull RunConfigurationBase<?> s) throws ConfigurationException {
    var aotConfiguration = AotIntellijHelpers.getOrCreateAotConfiguration(s);
    aotConfiguration.setEnabled(this.enableAheadOfTimeCheckBox.isSelected());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return rootPanel;
  }

}

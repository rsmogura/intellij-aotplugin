package eu.smogura.intellij.aotplugin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.ModuleRunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AotRunProfile implements ModuleRunProfile {
  private final Module module;

  public AotRunProfile(Module module) {
    this.module = module;
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
    return new CommandLineState(environment) {
      @NotNull
      @Override
      protected ProcessHandler startProcess() throws ExecutionException {
        return null;
      }
    };
  }

  @NotNull
  @Override
  public String getName() {
    return "Generate AOT Library";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return null;
  }
}

package eu.smogura.intellij.aotplugin;

import com.intellij.execution.CantRunException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.MessageException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AotApplicationConfigurationAdapter {

  private static final Logger logger = Logger.getInstance(AotApplicationConfigurationAdapter.class);

  /**
   * Configuration used to build this adapter.
   */
  private final ApplicationConfiguration runConfiguration;

  /**
   * Module to use when building AOT library.
   */
  private final Module module;

  /**
   * Holds Java Parameters used to determine digest, and `jaotc` params. Lazy initialized. Use
   * `getJavaParameters`.
   */
  private JavaParameters javaParameters;

  private List<String> jvmArguments;
  private List<String> classPathList;
  private String jdkBinPath;

  public AotApplicationConfigurationAdapter(@NonNull ApplicationConfiguration runConfiguration) {
    this.runConfiguration = runConfiguration;
    this.module = runConfiguration.getConfigurationModule().getModule();
  }


  private JavaParameters getJavaParameters() {
    if (this.javaParameters == null) {
      this.javaParameters = new JavaParameters();
      try {
        this.javaParameters.configureByModule(this.module, JavaParameters.JDK_AND_CLASSES);
      } catch (CantRunException e) {
        throw new RuntimeException(e);
      }
    }

    return javaParameters;
  }

  /**
   * Get original VM arguments used in configuration.
   *
   * @return
   */
  public List<String> getVMArguments() {
    if (jvmArguments == null) {
      jvmArguments = new ArrayList<>(getJavaParameters().getVMParametersList().getList());
    }

    return jvmArguments;
  }

  public List<String> getClassPath() {
    if (classPathList == null) {
      classPathList = new ArrayList<>(getJavaParameters().getClassPath().getPathList());
    }

    return classPathList;
  }

  /**
   * Gets path identifying SDK bin path.
   */
  public String getJdkBinPath() {
    if (jdkBinPath == null) {
      jdkBinPath = findJdkBinPath();
    }

    return jdkBinPath;
  }

  private String findJdkBinPath() {
    Sdk sdk = ModuleRootManager.getInstance(this.module).getSdk();
    SdkTypeId sdkType = sdk.getSdkType();
    if (sdkType instanceof JavaSdkType) {
      JavaSdkType javaSdkType = (JavaSdkType) sdkType;
      return javaSdkType.getBinPath(sdk);
    }

    throw new MessageException("SDK " + sdk.getName() + " is not supported for AOT");
  }

  public String getName() {
    return this.runConfiguration.getName();
  }

}

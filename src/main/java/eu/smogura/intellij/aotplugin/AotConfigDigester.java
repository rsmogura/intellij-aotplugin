package eu.smogura.intellij.aotplugin;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;

/** Digests configuration, to fast determine changes in library. */
public class AotConfigDigester {
  public static String digestConfiguration(AotApplicationConfigurationAdapter configurationAdapter) {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");

      // Append configuration name
      digest.update(configurationAdapter.getName().getBytes("UTF-8"));

      // Append JDK bin path (we assume each JDK will be in new folder,
      // it may not be a case for some users
      digest.update(configurationAdapter.getJdkBinPath().getBytes("UTF-8"));

      // Append class path
      for (var cp : configurationAdapter.getClassPath()) {
        digest.update(cp.getBytes("UTF-8"));
      }

      // Append VM arguments
      for (var vmArg : configurationAdapter.getVMArguments()) {
        digest.update(vmArg.getBytes("UTF-8"));
      }

      // Generate digest and return value
      final var digestBytes = digest.digest();

      return Hex.encodeHexString(digestBytes);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

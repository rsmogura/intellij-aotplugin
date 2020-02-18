package eu.smogura.intellij.aotplugin;

import com.intellij.openapi.util.Key;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdom.Element;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AotConfiguration {
  public static final Key<AotConfiguration> KEY = new Key<>("AotConfiguration");

  private boolean enabled = true;

  private volatile AotLibraryInfo libraryInfo;

  public void readExternal(Element element) {
    enabled = Optional.ofNullable(element.getAttribute("enabled"))
        .map(a -> Boolean.valueOf(a.getValue()))
        .orElse(Boolean.TRUE);

    final var configDigestAttr = element.getAttribute("config-digest");
    final var libraryPathAttr = element.getAttribute("library-path");

    if (configDigestAttr != null && libraryPathAttr != null) {
      this.libraryInfo = AotLibraryInfo.builder()
          .configDigest(configDigestAttr.getValue())
          .libraryPath(libraryPathAttr.getValue())
          .build();
    }
  }

  public void writeExternal(Element element) {
    element.setAttribute("enabled", Boolean.toString(this.enabled));
    final var info = libraryInfo;
    if (info != null) {
      element.setAttribute("config-digest", info.configDigest);
      element.setAttribute("library-path", info.libraryPath);
    }
  }

  @Builder
  @Getter
  public static class AotLibraryInfo {
    private String configDigest;
    private String libraryPath;
  }
}

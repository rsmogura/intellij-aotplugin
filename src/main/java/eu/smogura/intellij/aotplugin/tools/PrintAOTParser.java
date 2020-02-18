package eu.smogura.intellij.aotplugin.tools;

import com.intellij.formatting.fileSet.PatternDescriptor;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses Java output for PrintAOT options, to discover loaded classes
 */
public class PrintAOTParser extends BasicParser {
  public static final Pattern AOT_LINE_PATTERN = Pattern.compile(
    "^\\[([0-9.])+s\\]\\[\\w+\\]\\[[\\w.,]+\\]\\s([\\w\\d\\$.$]+).*"
  );


  @Override
  protected ParsedEntry parseLine(String line) {
    final var matcher = AOT_LINE_PATTERN.matcher(line);
    if (matcher.matches()) {
      return new ParsedEntry(matcher.group(2));
    }

    return null;
  }
}

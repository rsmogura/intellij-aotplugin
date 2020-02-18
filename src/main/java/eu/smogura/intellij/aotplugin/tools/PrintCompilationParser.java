package eu.smogura.intellij.aotplugin.tools;

import java.util.regex.Pattern;

public class PrintCompilationParser extends BasicParser {
  public static final Pattern COMPILATION_LINE_PATTERN = Pattern.compile(
      "^\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*([\\w.$]+)::([\\w$]+).*"
  );

  @Override
  protected ParsedEntry parseLine(String line) {
    final var matcher = COMPILATION_LINE_PATTERN.matcher(line);
    if (matcher.matches()) {
      return new ParsedEntry(
          matcher.group(4),
          matcher.group(5)
      );
    }

    return null;
  }
}

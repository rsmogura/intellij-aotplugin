package eu.smogura.intellij.aotplugin.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;

public abstract class BasicParser {

  public List<ParsedEntry> parseStream(@NonNull final List<String> classPatterns,
      @NonNull final InputStream in) throws IOException {

    final var classesList = new LinkedList<ParsedEntry>();
    final var patterns = classPatterns.stream().map(Pattern::compile).collect(Collectors.toList());

    // Read stream
    try (final var inReader = new InputStreamReader(in);
        final var linesReader = new LineNumberReader(inReader)) {

      String line;
      while ((line = linesReader.readLine()) != null) {
        final var parsedLine = this.parseLine(line);
        if (parsedLine != null) {
          // Check if class name matches one of the patterns
          if (matchesPattern(patterns, parsedLine.getClassName())) {
            classesList.add(parsedLine);
          }
        }
      }
    }
    return classesList;
  }

  public void parseToAotCommands(@NonNull final List<String> classPatterns,
      @NonNull final InputStream in,
      @NonNull final Writer outWriter) throws IOException {
    final var parsedList = this.parseStream(classPatterns, in);

    for (var entry : parsedList) {

      final var classNameEscaped = StringUtils.replace(
          entry.getClassName(),"$",  "\\$");

      outWriter
          .append("compileOnly ")
          .append(classNameEscaped);

      final var methodName = entry.getMethodName();
      if (methodName != null) {
        final var methodNameEscaped = StringUtils.replace(
            methodName, "$", "\\$");
        outWriter
            .append(".")
            .append(methodName);
      }

      outWriter.append(".*\n");
    }
  }

  /**
   * Parses log line.
   */
  protected abstract ParsedEntry parseLine(String line);

  private boolean matchesPattern(final List<Pattern> classPatterns, final String className) {
    for (var pattern : classPatterns) {
      if (pattern.matcher(className).matches()) {
        return true;
      }
    }

    return false;
  }

  @Data
  @AllArgsConstructor
  public static class ParsedEntry {

    private String className;
    private String methodName;

    public ParsedEntry(final String className) {
      this(className, null);
    }
  }
}

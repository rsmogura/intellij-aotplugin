package eu.smogura.intellij.aotplugin.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.io.CharArrayWriter;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PrintCompilationParserTest {
  @Test
  public void testParing() throws Exception {
    final var compilationParser  = new PrintCompilationParser();
    final var patterns = Arrays.asList(
        "^java\\..*",
        "^io\\.micronaut\\..*",
        "^io\\.netty\\..*"
    );

    CharArrayWriter out = new CharArrayWriter();
    compilationParser.parseToAotCommands(
        patterns,
        getClass().getResourceAsStream("/micronaout-compilation-output.txt"),
        out);

    final var outputCommands = out.toString();

    assertTrue(outputCommands.contains("compileOnly java.util.ImmutableCollections\\$SetN\\$SetNIterator.nextIndex.*"));
  }
}
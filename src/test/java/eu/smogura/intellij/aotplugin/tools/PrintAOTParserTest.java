package eu.smogura.intellij.aotplugin.tools;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PrintAOTParserTest {
  @Test
  public void testParsingAOTOutput() throws Exception {
    final var sampleStream = PrintAOTParserTest.class
        .getResourceAsStream("/micronaout-printaot.txt");
    final var aotParser = new PrintAOTParser();
    final var patterns = Arrays.asList(
      "^io\\.micronaut\\..*",
      "^io\\.netty\\..*",
      "^org\\.apache\\..*"
    );


    CharArrayWriter out = new CharArrayWriter();
    aotParser.parseToAotCommands(patterns, sampleStream, out);

    final var outputCommands = out.toString();

    assertTrue(outputCommands.contains("\ncompileOnly io.micronaut.core.io.service.SoftServiceLoader\\$1.*\n"));
  }
}
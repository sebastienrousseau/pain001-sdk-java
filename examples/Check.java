// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.CheckFinding;
import com.pain001.sdk.CheckResponse;
import com.pain001.sdk.Pain001Client;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code check(...)}: runs the XSD, MDR and scheme rungs on an existing pain.001 document.
 *
 * <p>It checks the kit's generated credit-transfer XML and prints each rung and finding. Run from
 * the repository root with {@code make examples}; the server URL comes from PAIN001_BASE_URL.
 */
public final class Check {

  private Check() {}

  public static void main(String[] args) throws IOException {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).build();
    Path path = Path.of("conformance/fixtures/credit-transfer.pain.001.001.09.xml");
    String xml = Files.readString(path, StandardCharsets.UTF_8);

    CheckResponse result = client.check(xml);
    System.out.println("message type: " + result.messageType());
    System.out.println("rungs:        " + String.join(", ", result.rungs()));
    System.out.println("passed:       " + result.passed() + " (" + result.errors() + " errors)");
    for (CheckFinding finding : result.findings()) {
      System.out.println("  " + finding.severity() + " " + finding.rule() + ": " + finding.message());
    }
    if (!result.passed()) {
      System.exit(1);
    }
  }
}

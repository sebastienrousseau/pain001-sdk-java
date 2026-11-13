// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.GenerateResponse;
import com.pain001.sdk.GenerationRefusedException;
import com.pain001.sdk.MessageType;
import com.pain001.sdk.Pain001Client;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Generates a pain.001.001.09 file from the conformance kit's synthetic credit-transfer CSV.
 *
 * <p>Run from the repository root with a server listening (see README):
 *
 * <pre>
 * make example                      # writes target/credit-transfer.xml
 * </pre>
 *
 * <p>Arguments: [output path]. The server URL comes from PAIN001_BASE_URL (default
 * http://127.0.0.1:8000), the optional key from PAIN001_API_KEY.
 */
public final class GenerateCreditTransfer {

  private GenerateCreditTransfer() {}

  public static void main(String[] args) throws IOException {
    Path input = Path.of("conformance/fixtures/credit-transfer.pain.001.001.09.csv");
    Path output = Path.of(args.length > 0 ? args[0] : "target/credit-transfer.xml");
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");

    Pain001Client client =
        Pain001Client.builder()
            .baseUrl(baseUrl)
            .apiKey(System.getenv("PAIN001_API_KEY"))
            .timeout(Duration.ofSeconds(30))
            .build();

    String csv = Files.readString(input, StandardCharsets.UTF_8);
    GenerateResponse result;
    try {
      result = client.generate(csv, "payments.csv", MessageType.PAIN_001_001_09.value());
    } catch (GenerationRefusedException refused) {
      GenerateResponse findings = refused.response();
      System.err.println("generation refused: " + findings.message());
      findings
          .validationErrors()
          .forEach(e -> System.err.println("  " + e.field() + ": " + e.message()));
      findings.schemeViolations().forEach(v -> System.err.println("  " + v.path("rule").asText()));
      System.exit(1);
      return;
    }
    if (output.getParent() != null) {
      Files.createDirectories(output.getParent());
    }
    Files.writeString(output, result.xml(), StandardCharsets.UTF_8);
    System.out.println(result.message());
    System.out.println("wrote " + output);
  }
}

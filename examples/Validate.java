// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.fasterxml.jackson.databind.JsonNode;
import com.pain001.sdk.MessageType;
import com.pain001.sdk.Pain001ApiException;
import com.pain001.sdk.Pain001Client;
import com.pain001.sdk.ValidationResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code validate(...)}: validates a payment file without generating XML.
 *
 * <p>Three files from the conformance kit: the good credit-transfer CSV (valid); the CHAPS file
 * checked against the {@code uk-chaps} rulebook (HTTP 200, not valid, with a scheme violation);
 * and the CSV with a broken IBAN check digit, which the server rejects outright (HTTP 400, raised
 * as {@link Pain001ApiException} with the reason in {@code detail()}). Run from the repository
 * root with {@code make examples}; the server URL comes from PAIN001_BASE_URL.
 */
public final class Validate {

  private Validate() {}

  public static void main(String[] args) throws IOException {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).build();
    String type = MessageType.PAIN_001_001_09.value();

    ValidationResponse good = client.validate(read("credit-transfer"), "payments.csv", type);
    System.out.println("credit transfer: valid=" + good.isValid() + ", rows=" + good.totalRows());

    ValidationResponse chaps =
        client.validate(read("chaps-no-purpose"), "payments.csv", type, "uk-chaps");
    System.out.println("CHAPS, uk-chaps: valid=" + chaps.isValid());
    for (JsonNode violation : chaps.schemeViolations()) {
      System.out.println("  " + violation.path("rule").asText() + " on "
          + violation.path("field").asText());
    }

    boolean rejected = false;
    try {
      client.validate(read("bad-iban"), "payments.csv", type);
    } catch (Pain001ApiException e) {
      rejected = e.status() == 400;
      System.out.println("bad IBAN: HTTP " + e.status() + ", " + e.detail().asText());
    }
    if (!good.isValid() || chaps.isValid() || !rejected) {
      System.exit(1);
    }
  }

  private static String read(String name) throws IOException {
    Path path = Path.of("conformance/fixtures/" + name + ".pain.001.001.09.csv");
    return Files.readString(path, StandardCharsets.UTF_8);
  }
}

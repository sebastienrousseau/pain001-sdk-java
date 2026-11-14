// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.GenerateResponse;
import com.pain001.sdk.GenerationRefusedException;
import com.pain001.sdk.NoDocumentException;
import com.pain001.sdk.Pain001ApiException;
import com.pain001.sdk.Pain001Client;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The four failures a typed call can raise, and how to tell them apart.
 *
 * <ul>
 *   <li>{@link GenerationRefusedException}: the server refused the data (HTTP 422); {@code
 *       response()} has the row errors and scheme violations. Shown with the kit's bad-currency
 *       file.
 *   <li>{@link Pain001ApiException}: any other non-2xx; {@code status()} and {@code detail()}.
 *       Shown with an unknown message type.
 *   <li>{@link NoDocumentException}: a 2xx from {@code generate} without the XML, which a server
 *       older than 0.0.87 sends for a refusal. A 0.0.87 server never does, so this example only
 *       shows where to catch it.
 *   <li>{@link UncheckedIOException}: no answer at all (see the Timeouts example).
 * </ul>
 *
 * <p>Catch the subclasses first. Run from the repository root with {@code make examples}.
 */
public final class ErrorHandling {

  private ErrorHandling() {}

  public static void main(String[] args) throws IOException {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).build();
    Path path = Path.of("conformance/fixtures/bad-currency.pain.001.001.09.csv");
    String csv = Files.readString(path, StandardCharsets.UTF_8);

    String refused = attempt(client, csv, "pain.001.001.09");
    String rejected = attempt(client, csv, "pain.999.001.01");
    if (!"refused".equals(refused) || !"rejected".equals(rejected)) {
      System.exit(1);
    }
  }

  private static String attempt(Pain001Client client, String csv, String messageType) {
    try {
      GenerateResponse result = client.generate(csv, "payments.csv", messageType);
      System.out.println(messageType + ": generated " + result.xml().length() + " characters");
      return "generated";
    } catch (GenerationRefusedException e) {
      System.out.println(messageType + ": refused, HTTP " + e.status());
      e.response().validationErrors().forEach(
          row -> System.out.println("  " + row.field() + ": " + row.message()));
      return "refused";
    } catch (Pain001ApiException e) {
      System.out.println(messageType + ": rejected, HTTP " + e.status() + ", " + e.detail());
      return "rejected";
    } catch (NoDocumentException e) {
      System.out.println(messageType + ": no document (server older than 0.0.87?)");
      return "no-document";
    } catch (UncheckedIOException e) {
      System.out.println(messageType + ": no answer: " + e.getMessage());
      return "unreachable";
    }
  }
}

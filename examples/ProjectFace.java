// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.FaceResponse;
import com.pain001.sdk.LossReport;
import com.pain001.sdk.Pain001Client;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code projectFace(...)}: projects a pain.001 document onto the Berlin Group payment initiation
 * body and prints the loss report.
 *
 * <p>Run from the repository root with {@code make examples}; the server URL comes from
 * PAIN001_BASE_URL.
 */
public final class ProjectFace {

  private ProjectFace() {}

  public static void main(String[] args) throws IOException {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).build();
    Path path = Path.of("conformance/fixtures/credit-transfer.pain.001.001.09.xml");
    String xml = Files.readString(path, StandardCharsets.UTF_8);

    FaceResponse result = client.projectFace("berlin_group", xml);
    LossReport loss = result.lossReport();
    System.out.println("face:     " + result.face() + " from " + result.version());
    System.out.println("mapped:   " + loss.mappedCount() + " of " + loss.totalSourceElements());
    System.out.println("lossless: " + loss.isLossless());
    System.out.println("payload keys: " + iterableToString(result.payload().fieldNames()));
    if (result.payload().isEmpty()) {
      System.exit(1);
    }
  }

  private static String iterableToString(java.util.Iterator<String> names) {
    StringBuilder out = new StringBuilder();
    names.forEachRemaining(n -> out.append(out.length() == 0 ? "" : ", ").append(n));
    return out.toString();
  }
}

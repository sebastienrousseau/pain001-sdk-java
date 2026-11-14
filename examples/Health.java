// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.HealthResponse;
import com.pain001.sdk.Pain001Client;

/**
 * {@code health()}: asks the server whether it is up and which pain001 version it runs.
 *
 * <p>Run with {@code make examples} (every example) or {@code make example-run
 * EXAMPLE=Health}. The server URL comes from PAIN001_BASE_URL (default
 * http://127.0.0.1:8000).
 */
public final class Health {

  private Health() {}

  public static void main(String[] args) {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).build();

    HealthResponse health = client.health();
    System.out.println("status:  " + health.status());
    System.out.println("version: " + health.version());
    if (!"healthy".equals(health.status())) {
      System.exit(1);
    }
  }
}

// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.HealthResponse;
import com.pain001.sdk.Pain001ApiException;
import com.pain001.sdk.Pain001Client;

/**
 * The API key option: {@code apiKey(...)} sends {@code Authorization: Bearer <key>} on every call.
 *
 * <p>The key comes from PAIN001_API_KEY (an example value when unset). A server started without
 * keys ignores the header; one started with keys answers 401 to a wrong key, which this example
 * reports. Keep keys out of source code and logs; the client never puts it in an exception
 * message. Run with {@code make examples}.
 */
public final class ApiKey {

  private ApiKey() {}

  public static void main(String[] args) {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    String key = System.getenv().getOrDefault("PAIN001_API_KEY", "example-key");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).apiKey(key).build();

    try {
      HealthResponse health = client.health();
      System.out.println("with a key: " + health.status());
    } catch (Pain001ApiException e) {
      System.out.println("with a key: HTTP " + e.status() + " (" + e.getMessage() + ")");
      System.exit(1);
    }
  }
}

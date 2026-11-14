// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.ApiResponse;
import com.pain001.sdk.Pain001Client;

/**
 * {@code request(method, path, body)}: calls any endpoint and gets the status and parsed body,
 * whatever the status. It does not raise on a non-2xx answer.
 *
 * <p>It reads the server's OpenAPI document, then asks for a path that does not exist. Run with
 * {@code make examples}; the server URL comes from PAIN001_BASE_URL.
 */
public final class RawRequest {

  private RawRequest() {}

  public static void main(String[] args) {
    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    Pain001Client client = Pain001Client.builder().baseUrl(baseUrl).build();

    ApiResponse openapi = client.request("GET", "/openapi.json", null);
    System.out.println("GET /openapi.json -> " + openapi.status() + ", "
        + openapi.body().path("info").path("title").asText());

    ApiResponse missing = client.request("GET", "/api/v1/no-such-endpoint", null);
    System.out.println("GET /api/v1/no-such-endpoint -> " + missing.status() + ", detail: "
        + missing.body().path("detail").asText());
    if (!openapi.isSuccess() || missing.status() != 404) {
      System.exit(1);
    }
  }
}

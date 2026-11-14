// SPDX-License-Identifier: Apache-2.0 OR MIT

import com.pain001.sdk.Pain001Client;
import com.pain001.sdk.RetryPolicy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;

/**
 * The timeout and the opt-in {@link RetryPolicy}.
 *
 * <p>A client with a short timeout and a retry policy calls a loopback port nobody listens on: the
 * {@code GET} is tried three times, then {@link UncheckedIOException} is raised. The same client
 * then calls the real server, which answers at once. Run with {@code make examples}; the server
 * URL comes from PAIN001_BASE_URL.
 */
public final class Timeouts {

  private Timeouts() {}

  public static void main(String[] args) throws IOException {
    RetryPolicy retries =
        RetryPolicy.builder()
            .maxAttempts(3)
            .initialBackoff(Duration.ofMillis(50))
            .maxBackoff(Duration.ofMillis(500))
            .build();

    Pain001Client nobody = client("http://127.0.0.1:" + closedPort(), retries);
    long start = System.nanoTime();
    try {
      nobody.health();
      System.exit(1);
    } catch (UncheckedIOException e) {
      long ms = (System.nanoTime() - start) / 1_000_000;
      System.out.println("unreachable: " + e.getMessage() + " after 3 attempts, " + ms + " ms");
    }

    String baseUrl = System.getenv().getOrDefault("PAIN001_BASE_URL", "http://127.0.0.1:8000");
    System.out.println("server: " + client(baseUrl, retries).health().status());
  }

  private static Pain001Client client(String baseUrl, RetryPolicy retries) {
    return Pain001Client.builder()
        .baseUrl(baseUrl)
        .timeout(Duration.ofSeconds(2))
        .retryPolicy(retries)
        .build();
  }

  private static int closedPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      return socket.getLocalPort();
    }
  }
}

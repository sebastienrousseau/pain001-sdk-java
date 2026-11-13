// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;

/** The server the conformance and typed tests run against, from {@code PAIN001_BASE_URL}. */
final class LiveServer {

  private LiveServer() {}

  /** A client for the live server; skips the calling test when the variable is unset. */
  static Pain001Client client() {
    String url = System.getenv("PAIN001_BASE_URL");
    assumeTrue(
        url != null && !url.isBlank(),
        "PAIN001_BASE_URL is not set: start `pain001 serve` and export it to run this test");
    return Pain001Client.builder()
        .baseUrl(url)
        .apiKey(System.getenv("PAIN001_API_KEY"))
        .timeout(Duration.ofSeconds(60))
        .build();
  }

  /** The text of a kit fixture. */
  static String fixture(String name) {
    return ConformanceKit.readText(ConformanceKit.DIR, "fixtures/" + name);
  }
}

// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The opt-in retry policy: GET only, 429/503 and connection failures, backoff, Retry-After. */
class RetryPolicyTest {

  private static final String HEALTH = "{\"status\": \"healthy\", \"version\": \"0.0.88\"}";
  private static final ScriptedServer.Answer BUSY = ScriptedServer.Answer.of(503, "{}");
  private static final ScriptedServer.Answer OK = ScriptedServer.Answer.of(200, HEALTH);
  private static final Instant NOW = Instant.parse("2026-11-14T08:00:00Z");

  private final List<Duration> sleeps = new ArrayList<>();

  private RetryPolicy.Builder policy() {
    return RetryPolicy.builder()
        .sleeper(sleeps::add)
        .random(() -> 1.0)
        .clock(Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static Pain001Client client(String url, RetryPolicy policy) {
    return Pain001Client.builder().baseUrl(url).retryPolicy(policy).build();
  }

  private static ScriptedServer server(ScriptedServer.Answer... answers) throws IOException {
    return new ScriptedServer(List.of(answers));
  }

  private static ScriptedServer.Answer retryAfter(int status, String value) {
    return new ScriptedServer.Answer(status, Map.of("Retry-After", value), "{}");
  }

  @Test
  void withoutAPolicyEveryRequestIsSentOnce() throws IOException {
    try (ScriptedServer server = server(BUSY, OK)) {
      Pain001Client client = Pain001Client.builder().baseUrl(server.url()).build();
      Pain001ApiException error = assertThrows(Pain001ApiException.class, client::health);
      assertEquals(503, error.status());
      assertEquals(1, server.requests());
    }
  }

  @Test
  void aGetIsRetriedWithExponentialBackoff() throws IOException {
    try (ScriptedServer server = server(BUSY, BUSY, OK)) {
      HealthResponse health = client(server.url(), policy().build()).health();
      assertEquals("healthy", health.status());
      assertEquals(3, server.requests());
      assertEquals(List.of(Duration.ofMillis(200), Duration.ofMillis(400)), sleeps);
    }
  }

  @Test
  void jitterPicksAPointInTheUpperHalf() throws IOException {
    try (ScriptedServer server = server(BUSY, OK)) {
      RetryPolicy half = policy().random(() -> 0.0).build();
      assertEquals(200, client(server.url(), half).request("GET", "/x", null).status());
      assertEquals(List.of(Duration.ofMillis(100)), sleeps);
    }
  }

  @Test
  void theBackoffStopsAtTheMaximum() {
    RetryPolicy policy =
        policy().initialBackoff(Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(3)).build();
    assertEquals(Optional.of(Duration.ofSeconds(3)), policy.delay(3, Optional.empty()));
    assertEquals(Optional.of(Duration.ofSeconds(3)), policy.delay(64, Optional.empty()));
    Duration century = Duration.ofDays(36_500);
    RetryPolicy huge = policy().initialBackoff(century).maxBackoff(century).build();
    assertEquals(Optional.of(century), huge.delay(2, Optional.empty()));
  }

  @Test
  void theLastAnswerIsReturnedWhenAttemptsRunOut() throws IOException {
    try (ScriptedServer server = server(BUSY)) {
      Pain001Client client = client(server.url(), policy().maxAttempts(3).build());
      Pain001ApiException error = assertThrows(Pain001ApiException.class, client::faces);
      assertEquals(503, error.status());
      assertEquals(3, server.requests());
      assertEquals(2, sleeps.size());
    }
  }

  @Test
  void retryAfterSecondsIsHonouredExactly() throws IOException {
    try (ScriptedServer server = server(retryAfter(429, " 2 "), OK)) {
      client(server.url(), policy().build()).health();
      assertEquals(List.of(Duration.ofSeconds(2)), sleeps);
    }
  }

  @Test
  void retryAfterAnHttpDateIsHonoured() throws IOException {
    String date = "Sat, 14 Nov 2026 08:00:03 GMT";
    try (ScriptedServer server = server(retryAfter(503, date), OK)) {
      client(server.url(), policy().build()).health();
      assertEquals(List.of(Duration.ofSeconds(3)), sleeps);
    }
  }

  @Test
  void aRetryAfterInThePastRetriesAtOnce() {
    RetryPolicy policy = policy().build();
    String past = "Sat, 14 Nov 2026 07:59:00 GMT";
    assertEquals(Optional.of(Duration.ZERO), policy.delay(1, Optional.of(past)));
  }

  @Test
  void aRetryAfterLongerThanTheMaximumStopsRetrying() throws IOException {
    try (ScriptedServer server = server(retryAfter(429, "60"), OK)) {
      ApiResponse response = client(server.url(), policy().build()).request("GET", "/x", null);
      assertEquals(429, response.status());
      assertEquals(1, server.requests());
      assertTrue(sleeps.isEmpty());
    }
  }

  @Test
  void anUnreadableRetryAfterFallsBackToTheBackoff() throws IOException {
    try (ScriptedServer server = server(retryAfter(503, "soon"), OK)) {
      client(server.url(), policy().build()).health();
      assertEquals(List.of(Duration.ofMillis(200)), sleeps);
    }
  }

  @Test
  void otherStatusesAreNotRetried() throws IOException {
    try (ScriptedServer server = server(ScriptedServer.Answer.of(500, "{}"), OK)) {
      Pain001Client client = client(server.url(), policy().build());
      assertThrows(Pain001ApiException.class, client::health);
      assertEquals(1, server.requests());
    }
  }

  @Test
  void aPostIsNeverRetried() throws IOException {
    try (ScriptedServer server = server(BUSY)) {
      Pain001Client client = client(server.url(), policy().maxAttempts(5).build());
      assertThrows(Pain001ApiException.class, () -> client.check("<x/>"));
      assertEquals(503, client.request("delete", "/x", null).status());
      assertEquals(2, server.requests());
      assertTrue(sleeps.isEmpty());
    }
  }

  @Test
  void aLowerCaseGetIsRetriedToo() throws IOException {
    try (ScriptedServer server = server(BUSY, OK)) {
      assertEquals(200, client(server.url(), policy().build()).request("get", "/x", null).status());
      assertEquals(2, server.requests());
    }
  }

  @Test
  void connectionFailuresAreRetriedThenRaised() throws IOException {
    String url;
    try (ServerSocket closed = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      url = "http://127.0.0.1:" + closed.getLocalPort();
    }
    Pain001Client client = client(url, policy().maxAttempts(3).build());
    UncheckedIOException error = assertThrows(UncheckedIOException.class, client::health);
    assertEquals("pain001 API request GET /api/v1/health failed", error.getMessage());
    assertEquals(2, sleeps.size());
  }

  @Test
  void anInterruptedWaitRaisesAndKeepsTheFlag() throws IOException {
    RetryPolicy.Sleeper interrupted =
        d -> {
          throw new InterruptedException("stop");
        };
    try (ScriptedServer server = server(BUSY, OK)) {
      Pain001Client client = client(server.url(), policy().sleeper(interrupted).build());
      UncheckedIOException error = assertThrows(UncheckedIOException.class, client::health);
      assertInstanceOf(InterruptedIOException.class, error.getCause());
      assertTrue(Thread.interrupted(), "the interrupt flag is restored");
      assertEquals(1, server.requests());
    }
  }

  @Test
  void anInterruptedSendRaisesWithoutRetrying() throws IOException {
    try (ScriptedServer server = server(OK)) {
      Pain001Client client = client(server.url(), policy().build());
      Thread.currentThread().interrupt();
      UncheckedIOException error = assertThrows(UncheckedIOException.class, client::health);
      assertInstanceOf(InterruptedIOException.class, error.getCause());
      assertTrue(Thread.interrupted(), "the interrupt flag is restored");
      assertTrue(sleeps.isEmpty());
    }
  }

  @Test
  void theDefaultSleeperAndRandomWait() throws IOException {
    RetryPolicy policy =
        RetryPolicy.builder()
            .initialBackoff(Duration.ofNanos(1_000_500))
            .maxBackoff(Duration.ofMillis(2))
            .build();
    try (ScriptedServer server = server(BUSY, OK)) {
      assertEquals("healthy", client(server.url(), policy).health().status());
      assertEquals(2, server.requests());
    }
  }

  @Test
  void theDefaultsAreReadable() {
    RetryPolicy policy = RetryPolicy.builder().build();
    assertEquals(3, policy.maxAttempts());
    assertEquals(Duration.ofMillis(200), policy.initialBackoff());
    assertEquals(Duration.ofSeconds(5), policy.maxBackoff());
  }

  @Test
  void theBuilderRejectsBadSettings() {
    Duration one = Duration.ofSeconds(1);
    assertThrows(IllegalArgumentException.class, () -> policy().maxAttempts(0).build());
    assertThrows(IllegalArgumentException.class, () -> policy().initialBackoff(null).build());
    assertThrows(
        IllegalArgumentException.class, () -> policy().initialBackoff(Duration.ZERO).build());
    assertThrows(
        IllegalArgumentException.class, () -> policy().maxBackoff(one.negated()).build());
    assertThrows(IllegalArgumentException.class, () -> policy().maxBackoff(null).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> policy().initialBackoff(one).maxBackoff(Duration.ofMillis(10)).build());
    assertEquals(1, policy().maxAttempts(1).build().maxAttempts());
  }
}

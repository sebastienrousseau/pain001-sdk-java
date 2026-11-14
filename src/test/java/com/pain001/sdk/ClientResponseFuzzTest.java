// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Fuzzes every typed call against a loopback server that answers with an arbitrary status (200 to
 * 599) and arbitrary body bytes.
 *
 * <p>An input is three header bytes and the body: byte 0 picks the call (modulo 6, in {@link
 * #CALLS} order) and bytes 1 and 2, big-endian, pick the status ({@code 200 + n % 400}). The seeds
 * under {@code ClientResponseFuzzTestInputs/} follow that layout and run as regression tests in
 * {@code mvn verify}; with {@code JAZZER_FUZZ=1} it fuzzes (see CONTRIBUTING.md).
 *
 * <p>The client promises: a 2xx returns a value; anything else raises {@link Pain001ApiException}
 * carrying the status and the body's {@code detail}, or {@link GenerationRefusedException} for a
 * refused {@code generate}; the message names only status, method and path, never the body. Any
 * other exception escaping, or a broken promise, is a finding.
 */
class ClientResponseFuzzTest {

  /** One typed call and the method and path it sends. */
  private record Call(String method, String path, Function<Pain001Client, Object> invoke) {}

  private static final String CSV = "a,b\n1,2\n";
  private static final String GENERATE = "/api/v1/ui/generate";

  private static final List<Call> CALLS =
      List.of(
          new Call("GET", "/api/v1/health", Pain001Client::health),
          new Call("POST", "/api/v1/ui/validate", c -> c.validate(CSV, "p.csv", "pain.001.001.09")),
          new Call("POST", GENERATE, c -> c.generate(CSV, "p.csv", "pain.001.001.09")),
          new Call("POST", "/api/v1/check", c -> c.check("<Document/>")),
          new Call("GET", "/api/v1/faces", Pain001Client::faces),
          new Call(
              "POST", "/api/v1/faces/berlin_group", c -> c.projectFace("berlin_group", "<D/>")));

  private static HttpServer server;
  private static Pain001Client client;
  private static volatile int status;
  private static volatile byte[] body;

  @BeforeAll
  static void start() throws IOException {
    // Headers and body go out in two writes; without TCP_NODELAY, Nagle and delayed ACK stall
    // each exchange about 40 ms on Linux loopback. The JDK reads it when its first HttpServer
    // starts, so it takes effect in a fuzzing run, where this is the only test class.
    System.setProperty("sun.net.httpserver.nodelay", "true");
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", ClientResponseFuzzTest::answer);
    server.start();
    String url = "http://127.0.0.1:" + server.getAddress().getPort();
    client = Pain001Client.builder().baseUrl(url).timeout(Duration.ofSeconds(10)).build();
  }

  @AfterAll
  static void stop() {
    server.stop(0);
  }

  private static void answer(HttpExchange exchange) throws IOException {
    exchange.getRequestBody().readAllBytes();
    byte[] bytes = body;
    exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  @FuzzTest(maxDuration = "60s")
  void typedCallsKeepTheirErrorContract(FuzzedDataProvider data) {
    byte[] head = data.consumeBytes(3);
    Call call = CALLS.get(unsigned(head, 0) % CALLS.size());
    int code = 200 + (unsigned(head, 1) << 8 | unsigned(head, 2)) % 400;
    byte[] bytes = data.consumeRemainingAsBytes();
    // 204 and 304 never carry a body on the wire.
    status = code;
    body = code == 204 || code == 304 ? new byte[0] : bytes;
    JsonNode sent = Json.parseLenient(new String(body, StandardCharsets.UTF_8));
    try {
      Object result = call.invoke().apply(client);
      assertTrue(code >= 200 && code < 300, "a value is returned only for 2xx, got " + code);
      assertTrue(result != null, "a typed call never returns null");
    } catch (GenerationRefusedException e) {
      assertRefusal(call, code, sent, e);
    } catch (Pain001ApiException e) {
      assertApiError(call, code, sent, e);
    }
  }

  private static void assertRefusal(
      Call call, int code, JsonNode sent, GenerationRefusedException e) {
    assertEquals(GENERATE, call.path(), "only generate is refused");
    assertTrue(isRefusal(code, sent), "refused only on 422 with success false");
    assertEquals(NullNode.getInstance(), e.detail());
    assertEquals(GenerateResponse.from(sent), e.response());
    assertMessage(call, code, e);
  }

  private static void assertApiError(Call call, int code, JsonNode sent, Pain001ApiException e) {
    assertTrue(code < 200 || code >= 300, "a 2xx never raises, got " + code);
    assertTrue(!GENERATE.equals(call.path()) || !isRefusal(code, sent), "a refusal is typed");
    assertEquals(expectedDetail(sent), e.detail(), "detail is the body's detail member");
    assertMessage(call, code, e);
  }

  /** The message is exactly status, method and path: no body text can reach a log line. */
  private static void assertMessage(Call call, int code, Pain001ApiException e) {
    assertEquals(code, e.status());
    String expected = "pain001 API returned HTTP " + code + " for " + call.method();
    assertEquals(expected + " " + call.path(), e.getMessage());
  }

  /** Byte {@code i} of {@code head} as 0 to 255; 0 when the input was shorter. */
  private static int unsigned(byte[] head, int i) {
    return i < head.length ? head[i] & 0xff : 0;
  }

  private static boolean isRefusal(int code, JsonNode sent) {
    JsonNode success = sent.path("success");
    return code == 422 && success.isBoolean() && !success.asBoolean();
  }

  private static JsonNode expectedDetail(JsonNode sent) {
    if (!sent.isObject()) {
      return sent;
    }
    return sent.has("detail") ? sent.get("detail") : NullNode.getInstance();
  }
}

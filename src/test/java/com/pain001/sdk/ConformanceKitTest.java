// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The runner itself, with no server: pointers, {@code $file} and every kind of failure. */
class ConformanceKitTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode json(String text) throws IOException {
    return MAPPER.readTree(text);
  }

  @Test
  void pointerFollowsRfc6901() throws IOException {
    JsonNode doc = json("{\"a/b\": {\"~\": 1}, \"list\": [10, 20], \"\": 3}");
    assertEquals(json("1"), ConformanceKit.pointer(doc, "/a~1b/~0").orElseThrow());
    assertEquals(json("20"), ConformanceKit.pointer(doc, "/list/1").orElseThrow());
    assertEquals(json("3"), ConformanceKit.pointer(doc, "/").orElseThrow());
    assertEquals(doc, ConformanceKit.pointer(doc, "").orElseThrow());
  }

  @Test
  void pointerReportsAbsentTargets() throws IOException {
    JsonNode doc = json("{\"list\": [10, 20], \"s\": \"x\"}");
    assertFalse(ConformanceKit.pointer(doc, "/list/2").isPresent());
    assertFalse(ConformanceKit.pointer(doc, "/list/-").isPresent());
    assertFalse(ConformanceKit.pointer(doc, "/list/99999999999").isPresent());
    assertFalse(ConformanceKit.pointer(doc, "/nope").isPresent());
    assertFalse(ConformanceKit.pointer(doc, "/s/0").isPresent());
    assertFalse(ConformanceKit.pointer(doc, "/nope/deeper").isPresent());
  }

  @Test
  void fileObjectsAreReplacedByFixtureText() throws IOException {
    JsonNode body = json("[{\"$file\": \"fixtures/bad-iban.pain.001.001.09.csv\"}, {\"k\": 1}]");
    JsonNode resolved = ConformanceKit.resolve(body, ConformanceKit.DIR);
    assertTrue(resolved.get(0).asText().startsWith("id,"));
    assertEquals(json("{\"k\": 1}"), resolved.get(1));
    JsonNode twoKeys = json("{\"$file\": \"x\", \"other\": 1}");
    assertEquals(twoKeys, ConformanceKit.resolve(twoKeys, ConformanceKit.DIR));
  }

  @Test
  void fileOutsideTheKitIsRefused() throws IOException {
    JsonNode body = json("{\"$file\": \"../pom.xml\"}");
    assertThrows(
        IllegalArgumentException.class, () -> ConformanceKit.resolve(body, ConformanceKit.DIR));
  }

  @Test
  void theVendoredKitIsVersionTwoWithFourteenCases() {
    JsonNode kit = ConformanceKit.readJson(ConformanceKit.DIR.resolve("cases.json"));
    assertEquals(2, kit.path("kit_version").asInt());
    assertEquals(14, ConformanceKit.cases(ConformanceKit.DIR).size());
  }

  @Test
  void theRunnerReportsEachKindOfFailure() throws IOException {
    String health = "{\"status\": \"healthy\", \"version\": \"0.0.85\", \"message\": \"ok\"}";
    try (StubServer server = new StubServer(200, health)) {
      Pain001Client client = Pain001Client.builder().baseUrl(server.url()).build();
      JsonNode kase =
          json(
              "{\"id\": \"x\", \"method\": \"GET\", \"path\": \"/api/v1/health\", \"expect\": {"
                  + "\"status\": 200,"
                  + "\"equals\": {\"/status\": \"down\", \"/nope\": 1},"
                  + "\"types\": {\"/status\": \"integer\", \"/gone\": \"string\"},"
                  + "\"contains\": {\"/status\": [\"zzz\"], \"/version\": [\"0.0.\"],"
                  + " \"/none\": [\"a\"]}}}");
      List<String> failures = ConformanceKit.runCase(client, kase, ConformanceKit.DIR);
      assertEquals(
          List.of(
              "/status != \"down\"",
              "/nope missing",
              "/status is not integer",
              "/gone missing",
              "/status lacks \"zzz\"",
              "/none missing"),
          failures);
      JsonNode wrongStatus =
          json("{\"method\": \"GET\", \"path\": \"/\", \"expect\": {\"status\": 404}}");
      assertEquals(
          List.of("status 200 != 404"),
          ConformanceKit.runCase(client, wrongStatus, ConformanceKit.DIR));
    }
  }

  @Test
  void theRunnerHandlesArraysAndNumbers() throws IOException {
    try (StubServer server = new StubServer(200, "[{\"name\": \"a\", \"n\": 5}]")) {
      Pain001Client client = Pain001Client.builder().baseUrl(server.url()).build();
      JsonNode kase =
          json(
              "{\"method\": \"GET\", \"path\": \"/x\", \"expect\": {\"status\": 200,"
                  + "\"equals\": {\"/9/name\": \"x\", \"/a\": 1, \"/0/n\": 5.0},"
                  + "\"types\": {\"\": \"array\", \"/0/n\": \"number\"},"
                  + "\"contains\": {\"/0\": [\"x\"]}}}");
      assertEquals(
          List.of("/9/name missing", "/a missing", "/0 is not a string"),
          ConformanceKit.runCase(client, kase, ConformanceKit.DIR));
    }
  }
}

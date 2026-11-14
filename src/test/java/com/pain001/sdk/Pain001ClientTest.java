// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The client against a stub server: request shape, response mapping and error mapping. */
class Pain001ClientTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static Pain001Client client(StubServer server) {
    return Pain001Client.builder().baseUrl(server.url()).build();
  }

  private static JsonNode sent(StubServer server) throws IOException {
    return MAPPER.readTree(server.last().body());
  }

  @Test
  void healthMapsFieldsAndSendsNoKeyByDefault() throws IOException {
    String body = "{\"status\": \"healthy\", \"version\": \"0.0.85\", \"message\": \"up\"}";
    try (StubServer server = new StubServer(200, body)) {
      HealthResponse health = client(server).health();
      assertEquals(new HealthResponse("healthy", "0.0.85", "up"), health);
      assertEquals("GET", server.last().method());
      assertEquals("/api/v1/health", server.last().path());
      assertFalse(server.last().headers().containsKey("authorization"));
      assertEquals("application/json", server.last().headers().get("accept"));
      assertEquals("pain001-sdk-java/0.0.88", server.last().headers().get("user-agent"));
    }
  }

  @Test
  void validateSendsTheKeyAndTheFilePayload() throws IOException {
    String body =
        "{\"is_valid\": false, \"total_rows\": 2, \"valid_rows\": 1, \"invalid_rows\": 1,"
            + " \"errors\": [{\"field\": \"debtor_name\", \"message\": \"bad\", \"value\": 7},"
            + " {\"field\": \"x\", \"message\": \"m\"}], \"scheme_violations\": []}";
    try (StubServer server = new StubServer(200, body)) {
      Pain001Client client =
          Pain001Client.builder().baseUrl(server.url() + "/").apiKey("k-123").build();
      ValidationResponse result = client.validate("a,b", "p.csv", "pain.001.001.09", "sepa-sct");
      assertFalse(result.isValid());
      assertEquals(2, result.totalRows());
      assertEquals(1, result.invalidRows());
      RowError first = result.errors().get(0);
      assertEquals("debtor_name", first.field());
      assertEquals("bad", first.message());
      assertEquals(7, first.value().asInt());
      assertTrue(result.errors().get(1).value().isNull());
      assertEquals("/api/v1/ui/validate", server.last().path());
      assertEquals("Bearer k-123", server.last().headers().get("authorization"));
      assertEquals("application/json", server.last().headers().get("content-type"));
      JsonNode payload = sent(server);
      assertEquals("a,b", payload.get("content").asText());
      assertEquals("p.csv", payload.get("filename").asText());
      assertEquals("pain.001.001.09", payload.get("message_type").asText());
      assertEquals("sepa-sct", payload.get("scheme").asText());
    }
  }

  @Test
  void generateOmitsAnAbsentSchemeAndMapsTheXml() throws IOException {
    String body =
        "{\"success\": true, \"message\": \"ok\", \"filename\": \"f.xml\", \"xml\": \"<D/>\"}";
    try (StubServer server = new StubServer(200, body)) {
      GenerateResponse result =
          client(server).generate("a", "p.csv", MessageType.PAIN_008_001_08.value());
      assertTrue(result.success());
      assertEquals("<D/>", result.xml());
      assertEquals("f.xml", result.filename());
      assertEquals(List.of(), result.validationErrors());
      assertEquals("/api/v1/ui/generate", server.last().path());
      assertFalse(sent(server).has("scheme"));
      assertEquals("pain.008.001.08", sent(server).get("message_type").asText());
    }
  }

  @Test
  void aRefusedGenerationCarriesItsFindings() throws IOException {
    String body =
        "{\"success\": false, \"message\": \"Validation failed\", \"filename\": null,"
            + " \"xml\": null, \"validation_errors\": [{\"field\": \"$.currency\","
            + " \"message\": \"bad currency\", \"value\": \"EURO\"}], \"scheme_violations\":"
            + " [{\"rule\": \"UK-CHAPS-PURP\", \"severity\": \"error\"}]}";
    try (StubServer server = new StubServer(422, body)) {
      Pain001Client client = client(server);
      GenerationRefusedException error =
          assertThrows(
              GenerationRefusedException.class,
              () -> client.generate("SECRET-ROW", "p.csv", "pain.001.001.09", "uk-chaps"));
      assertEquals(422, error.status());
      assertTrue(error.detail().isNull());
      assertEquals(
          "pain001 API returned HTTP 422 for POST /api/v1/ui/generate", error.getMessage());
      GenerateResponse refused = error.response();
      assertFalse(refused.success());
      assertNull(refused.xml());
      assertEquals("Validation failed", refused.message());
      RowError row = refused.validationErrors().get(0);
      assertEquals("$.currency", row.field());
      assertEquals("EURO", row.value().asText());
      assertEquals("UK-CHAPS-PURP", refused.schemeViolations().get(0).get("rule").asText());
    }
  }

  @Test
  void aRequestValidation422FromGenerateIsAPlainApiException() throws IOException {
    try (StubServer server = new StubServer(422, "{\"detail\": [{\"msg\": \"missing\"}]}")) {
      Pain001Client client = client(server);
      Pain001ApiException error =
          assertThrows(
              Pain001ApiException.class, () -> client.generate("a", "p.csv", "pain.001.001.09"));
      assertFalse(error instanceof GenerationRefusedException);
      assertEquals("missing", error.detail().get(0).get("msg").asText());
    }
  }

  @Test
  void checkMapsFindings() throws IOException {
    String body =
        "{\"message_type\": \"pain.001.001.09\", \"passed\": false, \"errors\": 1,"
            + " \"warnings\": 0, \"rungs\": [\"xsd\"], \"findings\": [{\"rung\": \"xsd\","
            + " \"rule\": \"r1\", \"severity\": \"error\", \"message\": \"m\","
            + " \"location\": null}]}";
    try (StubServer server = new StubServer(200, body)) {
      CheckResponse result = client(server).check("<x/>", "sepa-sct", "sepa");
      assertFalse(result.passed());
      assertEquals(List.of("xsd"), result.rungs());
      assertEquals(new CheckFinding("xsd", "r1", "error", "m", null), result.findings().get(0));
      assertEquals("sepa", sent(server).get("schematron").asText());
      assertEquals("sepa-sct", sent(server).get("scheme").asText());
    }
  }

  @Test
  void facesAndProjectionMapTheirFields() throws IOException {
    String list = "[{\"name\": \"uk\", \"description\": \"d\", \"bindings\": [\"ConsentId\"]}]";
    try (StubServer server = new StubServer(200, list)) {
      List<FaceInfo> faces = client(server).faces();
      assertEquals(List.of(new FaceInfo("uk", "d", List.of("ConsentId"))), faces);
    }
    String face =
        "{\"face\": \"uk\", \"version\": \"pain.001.001.09\", \"payload\": {\"a\": 1},"
            + " \"loss_report\": {\"face\": \"uk\", \"total_source_elements\": 4,"
            + " \"mapped_count\": 3, \"unmapped_count\": 1, \"is_lossless\": false,"
            + " \"loss_ratio\": 0.25, \"mapped_paths\": [\"p\"], \"unmapped_paths\": [\"q\"]}}";
    try (StubServer server = new StubServer(200, face)) {
      FaceResponse result =
          client(server).projectFace("uk", "<x/>", null, Map.of("ConsentId", "c-1"));
      assertEquals(1, result.payload().get("a").asInt());
      assertEquals(
          new LossReport("uk", 4, 3, 1, false, 0.25, List.of("p"), List.of("q")),
          result.lossReport());
      assertEquals("/api/v1/faces/uk", server.last().path());
      assertEquals("c-1", sent(server).get("bindings").get("ConsentId").asText());
      assertFalse(sent(server).has("version"));
    }
  }

  @Test
  void aFaceNameThatIsNotPlainIsRefusedBeforeSending() throws IOException {
    try (StubServer server = new StubServer(200, "{}")) {
      Pain001Client client = client(server);
      assertThrows(IllegalArgumentException.class, () -> client.projectFace("../check", "<x/>"));
      assertThrows(IllegalArgumentException.class, () -> client.projectFace(null, "<x/>"));
      assertNull(server.last());
    }
  }

  @Test
  void aStringDetailBecomesAnApiExceptionWithoutRequestContent() throws IOException {
    try (StubServer server = new StubServer(400, "{\"detail\": \"not a valid IBAN\"}")) {
      Pain001ApiException error =
          assertThrows(
              Pain001ApiException.class,
              () -> client(server).validate("SECRET-ROW", "p.csv", "pain.001.001.09"));
      assertEquals(400, error.status());
      assertEquals("not a valid IBAN", error.detail().asText());
      assertEquals(
          "pain001 API returned HTTP 400 for POST /api/v1/ui/validate", error.getMessage());
      assertFalse(error.getMessage().contains("SECRET-ROW"));
    }
  }

  @Test
  void anArrayDetailIsKeptAsAnArray() throws IOException {
    try (StubServer server = new StubServer(422, "{\"detail\": [{\"msg\": \"bad type\"}]}")) {
      Pain001ApiException error =
          assertThrows(Pain001ApiException.class, () -> client(server).check("<x/>"));
      assertEquals(422, error.status());
      assertTrue(error.detail().isArray());
      assertEquals("bad type", error.detail().get(0).get("msg").asText());
    }
  }

  @Test
  void anErrorBodyWithoutDetailGivesNullOrTheBody() throws IOException {
    try (StubServer server = new StubServer(500, "{\"error\": \"x\"}")) {
      Pain001ApiException error =
          assertThrows(Pain001ApiException.class, () -> client(server).health());
      assertTrue(error.detail().isNull());
    }
    try (StubServer server = new StubServer(502, "<html>bad gateway</html>")) {
      Pain001ApiException error =
          assertThrows(Pain001ApiException.class, () -> client(server).faces());
      assertEquals("<html>bad gateway</html>", error.detail().asText());
    }
  }

  @Test
  void requestReturnsAnyStatusWithoutRaising() throws IOException {
    try (StubServer server = new StubServer(404, "{\"detail\": \"Not Found\"}")) {
      ApiResponse response =
          client(server).request("post", "/api/v1/x", MAPPER.readTree("{\"a\": 1}"));
      assertEquals(404, response.status());
      assertFalse(response.isSuccess());
      assertEquals("Not Found", response.body().get("detail").asText());
      assertEquals("POST", server.last().method());
      assertEquals("{\"a\":1}", server.last().body());
    }
    try (StubServer server = new StubServer(204, "")) {
      ApiResponse response = client(server).request("DELETE", "/api/v1/jobs/1", null);
      assertTrue(response.isSuccess());
      assertTrue(response.body().isNull());
      assertFalse(server.last().headers().containsKey("content-type"));
    }
  }

  @Test
  void aPathMustStartWithASlash() throws IOException {
    try (StubServer server = new StubServer(200, "{}")) {
      Pain001Client client = client(server);
      assertThrows(IllegalArgumentException.class, () -> client.request("GET", "api", null));
    }
  }

  @Test
  void aSlowServerTimesOut() throws IOException {
    try (StubServer server = new StubServer(200, "{}", 2000)) {
      Pain001Client client =
          Pain001Client.builder().baseUrl(server.url()).timeout(Duration.ofMillis(200)).build();
      UncheckedIOException error = assertThrows(UncheckedIOException.class, client::health);
      assertEquals("pain001 API request GET /api/v1/health failed", error.getMessage());
    }
  }

  @Test
  void anUnreachableServerRaisesUncheckedIo() throws IOException {
    String url;
    try (StubServer server = new StubServer(200, "{}")) {
      url = server.url();
    }
    Pain001Client client = Pain001Client.builder().baseUrl(url).build();
    assertThrows(UncheckedIOException.class, client::health);
  }

  @Test
  void theBuilderRejectsBadSettings() {
    assertThrows(IllegalArgumentException.class, () -> Pain001Client.builder().build());
    assertThrows(
        IllegalArgumentException.class, () -> Pain001Client.builder().baseUrl(" ").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> Pain001Client.builder().baseUrl("ftp://h").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> Pain001Client.builder().baseUrl("http://h/?q=1").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> Pain001Client.builder().baseUrl("not a url").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> Pain001Client.builder().baseUrl("http://h").timeout(Duration.ZERO).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> Pain001Client.builder().baseUrl("http://h").timeout(null).build());
  }

  @Test
  void messageTypesRoundTrip() {
    assertEquals(13, MessageType.values().length);
    assertEquals("pain.001.001.09", MessageType.PAIN_001_001_09.toString());
    MessageType parsed = MessageType.fromValue("pain.008.001.02").orElseThrow();
    assertEquals(MessageType.PAIN_008_001_02, parsed);
    assertFalse(MessageType.fromValue("pain.999.001.01").isPresent());
  }
}

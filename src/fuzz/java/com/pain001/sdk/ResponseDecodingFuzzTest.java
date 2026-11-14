// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Fuzzes the decoding of a response body: whatever a server (or a proxy in front of it) sends, the
 * lenient parse and every record's decoder must return a value, never throw.
 *
 * <p>Runs the seeds under {@code ResponseDecodingFuzzTestInputs/} as regression tests in {@code
 * mvn verify}; with {@code JAZZER_FUZZ=1} it fuzzes (see CONTRIBUTING.md).
 */
class ResponseDecodingFuzzTest {

  @FuzzTest(maxDuration = "60s")
  void anyBodyDecodesIntoEveryRecord(FuzzedDataProvider data) {
    String text = new String(data.consumeRemainingAsBytes(), StandardCharsets.UTF_8);
    JsonNode body = Json.parseLenient(text);
    assertNotNull(body);
    assertKeepsWhatItCannotParse(text, body);
    decodeAll(body);
    Json.elements(body).forEach(ResponseDecodingFuzzTest::decodeAll);
  }

  /** Blank becomes JSON null; text that is not JSON comes back unchanged as a JSON string. */
  private static void assertKeepsWhatItCannotParse(String text, JsonNode body) {
    if (text.isBlank()) {
      assertTrue(body.isNull(), "a blank body decodes to JSON null");
      return;
    }
    if (body.isTextual() && !text.strip().startsWith("\"")) {
      assertEquals(text, body.asText(), "a body that is not JSON is kept verbatim");
    }
  }

  /** Every typed decoder over one node; each must succeed and give non-null lists. */
  private static void decodeAll(JsonNode node) {
    GenerateResponse generate = GenerateResponse.from(node);
    nonNull(generate.validationErrors(), generate.schemeViolations());
    ValidationResponse validation = ValidationResponse.from(node);
    nonNull(validation.errors(), validation.schemeViolations());
    CheckResponse check = CheckResponse.from(node);
    nonNull(check.rungs(), check.findings());
    FaceResponse face = FaceResponse.from(node);
    assertNotNull(face.payload());
    nonNull(face.lossReport().mappedPaths(), face.lossReport().unmappedPaths());
    assertNotNull(FaceInfo.from(node).bindings());
    assertNotNull(HealthResponse.from(node));
    assertNotNull(RowError.from(node).value());
    assertNotNull(CheckFinding.from(node));
  }

  private static void nonNull(List<?> first, List<?> second) {
    assertNotNull(first);
    assertNotNull(second);
  }
}

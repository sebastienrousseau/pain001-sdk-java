// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** One test per SDK method against a live server (skipped when PAIN001_BASE_URL is unset). */
class TypedApiTest {

  private static final String CSV = "credit-transfer.pain.001.001.09.csv";
  private static final String XML = "credit-transfer.pain.001.001.09.xml";
  private static final String TYPE = MessageType.PAIN_001_001_09.value();

  @Test
  void health() {
    HealthResponse health = LiveServer.client().health();
    assertEquals("healthy", health.status());
    assertFalse(health.version().isEmpty());
  }

  @Test
  void validate() {
    ValidationResponse result =
        LiveServer.client().validate(LiveServer.fixture(CSV), "payments.csv", TYPE);
    assertTrue(result.isValid());
    assertEquals(5, result.totalRows());
    assertEquals(5, result.validRows());
    assertEquals(0, result.invalidRows());
    assertEquals(List.of(), result.errors());
  }

  @Test
  void validateBadIbanRaises() {
    Pain001Client client = LiveServer.client();
    String bad = LiveServer.fixture("bad-iban.pain.001.001.09.csv");
    Pain001ApiException error =
        assertThrows(Pain001ApiException.class, () -> client.validate(bad, "payments.csv", TYPE));
    assertEquals(400, error.status());
    assertTrue(error.detail().asText().contains("not a valid IBAN"));
  }

  @Test
  void generate() {
    GenerateResponse result =
        LiveServer.client().generate(LiveServer.fixture(CSV), "payments.csv", TYPE);
    assertTrue(result.success());
    assertTrue(result.xml().contains("<NbOfTxs>5</NbOfTxs>"));
    assertTrue(result.xml().contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.09"));
  }

  @Test
  void generateRefusedRowsCarriesTheRowErrors() {
    Pain001Client client = LiveServer.client();
    String bad = LiveServer.fixture("bad-currency.pain.001.001.09.csv");
    GenerationRefusedException error =
        assertThrows(
            GenerationRefusedException.class, () -> client.generate(bad, "payments.csv", TYPE));
    assertEquals(422, error.status());
    GenerateResponse refused = error.response();
    assertFalse(refused.success());
    assertNull(refused.xml());
    assertEquals("$.currency", refused.validationErrors().get(0).field());
    assertEquals("EURO", refused.validationErrors().get(0).value().asText());
  }

  @Test
  void generateRefusedSchemeCarriesTheViolations() {
    Pain001Client client = LiveServer.client();
    String chaps = LiveServer.fixture("chaps-no-purpose.pain.001.001.09.csv");
    GenerationRefusedException error =
        assertThrows(
            GenerationRefusedException.class,
            () -> client.generate(chaps, "payments.csv", TYPE, "uk-chaps"));
    GenerateResponse refused = error.response();
    assertEquals(List.of(), refused.validationErrors());
    assertEquals("UK-CHAPS-PURP", refused.schemeViolations().get(0).get("rule").asText());
  }

  @Test
  void check() {
    CheckResponse result = LiveServer.client().check(LiveServer.fixture(XML));
    assertTrue(result.passed());
    assertEquals(0, result.errors());
    assertEquals(TYPE, result.messageType());
    assertTrue(result.rungs().contains("xsd"));
  }

  @Test
  void checkRejectsANonPayment() {
    Pain001Client client = LiveServer.client();
    Pain001ApiException error = assertThrows(Pain001ApiException.class, () -> client.check("<x/>"));
    assertEquals(422, error.status());
  }

  @Test
  void faces() {
    List<String> names = LiveServer.client().faces().stream().map(FaceInfo::name).toList();
    assertEquals(List.of("berlin_group", "iso_2018", "uk_open_banking"), names);
  }

  @Test
  void projectFace() {
    FaceResponse result = LiveServer.client().projectFace("berlin_group", LiveServer.fixture(XML));
    assertEquals("berlin_group", result.face());
    assertEquals(TYPE, result.version());
    assertEquals(
        "DE07512108001245126162", result.payload().at("/debtorAccount/iban").asText());
    LossReport loss = result.lossReport();
    assertEquals(loss.totalSourceElements(), loss.mappedCount() + loss.unmappedCount());
  }

  @Test
  void request() {
    ApiResponse response = LiveServer.client().request("GET", "/api/v1/health", null);
    assertEquals(200, response.status());
    assertEquals("healthy", response.body().get("status").asText());
  }
}

// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A 2xx from generate without the XML is an error, never a response without a document. */
class NoDocumentTest {

  private static final List<String> BODIES =
      List.of(
          // What a pain001 server before 0.0.87 sends for a refused file.
          "{\"success\": false, \"message\": \"Validation failed: 4/5 rows valid\", \"xml\": null,"
              + " \"validation_errors\": [{\"field\": \"$.currency\", \"value\": \"EURO\"}]}",
          "{\"success\": true, \"message\": \"Generated\", \"xml\": \"\"}",
          "{\"success\": true, \"message\": \"Generated\"}",
          "{}",
          "");

  @Test
  void aSuccessWithoutTheXmlRaisesNoDocument() throws IOException {
    for (String body : BODIES) {
      try (StubServer server = new StubServer(200, body)) {
        Pain001Client client = Pain001Client.builder().baseUrl(server.url()).build();
        NoDocumentException error =
            assertThrows(
                NoDocumentException.class,
                () -> client.generate("SECRET-ROW", "p.csv", "pain.001.001.09"),
                body);
        assertEquals(200, error.status());
        assertEquals(
            "pain001 API returned HTTP 200 for POST /api/v1/ui/generate without an XML document",
            error.getMessage());
        assertFalse(error.getMessage().contains("EURO"));
      }
    }
  }

  @Test
  void noDocumentIsATransportFailure() {
    UncheckedIOException error = new NoDocumentException(201, "POST", "/x");
    assertEquals("pain001 API returned HTTP 201 for POST /x without an XML document",
        error.getCause().getMessage());
  }
}

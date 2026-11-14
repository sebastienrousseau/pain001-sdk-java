// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.POJONode;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The edges of each guard: the inputs the main tests do not reach. */
class BoundaryTest {

  @Test
  void aStatusBelow200IsNotASuccess() {
    assertFalse(new ApiResponse(199, NullNode.getInstance()).isSuccess());
    assertTrue(new ApiResponse(299, NullNode.getInstance()).isSuccess());
    assertFalse(new ApiResponse(300, NullNode.getInstance()).isSuccess());
  }

  @Test
  void aNullBodyParsesAsJsonNull() {
    assertTrue(Json.parseLenient(null).isNull());
  }

  @Test
  void aTreeJacksonCannotWriteIsAnIllegalArgument() {
    POJONode opaque = new POJONode(new Object());
    assertThrows(IllegalArgumentException.class, () -> Json.write(opaque));
  }

  @Test
  void aNullPathIsRefusedBeforeSending() {
    Pain001Client client = Pain001Client.builder().baseUrl("http://127.0.0.1:9").build();
    assertThrows(IllegalArgumentException.class, () -> client.request("GET", null, null));
  }

  @Test
  void a422WithSuccessTrueIsNotARefusal() throws IOException {
    try (StubServer server = new StubServer(422, "{\"success\": true, \"detail\": \"odd\"}")) {
      Pain001Client client = Pain001Client.builder().baseUrl(server.url()).build();
      Pain001ApiException error =
          assertThrows(
              Pain001ApiException.class, () -> client.generate("a", "p.csv", "pain.001.001.09"));
      assertFalse(error instanceof GenerationRefusedException);
      assertEquals("odd", error.detail().asText());
    }
  }

  @Test
  void theBuilderAcceptsHttpsAndRefusesTheOtherEdges() {
    assertTrue(Pain001Client.builder().baseUrl("HTTPS://example.test/").build() != null);
    assertThrows(
        IllegalArgumentException.class,
        () -> Pain001Client.builder().baseUrl("http://h").timeout(Duration.ofSeconds(-1)).build());
    assertThrows(
        IllegalArgumentException.class, () -> Pain001Client.builder().baseUrl("http://h#f").build());
    assertThrows(
        IllegalArgumentException.class, () -> Pain001Client.builder().baseUrl("http:///p").build());
  }
}

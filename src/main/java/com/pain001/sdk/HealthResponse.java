// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code GET /api/v1/health}.
 *
 * @param status the service status, {@code healthy} when it is up
 * @param version the server's pain001 version
 * @param message a human-readable status line
 */
public record HealthResponse(String status, String version, String message) {

  static HealthResponse from(JsonNode node) {
    return new HealthResponse(
        Json.text(node, "status"), Json.text(node, "version"), Json.text(node, "message"));
  }
}

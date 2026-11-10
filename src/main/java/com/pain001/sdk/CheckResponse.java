// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * {@code POST /api/v1/check}: the verdict, the rungs that ran and every finding.
 *
 * @param messageType the detected message type
 * @param passed whether no rung reported an error
 * @param errors the number of error findings
 * @param warnings the number of warning findings
 * @param rungs the rungs that ran, in order
 * @param findings every finding
 */
public record CheckResponse(
    String messageType,
    boolean passed,
    int errors,
    int warnings,
    List<String> rungs,
    List<CheckFinding> findings) {

  /** Copies the lists so the record stays immutable. */
  public CheckResponse {
    rungs = List.copyOf(rungs);
    findings = List.copyOf(findings);
  }

  static CheckResponse from(JsonNode node) {
    return new CheckResponse(
        Json.text(node, "message_type"),
        Json.bool(node, "passed"),
        Json.integer(node, "errors"),
        Json.integer(node, "warnings"),
        Json.texts(node, "rungs"),
        Json.nodes(node, "findings").stream().map(CheckFinding::from).toList());
  }
}

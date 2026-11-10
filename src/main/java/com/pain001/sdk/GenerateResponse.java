// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * {@code POST /api/v1/ui/generate} (the server's {@code UiGenerateResponse}).
 *
 * @param success whether the XML was generated
 * @param message a human-readable result line
 * @param filename the suggested download name, or null
 * @param xml the generated XML document, or null when generation failed
 * @param validationErrors row-level validation errors, when validation failed
 * @param schemeViolations scheme-rulebook violations, when scheme validation failed
 */
public record GenerateResponse(
    boolean success,
    String message,
    String filename,
    String xml,
    List<RowError> validationErrors,
    List<JsonNode> schemeViolations) {

  /** Copies the lists so the record stays immutable. */
  public GenerateResponse {
    validationErrors = List.copyOf(validationErrors);
    schemeViolations = List.copyOf(schemeViolations);
  }

  static GenerateResponse from(JsonNode node) {
    return new GenerateResponse(
        Json.bool(node, "success"),
        Json.text(node, "message"),
        Json.text(node, "filename"),
        Json.text(node, "xml"),
        Json.nodes(node, "validation_errors").stream().map(RowError::from).toList(),
        Json.nodes(node, "scheme_violations"));
  }
}

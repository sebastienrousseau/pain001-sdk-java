// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * {@code POST /api/v1/ui/validate}.
 *
 * @param isValid whether every row is valid
 * @param totalRows rows read
 * @param validRows rows that passed
 * @param invalidRows rows that failed
 * @param errors one entry per row-level validation error
 * @param schemeViolations scheme-rulebook violations, when a scheme was given
 */
public record ValidationResponse(
    boolean isValid,
    int totalRows,
    int validRows,
    int invalidRows,
    List<RowError> errors,
    List<JsonNode> schemeViolations) {

  /** Copies the lists so the record stays immutable. */
  public ValidationResponse {
    errors = List.copyOf(errors);
    schemeViolations = List.copyOf(schemeViolations);
  }

  static ValidationResponse from(JsonNode node) {
    return new ValidationResponse(
        Json.bool(node, "is_valid"),
        Json.integer(node, "total_rows"),
        Json.integer(node, "valid_rows"),
        Json.integer(node, "invalid_rows"),
        Json.nodes(node, "errors").stream().map(RowError::from).toList(),
        Json.nodes(node, "scheme_violations"));
  }
}

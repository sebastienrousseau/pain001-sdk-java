// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * One row-level validation error from {@code validate} or {@code generate}.
 *
 * <p>This is pain001's row error, not the request-validation items of an HTTP 422 {@code detail},
 * which {@link Pain001ApiException#detail()} carries as raw JSON.
 *
 * @param field the input column or field at fault
 * @param message what is wrong
 * @param value the offending value as the server sent it (any JSON), or JSON null
 */
public record RowError(String field, String message, JsonNode value) {

  static RowError from(JsonNode node) {
    JsonNode value = node.path("value");
    return new RowError(
        Json.text(node, "field"),
        Json.text(node, "message"),
        value.isMissingNode() ? NullNode.getInstance() : value);
  }
}

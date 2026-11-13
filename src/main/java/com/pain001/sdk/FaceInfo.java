// SPDX-License-Identifier: Apache-2.0 OR MIT
package com.pain001.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * One face from {@code GET /api/v1/faces}.
 *
 * @param name the face name, used in the {@code /api/v1/faces/{face}} path
 * @param description the target standard
 * @param bindings the binding keys the face accepts
 */
public record FaceInfo(String name, String description, List<String> bindings) {

  /** Copies the list so the record stays immutable. */
  public FaceInfo {
    bindings = List.copyOf(bindings);
  }

  static FaceInfo from(JsonNode node) {
    return new FaceInfo(
        Json.text(node, "name"), Json.text(node, "description"), Json.texts(node, "bindings"));
  }
}
